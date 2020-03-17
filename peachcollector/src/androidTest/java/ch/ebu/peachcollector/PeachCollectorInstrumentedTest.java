package ch.ebu.peachcollector;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Date;

import static ch.ebu.peachcollector.Constant.PEACH_LOG_NOTIFICATION;
import static ch.ebu.peachcollector.Constant.PEACH_LOG_NOTIFICATION_MESSAGE;
import static ch.ebu.peachcollector.Constant.PEACH_LOG_NOTIFICATION_PAYLOAD;

import static org.junit.Assert.*;



/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(JUnit4.class)
public class PeachCollectorInstrumentedTest{

    public static String PUBLISHER_NAME = "DefaultPublisher";
    public static String PUBLISHER_NAME2 = "SecondPublisher";
    public static String PUBLISHER_NAME3 = "ThirdPublisher";
    public LogReceiver mReceiver = new LogReceiver();

    private Context mContext;

    public String currentEventType;


    @Before
    public void initializeModule() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        PeachCollector.isUnitTesting = true;
        PeachCollector.shouldCollectAnonymousEvents = true;
        Application app = (Application) mContext.getApplicationContext();
        PeachCollector.init(app);
        PeachCollector.clean();
        Publisher publisher = new Publisher("zzebu00000000017");
        PeachCollector.addPublisher(publisher, PUBLISHER_NAME);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PEACH_LOG_NOTIFICATION);
        mContext.registerReceiver(mReceiver, filter);
    }

    @After
    public void tearDown(){
        mContext.unregisterReceiver(mReceiver);
    }

    @Test
    public void testInitialization(){
        assertNotNull("PeachCollector is not initialized", PeachCollector.sharedCollector);
        assertNotNull("PeachCollector start timestamp not set", PeachCollector.sessionStartTimestamp);
        assertNotNull("PeachCollector CoreData stack is not initialized", PeachCollector.sharedCollector.database);
        assertNotNull("PeachCollector publishers not initialized", PeachCollector.sharedCollector.publishers);
        assertNotNull("PeachCollector publisher was not added", PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME));
    }

    @Test
    public void testPublisherConfiguration() throws InterruptedException {
        Publisher publisher = PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME);
        publisher.interval = 2;
        publisher.maxEventsPerBatch = 3;


        for (int i=0; i<3; i++) {
            Event.sendPageView("page00"+i, null, "reco00");
        }

        Thread.sleep(5000);
        boolean b = mReceiver.testSuccess;
        assertTrue("The right number of events was sent",b);
    }



    @Test
    public void testWorkingPublisherWith1000Events() throws InterruptedException {
        Publisher publisher = PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME);
        publisher.interval = 1;
        publisher.maxEventsPerBatch = 2;

        for (int i=0; i<1000; i++) {
            Event.sendPageView("page00"+i, null, "reco00");
        }

        Thread.sleep(20000);
        int b = mReceiver.publishedEventsCount;
        assertEquals("The right number of events was sent (1000)", 1000, b);
    }



    @Test
    public void testFailingPublisherWith1000Events() throws InterruptedException {
        Publisher publisher = PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME);
        publisher.serviceURL = "";
        publisher.interval = 1;
        publisher.maxEventsPerBatch = 2;

        for (int i=0; i<1000; i++) {
            Event.sendPageView("page00"+i, null, "reco00");
        }

        Thread.sleep(1000);
        int b = mReceiver.publishedEventsCount;
        assertEquals(0, b);


        publisher.serviceURL = "https://pipe-collect.ebu.io/v3/collect?s=zzebu00000000017";
        Thread.sleep(10000);
        b = mReceiver.publishedEventsCount;
        assertEquals("The right number of events was sent (1000)", 1000, b);

    }

    @Test
    public void test3PublishersWith1000Events() throws InterruptedException {
        Publisher publisher = PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME);
        publisher.interval = 1;
        publisher.maxEventsPerBatch = 2;

        Publisher publisher2 = new Publisher("zzebu00000000017");
        publisher2.interval = 5;
        publisher2.maxEventsPerBatch = 5;
        PeachCollector.addPublisher(publisher2, PUBLISHER_NAME2);

        Publisher publisher3 = new Publisher("zzebu00000000017");
        publisher3.interval = 50;
        publisher3.maxEventsPerBatch = 1000;
        PeachCollector.addPublisher(publisher3, PUBLISHER_NAME3);

        for (int i=0; i<1000; i++) {
            Event.sendPageView("page00"+i, null, "reco00");
        }

        Thread.sleep(50000);
        int b = mReceiver.publishedEventsCount;
        int b2 = mReceiver.publishedEventsCount2;
        int b3 = mReceiver.publishedEventsCount3;

        assertEquals("The right number of events was sent (1000) - " + PUBLISHER_NAME, 1000, b);
        assertEquals("The right number of events was sent (1000) - " + PUBLISHER_NAME2, 1000, b2);
        assertEquals("The right number of events was sent (1000) - " + PUBLISHER_NAME3, 1000, b3);

    }

    @Test
    public void testRecommendationHit() throws InterruptedException {
        Publisher publisher = PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME);
        publisher.interval = 1;
        publisher.maxEventsPerBatch = 1;

        EventContextComponent carouselComponent = new EventContextComponent();
        carouselComponent.type = "Carousel";
        carouselComponent.name = "recoCarousel";
        carouselComponent.version = "1.0";

        currentEventType = "recommendationHit";

        Event.sendRecommendationHit("reco00", "media01", 1, null, null, carouselComponent);

        Thread.sleep(2000);
    }

    @Test
    public void testRecommendationDisplayed() throws InterruptedException {
        Publisher publisher = PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME);
        publisher.interval = 1;
        publisher.maxEventsPerBatch = 1;

        EventContextComponent carouselComponent = new EventContextComponent();
        carouselComponent.type = "Carousel";
        carouselComponent.name = "recoCarousel";
        carouselComponent.version = "1.0";

        ArrayList<String> items = new ArrayList<>();
        items.add("media00");
        items.add("media01");
        items.add("media02");
        items.add("media03");

        Event.sendRecommendationDisplayed("reco00", items , null, null, carouselComponent);
        currentEventType = "recommendationDisplayed";

        Thread.sleep(2000);
    }


    @Test
    public void testMediaSeekEvent() throws InterruptedException {
        Publisher publisher = PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME);
        publisher.interval = 1;
        publisher.maxEventsPerBatch = 1;

        EventContextComponent carouselComponent = new EventContextComponent();
        carouselComponent.type = "player";
        carouselComponent.name = "bottomPlayer";
        carouselComponent.version = "1.0";

        EventContext eventContext = EventContext.mediaContext("reco00", "playlist", null, null, carouselComponent);
        eventContext.add("testKey", "testValue");

        EventProperties props = new EventProperties();
        props.audioMode = Constant.Media.AudioMode.Normal;
        props.playbackPosition = 10;
        props.previousPlaybackPosition = 5;
        props.startMode = Constant.Media.StartMode.Normal;
        props.isPlaying = false;

        currentEventType = "mediaSeek";
        Event.sendMediaSeek("media01", props, eventContext, null);
        props.previousPlaybackPosition = null;

        Thread.sleep(2000);
    }


    @Test
    public void testMaxEvents() throws InterruptedException {
        Publisher publisher = PeachCollector.sharedCollector.publishers.get(PUBLISHER_NAME);
        publisher.serviceURL = "";
        publisher.interval = 1;
        publisher.maxEventsPerBatch = 1000;

        for (int i=0; i<1000; i++) {
            Event.sendPageView("page"+i, null, "reco00");
        }

        int b = mReceiver.publishedEventsCount;
        assertEquals(0, b);

        PeachCollector.maximumStoredEvents = 500;
        PeachCollector.sharedCollector.checkStoredEvents();
        Thread.sleep(1000);

        currentEventType = "pageViewMax";
        publisher.serviceURL = "https://pipe-collect.ebu.io/v3/collect?s=zzebu00000000017";
        Thread.sleep(10000);
        b = mReceiver.publishedEventsCount;
        boolean test = mReceiver.testMaxSuccess;
        assertEquals("The right number of events was sent (500)", 500, b);
        assertTrue(test);
    }


    public class LogReceiver extends BroadcastReceiver {

        // test1
        boolean testSuccess;

        // test2
        int publishedEventsCount = 0;

        // test4
        int publishedEventsCount2 = 0;
        int publishedEventsCount3 = 0;

        // testMax
        boolean testMaxSuccess;

        public LogReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(PEACH_LOG_NOTIFICATION_MESSAGE);
            String payload = intent.getStringExtra(PEACH_LOG_NOTIFICATION_PAYLOAD);
            if (msg != null) {
                if(msg.contains("3 events")) {
                    testSuccess = true;
                }

                String numbersInMessage = msg.replaceAll("[\\D]", "");
                if(numbersInMessage.length()>0) {
                    if (msg.contains(PUBLISHER_NAME)) {
                        int i = Integer.parseInt(numbersInMessage);
                        publishedEventsCount = publishedEventsCount + i;
                    } else if (msg.contains(PUBLISHER_NAME2)) {
                        int i = Integer.parseInt(numbersInMessage);
                        publishedEventsCount2 = publishedEventsCount2 + i;
                    } else if (msg.contains(PUBLISHER_NAME3)) {
                        int i = Integer.parseInt(numbersInMessage);
                        publishedEventsCount3 = publishedEventsCount3 + i;
                    }
                }
            }
            else if(payload != null && currentEventType != null){
                Log.e("PEACH COLLECTOR", "payload received");
                if (currentEventType.equalsIgnoreCase("recommendationHit")){
                    try {
                        JSONObject json = new JSONObject(payload);
                        JSONArray jArray = json.getJSONArray("events");
                        assertEquals(jArray.length(), 1);
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject event = jArray.getJSONObject(i);
                            assertEquals("recommendation_hit", event.getString("type"));
                            assertTrue(event.getLong("event_timestamp") < (new Date()).getTime());
                            assertTrue(event.getLong("event_timestamp") > (new Date()).getTime() - 10000);
                            assertEquals("reco00", event.getString("id"));

                            JSONObject eventContext = event.getJSONObject("context");
                            assertEquals(1, eventContext.getInt("hit_index"));
                            assertEquals("media01", eventContext.getString("item_id"));

                            JSONObject component = eventContext.getJSONObject("component");
                            assertEquals("recoCarousel", component.getString("name"));
                            assertEquals("Carousel", component.getString("type"));
                            assertEquals("1.0", component.getString("version"));

                            if ()
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.e("PEACH COLLECTOR", payload);
                }
                else if (currentEventType.equalsIgnoreCase("recommendationDisplayed")){
                    try {
                        JSONObject json = new JSONObject(payload);
                        JSONArray jArray = json.getJSONArray("events");
                        assertEquals(jArray.length(), 1);
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject event = jArray.getJSONObject(i);
                            assertEquals("recommendation_displayed", event.getString("type"));
                            assertTrue(event.getLong("event_timestamp") < (new Date()).getTime());
                            assertTrue(event.getLong("event_timestamp") > (new Date()).getTime() - 10000);
                            assertEquals("reco00", event.getString("id"));

                            JSONObject eventContext = event.getJSONObject("context");
                            JSONArray items = eventContext.getJSONArray("items");
                            assertEquals("media00", items.getString(0));
                            assertEquals("media01", items.getString(1));

                            JSONObject component = eventContext.getJSONObject("component");
                            assertEquals("recoCarousel", component.getString("name"));
                            assertEquals("Carousel", component.getString("type"));
                            assertEquals("1.0", component.getString("version"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.e("PEACH COLLECTOR", payload);
                }
                else if (currentEventType.equalsIgnoreCase("mediaSeek")){
                    try {
                        JSONObject json = new JSONObject(payload);
                        JSONArray jArray = json.getJSONArray("events");
                        assertEquals(jArray.length(), 1);
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject event = jArray.getJSONObject(i);
                            assertEquals("media_seek", event.getString("type"));
                            assertTrue(event.getLong("event_timestamp") < (new Date()).getTime());
                            assertTrue(event.getLong("event_timestamp") > (new Date()).getTime() - 10000);
                            assertEquals("media01", event.getString("id"));

                            JSONObject eventContext = event.getJSONObject("context");
                            assertEquals("reco00", eventContext.getString("id"));
                            assertEquals("playlist", eventContext.getString("type"));
                            assertEquals("testValue", eventContext.getString("testKey"));

                            JSONObject eventProps = event.getJSONObject("props");
                            assertEquals(5, eventProps.getLong("previous_playback_position_s"));
                            assertEquals(10, eventProps.getLong("playback_position_s"));
                            assertEquals("normal", eventProps.getString("start_mode"));
                            assertEquals("normal", eventProps.getString("audio_mode"));
                            assertFalse(eventProps.getBoolean("is_playing"));

                            JSONObject component = eventContext.getJSONObject("component");
                            assertEquals("bottomPlayer", component.getString("name"));
                            assertEquals("player", component.getString("type"));
                            assertEquals("1.0", component.getString("version"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.e("PEACH COLLECTOR", payload);
                }
                else if(currentEventType.equalsIgnoreCase("pageViewMax")){
                    try {
                        JSONObject json = new JSONObject(payload);
                        JSONArray jArray = json.getJSONArray("events");
                        assertEquals(jArray.length(), 1);
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject event = jArray.getJSONObject(i);
                            if(event.getString("id").equalsIgnoreCase("page800")) {
                                testMaxSuccess = true;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }


                testSuccess = false;
            }
        }
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("ch.ebu.peachcollector.test", appContext.getPackageName());
    }
}
