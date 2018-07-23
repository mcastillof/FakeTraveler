package cl.coders.faketraveler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.IBinder;
import android.widget.Toast;

public class ApplyMockService extends Service {

    private int SCHEDULE_REQUEST_CODE = 1;
    Intent serviceIntent;
    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    int timesLeft;
    Context context;

    public ApplyMockService() {
        alarmManager = MainActivity.alarmManager;
        serviceIntent = MainActivity.serviceIntent;
        pendingIntent = MainActivity.pendingIntent;
        SharedPreferences sharedPref = MainActivity.sharedPref;
        timesLeft = MainActivity.timesLeft;
        context = MainActivity.context;
    }

    @Override
    public void onCreate() {
        // The service is being created
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);

        Double lat = Double.parseDouble(sharedPref.getString("Lat","0"));
        Double lng = Double.parseDouble(sharedPref.getString("Lng","0"));

        int timeInterval = Integer.parseInt(sharedPref.getString("timeInterval", "10"));
        int howManyTimes = Integer.parseInt(sharedPref.getString("howManyTimes","1"));

        if (howManyTimes == MainActivity.KEEP_GOING)
            timesLeft = MainActivity.INFINITE;
        //else
        //    timesLeft = sharedPref.getInt("timesLeft", 1) ;

        try {
            MockLocationProvider mockNetwork = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, this);
            mockNetwork.pushLocation(lat, lng);
            MockLocationProvider mockGps = new MockLocationProvider(LocationManager.GPS_PROVIDER, this);
            mockGps.pushLocation(lat, lng);
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Mocked location not applied. You must enable mock location in developer settings. Service stoped.", Toast.LENGTH_LONG).show();
            MainActivity.changeButtonToApply();
            return -1;
        }

        if (timesLeft - 1 > 0) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + timeInterval * 1000, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + timeInterval * 1000, pendingIntent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
        {
            MainActivity.changeButtonToApply();
        }

        MainActivity.timesLeft--;

        int mStartMode = 0;
        return mStartMode;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        IBinder mBinder = null;
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()

        boolean mAllowRebind = false;
        return mAllowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
    }

}



