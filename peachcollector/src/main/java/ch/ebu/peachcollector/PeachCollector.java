package ch.ebu.peachcollector;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.content.Context.MODE_PRIVATE;
import static ch.ebu.peachcollector.Constant.*;
import static java.lang.Math.min;

public class PeachCollector {

    public static volatile PeachCollector sharedCollector;
    private static volatile Context applicationContext;
    private static volatile Application application;
    public RoomDatabase database;
    public HashMap<String, Publisher> publishers;
    private HashMap<String, Timer> publisherTimers;
    private HashMap<String, Integer> publisherFailures;
    private Executor dbExecutor;
    private HandlerThread handlerThread;
    private static String deviceID;
    private static boolean limitedTrackingEnabled = false;
    private LifecycleHandler lifecycleHandler = new LifecycleHandler();

    /**
     *  Implementation version of the framework
     *  Value is null by default, it will not be sent unless set
     */
    public static String implementationVersion;

    /**
     *  User unique identifier that should be set as soon as a user is logged in
     */
    public static String userID;

    /**
     *  The Device ID used by the framework is the Advertising ID provided by Apple.
     *  This ID can be reseted or even null. If the Advertising Id is null and the user is not logged in,
     *  collecting of the events should be stopped unless if it is needed for anonymous analytics.
     *  When set to `true`, collection will work even if there is no Device ID and User ID
     *  Default value is `false`.
     */
    public static boolean shouldCollectAnonymousEvents = false;

    /**
     *  When set to `true`, notifications will be emitted when events are recorded and when they are sent
     *  @see Constant for notification name and parameters
     *  Default value is `false`.
     */
    public static boolean isUnitTesting = false;


    /**
     * Minimum duration (in milliseconds) of inactivity that will cause sessionStartTimestamp to be reset when app becomes active
     * Default is 1800 seconds (30 minutes)
     */
    public static long inactivityInterval = 1800000;

    /**
     * Timestamp of the start of the session
     */
    public static long sessionStartTimestamp;

    /**
     * Maximum duration (in days) of storage for an event (should be changed before init)
     * Default is 30 days
     */
    public static int maximumStorageDays = 30;

    /**
     * Maximum duration (in days) of storage for an event (should be changed before init)
     * Default is 30 days
     */
    public static int maximumStoredEvents = 5000;

