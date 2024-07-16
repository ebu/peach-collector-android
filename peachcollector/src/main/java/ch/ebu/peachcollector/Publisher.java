package ch.ebu.peachcollector;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static android.content.Context.MODE_PRIVATE;
import static ch.ebu.peachcollector.Constant.*;

import androidx.annotation.Nullable;

public class Publisher {

    /**
     *  End point of the publisher, where all requests should be sent
     */
    public String serviceURL;

    /**
     *  URL for the remote configuration
     */
    public String remoteConfigurationURL;

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
     *  Interval between heartbeats when tracking a media.
     *  Default value is 5 seconds.
     */
    public Integer playerTrackerHeartbeatInterval = 5;

    /**
     *  How the publisher should behave after an offline period
     *  Default is `send all`
     */
    public GoBackOnlinePolicy gotBackPolicy = GoBackOnlinePolicy.SEND_ALL;


    private Map<String, Object> clientInfo;
    private Map<String, Object> deviceInfo;
    private JSONObject remoteConfiguration;

    public Publisher() {}

    public Publisher(String siteKey) {
        if (!TextUtils.isEmpty(siteKey)) {
            serviceURL = "https://pipe-collect.ebu.io/v3/collect?s=" + siteKey;
        }
    }

    public Publisher(String siteKey, String remoteURL) {
        if (!TextUtils.isEmpty(siteKey) && !TextUtils.isEmpty(remoteURL)) {
            serviceURL = "https://pipe-collect.ebu.io/v3/collect?s=" + siteKey;
            remoteConfigurationURL = remoteURL;

            Context appContext = PeachCollector.getApplicationContext();

            SharedPreferences sPrefs= appContext.getSharedPreferences("PeachCollector", MODE_PRIVATE);
            String textJson = sPrefs.getString(remoteConfigurationURL, null);
            if (textJson != null) {
                try {
                    remoteConfiguration = new JSONObject(textJson);
                } catch (JSONException e) { }
            }
            checkConfig();
        }
    }

    private void checkConfig() {
        String expiryDateKey = remoteConfigurationURL + "_date";

        Context appContext = PeachCollector.getApplicationContext();

        long currentTimestamp = (new Date()).getTime();
        SharedPreferences sPrefs= appContext.getSharedPreferences("PeachCollector", MODE_PRIVATE);
        long expiryTimestamp = sPrefs.getLong(expiryDateKey, currentTimestamp);

        if (currentTimestamp > expiryTimestamp) {
            remoteConfiguration = null;
        }

       if (remoteConfiguration != null) {
           try {
               if (remoteConfiguration.has("max_batch_size")) {
                   maxEventsPerBatch = remoteConfiguration.getInt("max_batch_size");
               }
               if (remoteConfiguration.has("max_events_per_request")) {
                   maxEventsPerBatchAfterOfflineSession = remoteConfiguration.getInt("max_events_per_request");
               }
               if (remoteConfiguration.has("flush_interval_sec")) {
                   interval = remoteConfiguration.getInt("flush_interval_sec");
               }
               if (remoteConfiguration.has("heartbeat_frequency_sec")) {
                   playerTrackerHeartbeatInterval = remoteConfiguration.getInt("heartbeat_frequency_sec");
               }
           } catch (JSONException e) {
               // Do nothing, we already have default values
           }
       }
       else {
           new RemoteTask().execute(remoteConfigurationURL);
       }
    }

    /**
     *  Return `YES` if the the publisher can process the event. This is used when an event is added to the queue to check
     *  if said event should be added to the publisher's queue.
     *  @param event The event to be queued.
     *  @return `YES` if the the publisher can process the event, `NO` otherwise.
     */
    public boolean shouldProcessEvent(Event event) {
        if (remoteConfiguration != null && remoteConfiguration.has("filter")) {
            try {
                JSONArray eventsFilter = remoteConfiguration.getJSONArray("filter");
                for (int i = 0; i < eventsFilter.length(); i++) {
                    String event_type = eventsFilter.getString(i);
                    if (event_type.equalsIgnoreCase(event.getType())) {
                        return true;
                    }
                }
            } catch (JSONException e) {
                return false;
            }
            return false;
        }

        return !TextUtils.isEmpty(serviceURL);
    }

