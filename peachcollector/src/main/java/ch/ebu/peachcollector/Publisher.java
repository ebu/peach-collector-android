package ch.ebu.peachcollector;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static ch.ebu.peachcollector.Constant.*;

public class Publisher {

    /**
     *  End point of the publisher, where all requests should be sent
     */
    public String serviceURL;

    /**
     * The interval in seconds at which events are sent to the server.
     * 0 means no buffering, every event is sent as soon as it is queued.
     * Default value is 20 seconds.
     */
    public Integer interval = 20;

    /**
     *  Number of events queued that triggers the publishing process even if the desired interval hasn't been reached.
     *  1 means no buffering, every event is sent as soon as it is queued.
     *  Default value is 20 events.
     */
    public Integer maxEventsPerBatch = 20;

    /**
     *  Maximum number of events that can be sent in a single batch.
     *  Default value is 1000 events.
     */
    public Integer maxEventsPerBatchAfterOfflineSession = 1000;

    /**
     *  How the publisher should behave after an offline period
     *  Default is `send all`
     */
    public GoBackOnlinePolicy gotBackPolicy = GoBackOnlinePolicy.SEND_ALL;


    private Map<String, Object> clientInfo;
    private Map<String, Object> deviceInfo;

    public Publisher() {}

    public Publisher(String siteKey) {
        serviceURL = "https://pipe-collect.ebu.io/v3/collect?s=" + siteKey;
    }

    /**
     *  Return `YES` if the the publisher can process the event. This is used when an event is added to the queue to check
     *  if said event should be added to the publisher's queue.
     *  @param event The event to be queued.
     *  @return `YES` if the the publisher can process the event, `NO` otherwise.
     */
    public boolean shouldProcessEvent(Event event){
        return true;
    }

    public Map<String, Object> clientInfo(){
        if (clientInfo != null) return clientInfo;
        clientInfo = new HashMap<>();

        Context appContext = PeachCollector.getApplicationContext();
        String packageName = appContext.getPackageName();
        if (packageName == null) packageName = "unknown";
        clientInfo.put(CLIENT_APP_ID_KEY, packageName);
        clientInfo.put(CLIENT_TYPE_KEY, "mobileapp");


        try {
            PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(packageName, 0);
            String version = pInfo.versionName;
            int versionCode = pInfo.versionCode;
            clientInfo.put(CLIENT_APP_VERSION_KEY, version + "b" + versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        ApplicationInfo applicationInfo = appContext.getApplicationInfo();
        CharSequence applicationLabel = appContext.getPackageManager().getApplicationLabel(applicationInfo);
        String applicationName = "";
        if (applicationLabel != null) {
            applicationName = applicationLabel.toString();
        }
        else {
            applicationName = (applicationInfo.labelRes == 0 && applicationInfo.nonLocalizedLabel != null) ? applicationInfo.nonLocalizedLabel.toString() : appContext.getString(applicationInfo.labelRes);
        }
        if (applicationName.length() == 0) applicationName = "unknown";
        clientInfo.put(CLIENT_APP_NAME_KEY, applicationName);
        clientInfo.put(CLIENT_ID_KEY, PeachCollector.getDeviceID());

        clientInfo.put(DEVICE_KEY, deviceInfo());
        clientInfo.put(OS_KEY, osInfo());

        return clientInfo;
    }

    public static Map<String, Object> deviceInfo(){
        HashMap<String, Object> deviceInfo = new HashMap<>();

        String screenResolution = "unknown";
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) PeachCollector.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            display.getMetrics(displayMetrics);

            int mHeightPixels = displayMetrics.heightPixels;
            int mWidthPixels = displayMetrics.widthPixels;

            if (Build.VERSION.SDK_INT >= 17) {
                try {
                    Point realSize = new Point();
                    Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
                    mWidthPixels = realSize.x;
                    mHeightPixels = realSize.y;
                } catch (Exception ignored) { }
            }

            screenResolution = mWidthPixels + "x" + mHeightPixels;
        }

        boolean isTablet = PeachCollector.getApplicationContext().getResources().getBoolean(R.bool.isTablet);

        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        double offsetFromUtc = tz.getOffset(now.getTime()) / 3600000.0;

        deviceInfo.put(DEVICE_TYPE_KEY, isTablet ? Constant.ClientDeviceType.Tablet : Constant.ClientDeviceType.Phone);
        deviceInfo.put(DEVICE_VENDOR_KEY, android.os.Build.MANUFACTURER + ", " + android.os.Build.BRAND);
        deviceInfo.put(DEVICE_MODEL_KEY, android.os.Build.MODEL + ", " + android.os.Build.PRODUCT);
        deviceInfo.put(DEVICE_SCREEN_SIZE_KEY, screenResolution);
        deviceInfo.put(DEVICE_LANGUAGE_KEY, Locale.getDefault().getDisplayLanguage());
        deviceInfo.put(DEVICE_TIMEZONE_KEY, offsetFromUtc);

        return deviceInfo;
    }

