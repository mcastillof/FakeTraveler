package cl.coders.faketraveler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    static final String sharedPrefKey = "cl.coders.mockposition.sharedpreferences";
    static final int KEEP_GOING = 0;
    static final int INFINITE = 2;
    static private int SCHEDULE_REQUEST_CODE = 1;
    public static Intent serviceIntent;
    public static PendingIntent pendingIntent;
    public static AlarmManager alarmManager;
    static Button button0;
    static Button button1;
    static WebView webView;
    static EditText editTextLat;
    static EditText editTextLng;
    static Context context;
    static SharedPreferences sharedPref;
    static Double lat;
    static Double lng;
    static int timesLeft;
    static int timeInterval;
    static int howManyTimes;
    static long endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        webView = findViewById(R.id.webView0);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.loadUrl("file:///android_asset/map.html");
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        button0 = (Button) findViewById(R.id.button0);
        button1 = (Button) findViewById(R.id.button1);
        editTextLat = findViewById(R.id.editText0);
        editTextLng = findViewById(R.id.editText1);

        button0.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                applyLocation();
            }

        });

        button1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent myIntent = new Intent(getBaseContext(), MoreActivity.class);
                startActivity(myIntent);
            }

        });

        editTextLat.setText(sharedPref.getString("Lat", ""));
        editTextLng.setText(sharedPref.getString("Lng", ""));
        endTime = sharedPref.getLong("endTime", 0);

        if(endTime > System.currentTimeMillis())
        {
            changeButtonToStop();
        }
    }

    protected static void applyLocation()
    {
        if (editTextLat.getText().toString().isEmpty() || editTextLng.getText().toString().isEmpty())
        {
            Toast.makeText(context, "No Lat or Lng value. Long press in the map where you want to be located", Toast.LENGTH_LONG).show();
            return;
        }

        lat = Double.parseDouble(editTextLat.getText().toString());
        lng = Double.parseDouble(editTextLng.getText().toString());

        timeInterval = Integer.parseInt(sharedPref.getString("timeInterval", "10"));
        howManyTimes = Integer.parseInt(sharedPref.getString("howManyTimes","1"));

        if (howManyTimes == KEEP_GOING)
            timesLeft = INFINITE;
        else
            timesLeft = howManyTimes;

        try {
            MockLocationProvider mockNetwork = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);
            mockNetwork.pushLocation(lat, lng);
            MockLocationProvider mockGps = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
            mockGps.pushLocation(lat, lng);
            Toast.makeText(context, "Mocked location applied.", Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(context, "Mocked location not applied. You must enable mock location in developer settings", Toast.LENGTH_LONG).show();
            return;
        }

        serviceIntent = new Intent(context, ApplyMockBroadcastReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(context, SCHEDULE_REQUEST_CODE, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (timesLeft - 1 > 0) {

            changeButtonToStop();

            try {
                if (Build.VERSION.SDK_INT >= 19) {
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

                Toast.makeText(context, "Mocking location service running.", Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("Lat", String.valueOf(lat));
        editor.putString("Lng", String.valueOf(lng));
        editor.putLong("endTime", System.currentTimeMillis() + howManyTimes * timeInterval * 1000);
        editor.commit();

        timesLeft--;
    }

    protected static void stopMockingLocation()
    {
        changeButtonToApply();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong("endTime", System.currentTimeMillis() - 1);
        editor.commit();

        alarmManager.cancel(pendingIntent);
        Toast.makeText(context, "Mocking stopped", Toast.LENGTH_LONG).show();
    }

    static void changeButtonToApply() {
        button0.setText("Apply");

        button0.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                applyLocation();
            }

        });
    }

    static void changeButtonToStop() {
        button0.setText("Stop");

        button0.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                stopMockingLocation();
            }

        });
    }
}