    @Nullable
    private Map<String, Object> customClientFields;

    private void addClientObject(String key, Object value) {
        if (value == null) {
            removeCustomClientField(key);
            return;
        }

        if (customClientFields == null) {
            customClientFields = new HashMap<>();
        }

        customClientFields.put(key, value);
    }

    /**
     * Add a custom string field to the client
     */
    public void addClientField(String key, String value){
        addClientObject(key, value);
    }

    /**
     * Add a custom number field to the client
     */
    public void addClientField(String key, Number value){
        addClientObject(key, value);
    }

    /**
     * Add a custom boolean field to the client
     */
    public void addClientField(String key, Boolean value){
        addClientObject(key, value);
    }

    /**
     * Remove a custom client field previously added
     */
    public void removeCustomClientField(String key){
        if (customClientFields != null && customClientFields.containsKey(key)){
            customClientFields.remove(key);
            if (customClientFields.size() == 0) customClientFields = null;
        }
    }

    /**
     * Retrieve a custom client field previously added.
     * @return null if the field was not found
     */
    @Nullable public Object getCustomClientField(String key){
        if (customClientFields != null) return customClientFields.get(key);
        return null;
    }


    public void invalidateClientInfo(){
        clientInfo = null;
    }
    public Map<String, Object> clientInfo(){
        if (clientInfo != null) {
            if (customClientFields != null) {
                HashMap<String, Object> newClientInfo = new HashMap<>();
                newClientInfo.putAll(clientInfo);
                newClientInfo.putAll(customClientFields);
                return newClientInfo;
            }
            return clientInfo;
        }
        clientInfo = new HashMap<>();
        if (customClientFields != null) {
            clientInfo.putAll(customClientFields);
        }

        Context appContext = PeachCollector.getApplicationContext();

        String packageName = PeachCollector.appID;
        if (packageName == null) {
            packageName = appContext.getPackageName();
        }
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
        clientInfo.put(CLIENT_USER_IS_LOGGED_IN_KEY, PeachCollector.getUserIsLoggedIn());

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

        Gson gson = new Gson();
        String obj = gson.toJson(map);

        if (PeachCollector.isUnitTesting) {
            Intent intent = new Intent();
            intent.setAction(PEACH_LOG_NOTIFICATION);
            intent.putExtra(PEACH_LOG_NOTIFICATION_PAYLOAD, obj);
            PeachCollector.getApplicationContext().sendBroadcast(intent);
        }

        new PostTask(finishHandler).execute(obj);
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

    protected class RemoteTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            URLConnection urlConn = null;
            BufferedReader bufferedReader = null;
            try {
                URL url = new URL(params[0]);
                urlConn = url.openConnection();
                bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

                StringBuffer stringBuffer = new StringBuffer();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }

                return new JSONObject(stringBuffer.toString());
            }
            catch(Exception ex) {
                return null;
            }
            finally {
                if(bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(JSONObject response)
        {
            if(response != null)
            {
                remoteConfiguration = response;
                double maxCacheHours = 1;

                try {
                    maxCacheHours = remoteConfiguration.getDouble("max_cache_hours");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Context appContext = PeachCollector.getApplicationContext();

                long currentTimestamp = (new Date()).getTime();
                long expiryTimestamp = (long) (currentTimestamp + (maxCacheHours*60*60*1000) + 10000);
                SharedPreferences sPrefs= appContext.getSharedPreferences("PeachCollector", MODE_PRIVATE);

                sPrefs.edit().putLong(remoteConfigurationURL + "_date", expiryTimestamp).apply();

                checkConfig();
            }
        }
    }
}