    /**
     * Timestamp of the start of the session
     * @param app application reference, usually retrieved with `getApplication()`
     */
    public static PeachCollector init(final Application app) {
        application = app;
        applicationContext = application.getApplicationContext();

        sessionStartTimestamp = (new Date()).getTime();
        new Thread(new Runnable() {
            public void run() {
                try {
                    AdvertisingIdClient.AdInfo adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext);
                    limitedTrackingEnabled = adInfo.isLimitAdTrackingEnabled();
                    if (!limitedTrackingEnabled){
                        deviceID = adInfo.getId();
                        if (deviceID == null) {
                            SharedPreferences sPrefs= applicationContext.getSharedPreferences("PeachCollector", MODE_PRIVATE);
                            deviceID = sPrefs.getString("device_id",null);

                            if (deviceID == null) {
                                deviceID = UUID.randomUUID().toString();
                            }
                            SharedPreferences.Editor editor = sPrefs.edit();
                            editor.putString("device_id", deviceID);
                            editor.apply();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        if (sharedCollector == null) {
            synchronized (RoomDatabase.class) {
                if (sharedCollector == null) {
                    sharedCollector = new PeachCollector();
                    sharedCollector.database = RoomDatabase.getDatabase(applicationContext);
                    sharedCollector.publishers = new HashMap<>();
                    sharedCollector.publisherTimers = new HashMap<>();
                    sharedCollector.publisherFailures = new HashMap<>();
                    sharedCollector.dbExecutor = Executors.newSingleThreadExecutor();
                    sharedCollector.handlerThread = new HandlerThread("PeachCollectorPostHandler");
                    sharedCollector.handlerThread.start(); //should call quit() onDestroy of activity ? sharedCollector.handlerThread.quit()
                    sharedCollector.application.registerActivityLifecycleCallbacks(sharedCollector.lifecycleHandler);
                    sharedCollector.checkStoredEvents();
                }
            }
        }
        return sharedCollector;
    }

    protected static Context getApplicationContext() {
        return applicationContext;
    }

    /**
     *  Device unique identifier (advertising identifier when ad tracking is not limited, "Anonymous" otherwise)
     */
    public static String getDeviceID(){
        if (limitedTrackingEnabled || deviceID == null) return "Anonymous";
        return deviceID;

    }

    public static void sendEvent(final Event event) {
        if (shouldCollectAnonymousEvents || !limitedTrackingEnabled || userID != null) {
            if (sharedCollector == null || sharedCollector.dbExecutor == null) return;
            sharedCollector.dbExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    long eventRowID = sharedCollector.database.peachCollectorEventDao().insert(event);

                    for (String publisherName : sharedCollector.publishers.keySet()) {
                        Publisher publisher = sharedCollector.publishers.get(publisherName);

                        if (publisher.shouldProcessEvent(event)) {
                            EventStatus status = new EventStatus((int) eventRowID, publisherName, 0);
                            sharedCollector.database.peachCollectorEventDao().insert(status);
                        }
                    }

                    if (PeachCollector.isUnitTesting) {
                        Intent intent = new Intent();
                        intent.setAction(PEACH_LOG_NOTIFICATION);
                        intent.putExtra(PEACH_LOG_NOTIFICATION_MESSAGE, "+ Event (" + event.getType() + ")");
                        applicationContext.sendBroadcast(intent);
                    }

                    sharedCollector.checkPublishers();
                }
            });
        }
    }

    /**
     *  Adds a publisher to the list of publishers linked to the queue
     *  A custom Publisher can send the events to another end point, potentially in a different format.
     *  @param publisher The publisher to add.
     *  @param publisherName The unique name of the publisher.
     */
    public static void addPublisher(Publisher publisher, String publisherName) {
        if (sharedCollector == null || sharedCollector.dbExecutor == null) return;
        sharedCollector.publishers.put(publisherName, publisher);
        // in case of a crash while sending events, reset related events statuses
        sharedCollector.resetPublisherStatuses(publisherName);
        // send events in the database that are queued for this publisher
        sharedCollector.sendEventsToPublisher(publisherName);
    }

    private void resetPublisherStatuses(final String publisherName){
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<EventStatus> statuses = database.peachCollectorEventDao().getStatuses(publisherName);
                for (EventStatus status: statuses) {
                    status.setStatus(0);
                    database.peachCollectorEventDao().update(status);
                }
            }
        });
    }

    void checkStoredEvents(){
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                database.peachCollectorEventDao().deleteEvents(Integer.MAX_VALUE, maximumStoredEvents);
                database.peachCollectorEventDao().deleteEvents((new Date()).getTime() - (maximumStorageDays * 60 * 60 * 24));
            }
        });
    }

    public static void clean() {
        if (sharedCollector == null || sharedCollector.dbExecutor == null) return;
        sharedCollector.cleanTimers();
        sharedCollector.dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                sharedCollector.database.peachCollectorEventDao().deleteAll();
            }
        });
    }

    public static void flush() {
        if (sharedCollector == null || sharedCollector.dbExecutor == null) return;
        for (String publisherName : sharedCollector.publishers.keySet()) {
            sharedCollector.sendEventsToPublisher(publisherName);
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
                List<EventStatus> statuses = sharedCollector.database.peachCollectorEventDao().getStatuses(publisherName);

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
        for (String publisherName: sharedCollector.publishers.keySet()) {
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
                for (EventStatus status: statuses) {
                    if (status.getStatus() == 1) {
                        // An event is in the process of being sent, stop this process
                        return;
                    }
                    if (status.getStatus() == 0) {
                        if (events.size() < publisher.maxEventsPerBatchAfterOfflineSession) {
                            Event event = database.peachCollectorEventDao().getEvent(status.eventID);
                            events.add(event);
                            status.setStatus(1);
                            database.peachCollectorEventDao().update(status);
                        }
                    }
                }

                if (events.size() == 0) {
                    // There are no events to send, do nothing
                    return;
                }

                cleanTimer(publisherName);

                Handler handler = new Handler(handlerThread.getLooper(), new Handler.Callback() {

                    @Override
                    public boolean handleMessage(Message msg) {
                        boolean sent = msg.what == 0;

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

                        if (!sent) {
                            Integer numberOfFailures = publisherFailures.get(publisherName);
                            numberOfFailures = (numberOfFailures == null) ? 1 : numberOfFailures+1;
                            publisherFailures.put(publisherName, numberOfFailures);

                            startTimerForPublisher(publisherName, false);

                            if (PeachCollector.isUnitTesting) {
                                Intent intent = new Intent();
                                intent.setAction(PEACH_LOG_NOTIFICATION);
                                intent.putExtra(PEACH_LOG_NOTIFICATION_MESSAGE, publisherName + " : Failed to publish events");
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
                                intent.setAction(PEACH_LOG_NOTIFICATION);
                                intent.putExtra(PEACH_LOG_NOTIFICATION_MESSAGE, publisherName + " : Published " + events.size() + " events");
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


    private int interval(String publisherName, boolean shouldFollowPolicy) {
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

    static void checkInactivity() {
        long currentTimestamp = (new Date()).getTime();
        SharedPreferences sPrefs= applicationContext.getSharedPreferences("PeachCollector", MODE_PRIVATE);
        sessionStartTimestamp = sPrefs.getLong(SESSION_START_TIMESTAMP_SPREF_KEY, currentTimestamp);
        long lastActiveTimestamp = sPrefs.getLong(SESSION_LAST_ACTIVE_TIMESTAMP_SPREF_KEY, currentTimestamp);

        if (currentTimestamp - lastActiveTimestamp > inactivityInterval) {
            sessionStartTimestamp = currentTimestamp;
            sPrefs.edit().putLong(SESSION_START_TIMESTAMP_SPREF_KEY, sessionStartTimestamp).apply();
        }
        sPrefs.edit().putLong(SESSION_LAST_ACTIVE_TIMESTAMP_SPREF_KEY, currentTimestamp).apply();
    }
}
