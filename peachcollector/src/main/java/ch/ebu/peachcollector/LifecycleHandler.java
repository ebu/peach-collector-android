package ch.ebu.peachcollector;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.Date;

import static android.content.Context.MODE_PRIVATE;
import static ch.ebu.peachcollector.Constant.SESSION_LAST_ACTIVE_TIMESTAMP_SPREF_KEY;

public class LifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private static int resumed;
    private static int paused;
    private static int started;
    private static int stopped;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
        PeachCollector.checkInactivity();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        ++paused;
        if(resumed <= paused) { // Application is not in foreground
            PeachCollector.flush();
            updateLastActiveTimestamp();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        ++started;
        PeachCollector.checkInactivity();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        ++stopped;
        if(started <= stopped) { // Application is not visible
            PeachCollector.flush();
            updateLastActiveTimestamp();
        }
    }

    void updateLastActiveTimestamp(){
        SharedPreferences sPrefs= PeachCollector.getApplicationContext().getSharedPreferences("PeachCollector", MODE_PRIVATE);
        sPrefs.edit().putLong(SESSION_LAST_ACTIVE_TIMESTAMP_SPREF_KEY, (new Date()).getTime()).apply();
    }

    public static boolean isApplicationVisible() {
        return started > stopped;
    }

    public static boolean isApplicationInForeground() {
        return resumed > paused;
    }
}
