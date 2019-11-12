package ch.ebu.peachcollector;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.lang.Math.min;

public class PeachCollector {
    private static volatile PeachCollector INSTANCE;
    private static volatile Context applicationContext;
    public RoomDatabase database;
    public HashMap<String, Publisher> publishers;
    public HashMap<String, Timer> publisherTimers;
    public HashMap<String, Integer> publisherFailures;
    private Executor dbExecutor;
    private HandlerThread handlerThread;
    public static String implementationVersion;
    public static String userID;
    public static boolean isUnitTesting = false;

    public static PeachCollector init(final Context context) {
        applicationContext = context;
        if (INSTANCE == null) {
            synchronized (RoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PeachCollector();
                    INSTANCE.database = RoomDatabase.getDatabase(context);
                    INSTANCE.publishers = new HashMap<>();
                    INSTANCE.publisherTimers = new HashMap<>();
                    INSTANCE.publisherFailures = new HashMap<>();
                    INSTANCE.dbExecutor = Executors.newSingleThreadExecutor();
                    INSTANCE.handlerThread = new HandlerThread("SomeNameHere");
                    INSTANCE.handlerThread.start(); //should call quit() onDestroy of activity ? INSTANCE.handlerThread.quit()
                }
            }
        }
        return INSTANCE;
    }

    protected static Context getApplicationContext() {
        return applicationContext;
    }


    public static void sendEvent(final Event event) {
        INSTANCE.dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long eventRowID = INSTANCE.database.peachCollectorEventDao().insert(event);

                for (String publisherName: INSTANCE.publishers.keySet()) {
                    Publisher publisher = INSTANCE.publishers.get(publisherName);

                    if (publisher.shouldProcessEvent(event)) {
                        EventStatus status = new EventStatus((int)eventRowID, publisherName, 0);
                        INSTANCE.database.peachCollectorEventDao().insert(status);
                    }
                }

                if (PeachCollector.isUnitTesting) {
                    Intent intent = new Intent();
                    intent.setAction("ch.ebu.testingLog");
                    intent.putExtra("Log", "+ Event (" + event.getType() + ")");
                    applicationContext.sendBroadcast(intent);
                }

                INSTANCE.checkPublishers();
            }
        });
    }

    public static void addPublisher(Publisher publisher, String publisherName) {
        INSTANCE.publishers.put(publisherName, publisher);
        // send events in the database that are queued for this publisher
        INSTANCE.sendEventsToPublisher(publisherName);
    }

    public static void clean() {
        INSTANCE.cleanTimers();
        INSTANCE.dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                INSTANCE.database.peachCollectorEventDao().deleteAll();
            }
        });
    }

    public static void flush() {
        for (String publisherName :INSTANCE.publishers.keySet()) {
            INSTANCE.sendEventsToPublisher(publisherName);
        }
    }

    private void checkPublishers() {
        for (String publisherName: publishers.keySet()) {
            checkPublisher(publisherName);
        }
    }

    private void checkPublisher(final String publisherName) {
        final Publisher publisher = publishers.get(publisherName);
        assert publisher != null;
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<EventStatus> statuses = INSTANCE.database.peachCollectorEventDao().getStatuses(publisherName);

                int pendingEventsCount = 0;
                for (EventStatus status: statuses) {
                    if (status.getStatus() == 1) return;
                    if (status.getStatus() == 0) {
                        pendingEventsCount++;
                    }
                }
                if (pendingEventsCount == 0)  return;
                boolean lastPublishingHasFailed = publisherFailures.containsKey(publisherName);
                boolean hasTooManyEvents = pendingEventsCount >= publisher.maxEventsPerBatchAfterOfflineSession;
                if (!lastPublishingHasFailed && (pendingEventsCount >= publisher.maxEventsPerBatch || publisher.interval == 0) && !hasTooManyEvents) {
                    sendEventsToPublisher(publisherName);
                }
                else if (!publisherTimers.containsKey(publisherName)) {
                    startTimerForPublisher(publisherName, hasTooManyEvents);
                }
            }
        });
    }

    private void startTimerForPublisher(final String publisherName, boolean shouldFollowPolicy) {
        long interval = interval(publisherName, shouldFollowPolicy) * 1000;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendEventsToPublisher(publisherName);
            }
        }, interval);
        publisherTimers.put(publisherName, timer);
        Log.d("TIMER", "Started timer for " + publisherName + " (" + interval + ")");
    }

    private void cleanTimer(String publisherName) {
        if (publisherTimers.containsKey(publisherName)) {
            Timer timer = publisherTimers.get(publisherName);
            if (timer != null) {
                timer.cancel();
                timer.purge();
                publisherTimers.remove(publisherName);
            }
        }
    }
    private void cleanTimers(){
        for (String publisherName: INSTANCE.publishers.keySet()) {
            cleanTimer(publisherName);
        }
    }

    private void sendEventsToPublisher(final String publisherName) {
        final Publisher publisher = publishers.get(publisherName);

        if (publisher == null) return;

        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<EventStatus> statuses = database.peachCollectorEventDao().getStatuses(publisherName);
                final ArrayList<Event> events = new ArrayList<>();
                int pendingEventsCount = 0;
                for (EventStatus status: statuses) {
                    if (status.getStatus() == 1) return;
                    if (status.getStatus() == 0) {
                        pendingEventsCount ++;
                        if (events.size() < publisher.maxEventsPerBatchAfterOfflineSession) {
                            Event event = database.peachCollectorEventDao().getEvent(status.eventID);
                            events.add(event);
                            status.setStatus(1);
                            database.peachCollectorEventDao().update(status);
                        }
                    }
                }
                if (events.size() == 0) return;

                cleanTimer(publisherName);

                //final boolean shouldContinueSending = pendingEventsCount > events.size();

                Handler handler = new Handler(handlerThread.getLooper(), new Handler.Callback() {

                    @Override
                    public boolean handleMessage(Message msg) {
                        boolean sent = msg.obj == null;

                        List<EventStatus> statuses = database.peachCollectorEventDao().getStatuses(publisherName);
                        boolean shouldContinueSending = statuses.size() > events.size();

                        for (Event event: events) {
                            database.peachCollectorEventDao().updateStatus(event.getId(), publisherName, sent ? Constant.Status.PUBLISHED : Constant.Status.QUEUED);
                            // check if all event statuses are `sent` and remove event if it is
                            List<EventStatus> eventStatuses = database.peachCollectorEventDao().getPendingEventStatuses(event.getId());
                            if (eventStatuses == null || eventStatuses.size() == 0) {
                                database.peachCollectorEventDao().delete(event);
                            }
                        }

                        if (msg.obj != null) {

                            Integer numberOfFailures = publisherFailures.get(publisherName);
                            numberOfFailures = (numberOfFailures == null) ? 1 : numberOfFailures+1;
                            publisherFailures.put(publisherName, numberOfFailures);

                            startTimerForPublisher(publisherName, false);

                            if (PeachCollector.isUnitTesting) {
                                Intent intent = new Intent();
                                intent.setAction("ch.ebu.testingLog");
                                intent.putExtra("Log", publisherName + " : Failed to publish events");
                                applicationContext.sendBroadcast(intent);
                            }
                        }
                        else {
                            publisherFailures.remove(publisherName);
                            if (shouldContinueSending) {
                                checkPublisher(publisherName);
                            }
                            if (PeachCollector.isUnitTesting) {
                                Intent intent = new Intent();
                                intent.setAction("ch.ebu.testingLog");
                                intent.putExtra("Log", publisherName + " : Published " + events.size() + " events");
                                applicationContext.sendBroadcast(intent);
                            }
                        }
                        return false;
                    }
                });


                publisher.processEvents(events, handler);
            }
        });
    }



    private int interval(String publisherName, boolean shouldFollowPolicy)
    {
        final Publisher publisher = publishers.get(publisherName);
        Integer numberOfFailures = publisherFailures.get(publisherName);
        if (numberOfFailures != null){
            return min(300, publisher.interval * (numberOfFailures + 1));
        }

        if (shouldFollowPolicy) {
            if (publisher.gotBackPolicy == Constant.GoBackOnlinePolicy.SEND_ALL) {
                return 0;
            }
            else {
                return new Random().nextInt(60);
            }
        }

        return publisher.interval;
    }

}
