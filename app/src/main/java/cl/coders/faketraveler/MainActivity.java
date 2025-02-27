package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.SourceChange.CHANGE_FROM_EDITTEXT;
import static cl.coders.faketraveler.MainActivity.SourceChange.CHANGE_FROM_MAP;
import static cl.coders.faketraveler.MainActivity.SourceChange.NONE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    public static final String sharedPrefKey = "cl.coders.mockposition.sharedpreferences";

    private final Handler simHandler = new Handler(Looper.getMainLooper());
    private Runnable simRunnable;

    private MaterialButton buttonApplyStop;
    private WebView webView;
    private EditText editTextLat;
    private EditText editTextLng;
    private Context context;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private Double lat;
    private Double lng;

    private double simLat = 0.0;
    private double simLon = 0.0;
    private boolean firstSim = true;

    private static int timeInterval;
    private static int mockCount;
    private long endTime;
    private int currentVersion;
    private MockLocationProvider mockNetwork;
    private MockLocationProvider mockGps;

    private SourceChange srcChange = NONE;

    @Override
    @SuppressLint("SetJavaScriptEnabled") // XSS unlikely an issue here...
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        webView = findViewById(R.id.webView0);
        WebAppInterface webAppInterface = new WebAppInterface(this);
        sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        buttonApplyStop = findViewById(R.id.button_applyStop);
        MaterialButton buttonSettings = findViewById(R.id.button_settings);
        editTextLat = findViewById(R.id.editTextLat);
        editTextLng = findViewById(R.id.editTextLng);

        buttonApplyStop.setOnClickListener(view -> applyLocation());

        buttonSettings.setOnClickListener(view -> {
            Intent myIntent = new Intent(getBaseContext(), MoreActivity.class);
            startActivity(myIntent);
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(webAppInterface, "Android");
        webView.loadUrl("file:///android_asset/map.html");

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentVersion = (int) (pInfo.getLongVersionCode() >> 32);
            } else {
                currentVersion = pInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(MainActivity.class.toString(), e.toString());
        }

        checkSharedPrefs();

        mockCount = Integer.parseInt(sharedPref.getString("howManyTimes", "1"));
        timeInterval = Integer.parseInt(sharedPref.getString("timeInterval", "10"));

        try {
            setLatLng(sharedPref.getString("lat", ""), sharedPref.getString("lng", ""),
                    CHANGE_FROM_EDITTEXT);
            editTextLat.setText(String.format(Locale.ROOT, "%f", lat));
            editTextLng.setText(String.format(Locale.ROOT, "%f", lng));
        } catch (NumberFormatException e) {
            Log.e(MainActivity.class.toString(), e.toString());
        }

        editTextLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!editTextLat.getText().toString().isEmpty() && !editTextLat.getText().toString().equals("-")) {
                    if (srcChange != CHANGE_FROM_MAP) {
                        lat = Double.parseDouble((editTextLat.getText().toString()));

                        if (lng == null)
                            return;

                        setLatLng(editTextLat.getText().toString(), lng.toString(), CHANGE_FROM_EDITTEXT);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        editTextLng.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!editTextLng.getText().toString().isEmpty() && !editTextLng.getText().toString().equals("-")) {
                    if (srcChange != CHANGE_FROM_MAP) {
                        lng = Double.parseDouble((editTextLng.getText().toString()));

                        if (lat == null)
                            return;

                        setLatLng(lat.toString(), editTextLng.getText().toString(), CHANGE_FROM_EDITTEXT);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        endTime = sharedPref.getLong("endTime", 0);


        //2do check running on start?
        if (endTime > System.currentTimeMillis()) {
            changeButtonToStop();
        } else {
            endTime = 0;
            editor.putLong("endTime", 0);
            editor.apply();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMockingLocation(true);
    }

    /**
     * Check and reinitialize shared preferences in case of problem.
     */
    void checkSharedPrefs() {
        int version = sharedPref.getInt("version", 0);
        String lat = sharedPref.getString("lat", "N/A");
        String lng = sharedPref.getString("lng", "N/A");
        String howManyTimes = sharedPref.getString("howManyTimes", "N/A");
        String timeInterval = sharedPref.getString("timeInterval", "N/A");
        String dMockLon = sharedPref.getString("DMockLon", "0");
        String dMockLat = sharedPref.getString("DMockLat", "0");
        sharedPref.getLong("endTime", 0);

        if (version != currentVersion) {
            editor.putInt("version", currentVersion);
            editor.apply();
        }

        try {
            Double.parseDouble(lat);
            Double.parseDouble(lng);
            Double.parseDouble(howManyTimes);
            Double.parseDouble(timeInterval);
            Double.parseDouble(dMockLon);
            Double.parseDouble(dMockLat);
        } catch (NumberFormatException e) {
            editor.clear();
            editor.putString("lat", lat);
            editor.putString("lng", lng);
            editor.putInt("version", currentVersion);
            editor.putString("howManyTimes", "1");
            editor.putString("timeInterval", "10");
            editor.putString("DMockLon", "0");
            editor.putString("DMockLat", "0");
            editor.putLong("endTime", 0);
            editor.apply();
            Log.e(MainActivity.class.toString(), e.toString());
        }
    }

    /**
     * Apply a mocked location, and start an alarm to keep doing it if howManyTimes is > 1
     * This method is called when "Apply" button is pressed.
     */
    protected void applyLocation() {
        if (latIsEmpty() || lngIsEmpty()) {
            toast(context.getResources().getString(R.string.MainActivity_NoLatLong));
            return;
        }

        try {
            mockNetwork = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);
            mockGps = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
        } catch (SecurityException e) {
            Log.e(MainActivity.class.toString(), e.toString());
            toast(context.getResources().getString(R.string.MainActivity_MockNotApplied));
            stopMockingLocation(false);
            return;
        }

        lat = Double.parseDouble(editTextLat.getText().toString());
        lng = Double.parseDouble(editTextLng.getText().toString());

        toast(context.getResources().getString(R.string.MainActivity_MockApplied));
        endTime = System.currentTimeMillis() + (mockCount - 1L) * timeInterval * 1000L;
        editor.putLong("endTime", endTime);
        editor.apply();

        changeButtonToStop();

        simulate();

        if (shouldStillRun()) {
            toast(context.getResources().getString(R.string.MainActivity_MockLocRunning));
            scheduleNext(timeInterval);
        } else {
            stopMockingLocation(true);
        }
    }

    /**
     * Set a mocked location based on simulation
     * Simulation starts after first position is set.
     * Map becomes updated during simulation
     * text box only after simulation is stopped
     */
    void simulate() {
        boolean movement = shouldSimulateMovement();
        if (firstSim) { // first call in simulation - take current position
            simLat = lat;
            simLon = lng;
            firstSim = false;
        } else if (movement) {
            simLat += Double.parseDouble(sharedPref.getString("DMockLat", "0")) / 1000000;
            simLon += Double.parseDouble(sharedPref.getString("DMockLon", "0")) / 1000000;
        }

        try {
            //MockLocationProvider mockNetwork = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);
            mockNetwork.pushLocation(simLat, simLon);
            //MockLocationProvider mockGps = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
            mockGps.pushLocation(simLat, simLon);

            if (movement) {
                //move map but do not edit text
                setMapMarker(simLat, simLon);
            }
        } catch (Exception e) {
            toast(context.getResources().getString(R.string.MainActivity_MockNotApplied));
            changeButtonToApply();
            Log.e(MainActivity.class.toString(), e.toString());
        }
    }

    /**
     * Check if mocking location should still be running
     *
     * @return true if it should still be running
     */
    boolean shouldStillRun() {
        return mockCount <= 0 || System.currentTimeMillis() <= endTime;
    }

    /**
     * Sets the next alarm accordingly to <seconds>
     *
     * @param seconds number of seconds
     */
    void scheduleNext(int seconds) {
        try {
            setSimTimer(seconds * 1000L);
        } catch (SecurityException e) {
            Log.e(MainActivity.class.toString(), e.toString());
        }
    }

    protected void setSimTimer(long msDelay) {
        simHandler.postDelayed(simRunnable = () -> {
            try {
                simulate();

                if (shouldStillRun())
                    simHandler.postDelayed(simRunnable, msDelay);
                else
                    stopMockingLocation(true);
            } catch (Exception e) {
                Log.e(MainActivity.class.toString(), e.toString());
            }
        }, msDelay);
    }

    protected void stopSimTimer(boolean showToast) {
        if (simRunnable == null) return;
        simHandler.removeCallbacks(simRunnable); //stop handler - remove callback
        toast(context.getResources().getString(R.string.MainActivity_MockStopped));
    }

    /**
     * Shows a toast
     */
    void toast(String str) {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns true editTextLat has no text
     */
    boolean latIsEmpty() {
        return editTextLat.getText().toString().isBlank();
    }

    /**
     * Returns true editTextLng has no text
     */
    boolean lngIsEmpty() {
        return editTextLng.getText().toString().isBlank();
    }

    protected void setMapMarker(double lat, double lng) {
        webView.loadUrl("javascript:setOnMap(" + lat + "," + lng + ");");
    }

    /**
     * Stops mocking the location.
     */
    protected void stopMockingLocation(boolean showToast) {
        changeButtonToApply();
        editor.putLong("endTime", System.currentTimeMillis() - 1);
        editor.apply();

        if (mockNetwork != null)
            mockNetwork.shutdown();
        if (mockGps != null)
            mockGps.shutdown();

        if (!firstSim)
            firstSim = true;

        // simulation moves map but does not update the text box
        // set text box to map position
        setLatLng(simLat + "", simLon + "", CHANGE_FROM_MAP);

        stopSimTimer(showToast);
    }

    /**
     * Changes the button to Apply, and its behavior.
     */
    void changeButtonToApply() {
        buttonApplyStop.setText(context.getResources().getString(R.string.ActivityMain_Apply));
        buttonApplyStop.setOnClickListener(view -> applyLocation());
    }

    /**
     * Changes the button to Stop, and its behavior.
     */
    void changeButtonToStop() {
        buttonApplyStop.setText(context.getResources().getString(R.string.ActivityMain_Stop));
        buttonApplyStop.setOnClickListener(view -> stopMockingLocation(true));
    }

    /**
     * Sets latitude and longitude
     *
     * @param mLat      latitude
     * @param mLng      longitude
     * @param srcChange CHANGE_FROM_EDITTEXT or CHANGE_FROM_MAP, indicates from where comes the change
     */
    void setLatLng(String mLat, String mLng, SourceChange srcChange) {
        lat = Double.parseDouble(mLat);
        lng = Double.parseDouble(mLng);

        if (srcChange == CHANGE_FROM_EDITTEXT) {
            setMapMarker(lat, lng);
        } else if (srcChange == CHANGE_FROM_MAP) {
            this.srcChange = CHANGE_FROM_MAP;
            editTextLat.setText(mLat);
            editTextLng.setText(mLng);
            this.srcChange = NONE;
        }

        editor.putString("lat", mLat);
        editor.putString("lng", mLng);
        editor.apply();
    }

    /**
     * returns latitude
     *
     * @return latitude
     */
    String getLat() {
        return editTextLat.getText().toString();
    }

    /**
     * returns latitude
     *
     * @return latitude
     */
    String getLng() {
        return editTextLng.getText().toString();
    }

    private boolean shouldSimulateMovement() {
        double dMockLat = Double.parseDouble(sharedPref.getString("DMockLat", "0"));
        double dMockLon = Double.parseDouble(sharedPref.getString("DMockLon", "0"));
        return dMockLat != 0 || dMockLon != 0;
    }

    static void setTimeInterval(int timeInterval) {
        MainActivity.timeInterval = timeInterval;
    }

    static void setMockCount(int mockCount) {
        MainActivity.mockCount = mockCount;
    }

    public enum SourceChange {
        NONE, CHANGE_FROM_EDITTEXT, CHANGE_FROM_MAP
    }

}
