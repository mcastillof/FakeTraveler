package cl.coders.faketraveler;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

public class ApplyMockBroadcastReceiver extends BroadcastReceiver {

    private int SCHEDULE_REQUEST_CODE = 1;
    Intent serviceIntent;
    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    int timesLeft;
    Context context;

    public ApplyMockBroadcastReceiver() {
        alarmManager = MainActivity.alarmManager;
        serviceIntent = MainActivity.serviceIntent;
        pendingIntent = MainActivity.pendingIntent;
        SharedPreferences sharedPref = MainActivity.sharedPref;
        timesLeft = MainActivity.timesLeft;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);

        Double lat = Double.parseDouble(sharedPref.getString("Lat","0"));
        Double lng = Double.parseDouble(sharedPref.getString("Lng","0"));

        int timeInterval = Integer.parseInt(sharedPref.getString("timeInterval", "10"));
        int howManyTimes = Integer.parseInt(sharedPref.getString("howManyTimes","1"));

        if (howManyTimes == MainActivity.KEEP_GOING)
            timesLeft = MainActivity.INFINITE;

        try {
            MockLocationProvider mockNetwork = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);
            mockNetwork.pushLocation(lat, lng);
            MockLocationProvider mockGps = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
            mockGps.pushLocation(lat, lng);
        }
        catch (Exception e) {
            Toast.makeText(context, context.getResources().getString(R.string.ApplyMockBroadRec_MockNotApplied), Toast.LENGTH_LONG).show();
            MainActivity.changeButtonToApply();
            return;
        }

        if (timesLeft - 1 > 0) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + timeInterval * 1000, pendingIntent);
                    }
                    else
                    {
                        alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + timeInterval * 1000, pendingIntent);
                    }
                } else {
                    alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + timeInterval * 1000, pendingIntent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
        {
            try {
                MainActivity.changeButtonToApply();
            }
            catch(Exception e)
            {
                Toast.makeText(context, context.getResources().getString(R.string.ApplyMockBroadRec_Closed), Toast.LENGTH_LONG).show();
            }

        }

        MainActivity.timesLeft--;
    }

}
