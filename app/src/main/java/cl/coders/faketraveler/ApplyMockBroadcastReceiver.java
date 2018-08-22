package cl.coders.faketraveler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;


public class ApplyMockBroadcastReceiver extends BroadcastReceiver {

    Intent serviceIntent;
    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    public ApplyMockBroadcastReceiver() {
        alarmManager = MainActivity.alarmManager;
        serviceIntent = MainActivity.serviceIntent;
        pendingIntent = MainActivity.pendingIntent;
        sharedPref = MainActivity.sharedPref;
        editor = MainActivity.editor;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            double lat = Double.parseDouble(sharedPref.getString("lat", "0"));
            double lng = Double.parseDouble(sharedPref.getString("lng", "0"));

            MainActivity.exec(lat, lng);

            if (!MainActivity.hasEnded()) {
                MainActivity.setAlarm(MainActivity.timeInterval);
            } else {
                MainActivity.stopMockingLocation();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