    public static Map<String, String> osInfo(){
        HashMap<String, String> osInfo = new HashMap<>();
        osInfo.put(OS_VERSION_KEY, System.getProperty("os.version") + " (" + android.os.Build.VERSION.INCREMENTAL + ") API" + android.os.Build.VERSION.SDK_INT);
        osInfo.put(OS_NAME_KEY, "Android");
        return osInfo;
    }


    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public void processEvents(List<Event> events, Handler finishHandler){

        HashMap map = new HashMap();
        map.put(PEACH_SCHEMA_VERSION_KEY, "1.0.3");
        map.put(PEACH_FRAMEWORK_VERSION_KEY, BuildConfig.VERSION_NAME + "b" + BuildConfig.VERSION_CODE);
        if (PeachCollector.implementationVersion != null) {
            map.put(PEACH_IMPLEMENTATION_VERSION_KEY, PeachCollector.implementationVersion);
        }
        map.put(SESSION_START_TIMESTAMP_KEY, PeachCollector.sessionStartTimestamp);
        map.put(SENT_TIMESTAMP_KEY, (new Date()).getTime());
        map.put(CLIENT_KEY, clientInfo());

        ArrayList eventList = new ArrayList();
        for (Event event :events) {
            Map<String, Object> eventJSON = event.jsonRepresentation();
            if (eventJSON != null) {
                eventList.add(eventJSON);
            }
        }

        map.put(EVENTS_KEY, eventList);

        if (PeachCollector.userID != null) {
            map.put(USER_ID_KEY, PeachCollector.userID);
        }

        JSONObject obj=new JSONObject(map);

        if (PeachCollector.isUnitTesting) {
            Intent intent = new Intent();
            intent.setAction(PEACH_LOG_NOTIFICATION);
            intent.putExtra(PEACH_LOG_NOTIFICATION_PAYLOAD, obj.toString());
            PeachCollector.getApplicationContext().sendBroadcast(intent);
        }

        new PostTask(finishHandler).execute(obj.toString());
    }




    public class PostTask extends AsyncTask<String, String, String> {
        Handler completionHandler;

        public PostTask(Handler handler){
            completionHandler = handler;
        }

        @Override
        protected String doInBackground(String... params) {
            String data = params[0];
            int responseCode = 0;
            try{
                URL url = new URL(serviceURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestMethod("POST");//important
                httpURLConnection.connect();
                //write data to the server using BufferedWriter
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpURLConnection.getOutputStream()));
                writer.write(data);
                writer.flush();
                //get response code and check if valid (HTTP OK)
                responseCode = httpURLConnection.getResponseCode();
                if(responseCode != 201){
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    while((line = reader.readLine()) != null){
                        stringBuilder.append(line);
                    }
                    stringBuilder.insert(0, "Error: ");
                    return stringBuilder.toString();
                }
                return null;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Error " + responseCode;
        }

        @Override
        protected void onPostExecute(String result) {
            if (completionHandler != null) {
                completionHandler.sendEmptyMessage(result != null ? 1 : 0);
            }
        }
    }

}
