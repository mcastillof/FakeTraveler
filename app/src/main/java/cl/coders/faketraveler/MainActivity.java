package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.SourceChange.CHANGE_FROM_EDITTEXT;
import static cl.coders.faketraveler.MainActivity.SourceChange.CHANGE_FROM_MAP;
import static cl.coders.faketraveler.MainActivity.SourceChange.LOAD;
import static cl.coders.faketraveler.MainActivity.SourceChange.NONE;
import static cl.coders.faketraveler.SharedPrefsUtil.getDouble;
import static cl.coders.faketraveler.SharedPrefsUtil.migrateOldPreferences;
import static cl.coders.faketraveler.SharedPrefsUtil.putDouble;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements ServiceConnection {

    @NonNull
    public static final String sharedPrefKey = "cl.coders.faketraveler.sharedprefs";
    @NonNull
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private MaterialButton buttonApplyStop;
    private WebView webView;
    private EditText editTextLat;
    private EditText editTextLng;
    private View mapContainer;
    private View latLonControls;
    private ScrollView settingsContainer;
    private BottomNavigationView bottomNav;
    private Context context;
    private int currentVersion;

    @Nullable
    private TextView tvKmzFileSize;
    @Nullable
    private TextView tvKmzPlacemarkName;
    /** LineString points parsed from the currently opened KMZ (File mode). */
    @Nullable
    private List<KmzFile.Point> currentKmzLine;

    private final ActivityResultLauncher<String[]> openKmzLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    onKmzFileSelected(new KmzFile(this, uri));
                }
            });

    @NonNull
    private SourceChange srcChange = NONE;

    @Nullable
    private MockedLocationService.MockedBinder binder = null;

    // Config
    private int version;
    private double lat;
    private double lng;
    private double zoom;
    private int mockCount;
    private int mockFrequency;
    private double dLat;
    private double dLng;
    private boolean mockSpeed;
    private long endTime;
    private String mapProvider;

    @Override
    @SuppressLint("SetJavaScriptEnabled") // XSS unlikely an issue here...
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        context = getApplicationContext();
        webView = findViewById(R.id.webView0);
        WebAppInterface webAppInterface = new WebAppInterface(this);

        buttonApplyStop = findViewById(R.id.button_applyStop);
        editTextLat = findViewById(R.id.editTextLat);
        editTextLng = findViewById(R.id.editTextLng);
        mapContainer = findViewById(R.id.map_container);
        latLonControls = findViewById(R.id.latlon_controls);
        settingsContainer = findViewById(R.id.settings_container);
        bottomNav = findViewById(R.id.bottom_nav);

        buttonApplyStop.setOnClickListener(view -> startAndBindMockService());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_search) {
                showSearchTab();
                return true;
            } else if (id == R.id.nav_latlon) {
                showLatLonTab();
                return true;
            } else if (id == R.id.nav_settings) {
                showSettingsTab();
                return true;
            }
            return false;
        });
        // Search is selected by default
        bottomNav.setSelectedItemId(R.id.nav_search);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(webAppInterface, "Android");

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentVersion = (int) (pInfo.getLongVersionCode() >> 32);
            } else {
                currentVersion = pInfo.versionCode;
            }
        } catch (NameNotFoundException e) {
            Log.e(MainActivity.class.toString(), "Could not read version info!", e);
        }

        loadSharedPrefs();
        applyIntentOrDefault(getIntent());

        editTextLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!editTextLat.getText().toString().isEmpty() && !editTextLat.getText().toString().equals("-")) {
                    if (srcChange != CHANGE_FROM_MAP) {
                        try {
                            lat = Double.parseDouble(editTextLat.getText().toString());
                            setLatLng(lat, lng, CHANGE_FROM_EDITTEXT);
                        } catch (Throwable t) {
                            Log.e(MainActivity.class.toString(), "Could not read latitude!", t);
                        }
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

        editTextLng.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!editTextLng.getText().toString().isEmpty() && !editTextLng.getText().toString().equals("-")) {
                    if (srcChange != CHANGE_FROM_MAP) {
                        try {
                            lng = Double.parseDouble(editTextLng.getText().toString());
                            setLatLng(lat, lng, CHANGE_FROM_EDITTEXT);
                        } catch (Throwable t) {
                            Log.e(MainActivity.class.toString(), "Could not read longitude!", t);
                        }
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

        //2do check running on start?
        if (endTime > System.currentTimeMillis()) {
            changeButtonToStop();
        } else {
            endTime = 0;
            saveSettings();
        }

        initSettingsTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        context = getApplicationContext();
        loadSharedPrefs();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        loadSharedPrefs();
        applyIntentOrDefault(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Check and (re-)initialize shared preferences.
     */
    private void loadSharedPrefs() {
        migrateOldPreferences(context);

        SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);

        version = sharedPref.getInt("version", 0);
        lat = getDouble(sharedPref, "lat", 12);
        lng = getDouble(sharedPref, "lng", 15);
        zoom = getDouble(sharedPref, "zoom", 12);
        mockCount = sharedPref.getInt("mockCount", 0);
        mockFrequency = sharedPref.getInt("mockFrequency", 10);
        if (mockFrequency <= 0) mockFrequency = 1;
        dLat = getDouble(sharedPref, "dLat", 0);
        dLng = getDouble(sharedPref, "dLng", 0);
        mockSpeed = sharedPref.getBoolean("mockSpeed", true);
        endTime = sharedPref.getLong("endTime", 0);
        mapProvider = sharedPref.getString("mapProvider", MapProviderUtil.getDefaultMapProvider(Locale.getDefault()));

        if (version != currentVersion) {
            version = currentVersion;
            saveSettings();
        }
    }

    private void saveSettings() {
        Editor editor = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE).edit();

        editor.putInt("version", version);
        putDouble(editor, "lat", lat);
        putDouble(editor, "lng", lng);
        putDouble(editor, "zoom", zoom);
        editor.putInt("mockCount", mockCount);
        editor.putInt("mockFrequency", mockFrequency);
        putDouble(editor, "dLat", dLat);
        putDouble(editor, "dLng", dLng);
        editor.putBoolean("mockSpeed", mockSpeed);
        editor.putLong("endTime", endTime);
        editor.putString("mapProvider", mapProvider);

        editor.apply();
    }

    private void applyIntentOrDefault(Intent intent) {
        String intentData = intent.getDataString();
        if (intentData != null) {
            try {
                GeoUri uri = GeoUri.parse(intentData);
                Log.i(MainActivity.class.toString(), "Received geo intent: " + uri);
                if (uri != null) {
                    lat = uri.lat();
                    lng = uri.lng();
                    Double zoomTmp = uri.zoom();
                    if (zoomTmp != null) zoom = zoomTmp;
                }
            } catch (Throwable t) {
                Log.e(MainActivity.class.toString(), "Could not read geo intent!", t);
            }
        }

        setLatLng(lat, lng, LOAD);

        webView.loadUrl(Uri.parse("file:///android_asset/map.html").buildUpon()
                .appendQueryParameter("lat", "" + lat)
                .appendQueryParameter("lng", "" + lng)
                .appendQueryParameter("zoom", "" + zoom)
                .appendQueryParameter("provider", mapProvider)
                .build()
                .toString());
    }

    /**
     * Apply a mocked location, and start an alarm to keep doing it if mockCount is > 1
     * This method is called when "Apply" button is pressed.
     */
    protected void applyLocation() {
        if (binder == null) return;

        SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        boolean isFileMode = "file".equals(sp.getString("mode", "point"));

        if (isFileMode && currentKmzLine != null && !currentKmzLine.isEmpty()) {
            applyRouteFromKmz();
            return;
        }

        if (latIsEmpty() || lngIsEmpty()) {
            showSnackbar(context.getResources().getString(R.string.MainActivity_NoLatLong));
            return;
        }

        lat = Double.parseDouble(editTextLat.getText().toString());
        lng = Double.parseDouble(editTextLng.getText().toString());

        float[] speed = {0};
        if (mockSpeed) {
            Location.distanceBetween(lat, lng, lat + dLat / 1000000, lng + dLng / 1000000, speed);
            speed[0] /= mockFrequency * 1000L;
        }
        binder.startMock(lng, lat, dLng / 1000000, dLat / 1000000, mockFrequency * 1000L, mockCount, speed[0]);
        endTime = System.currentTimeMillis() + (mockCount - 1L) * mockFrequency * 1000L;
        saveSettings();
    }

    /**
     * File-mode Apply: samples the previously-parsed KMZ LineString at the
     * configured speed and interval, then dispatches those points to the
     * mock-location service.
     */
    private void applyRouteFromKmz() {
        if (binder == null || currentKmzLine == null || currentKmzLine.isEmpty()) return;

        SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        int frequencyS = sp.getInt("mockFrequency", 10);
        if (frequencyS <= 0) frequencyS = 1;
        double speedKmh = getDouble(sp, "movementSpeedKmh", 60);
        int secondsBeforeStart = Math.max(0, sp.getInt("secondsBeforeStart", 5));

        List<KmzFile.Point> samples = KmzFile.sampleAlongLine(currentKmzLine, speedKmh, frequencyS);
        if (samples.isEmpty()) {
            showSnackbar(context.getResources().getString(R.string.MainActivity_NoLatLong));
            return;
        }

        // Hold on the starting point for the requested extra time by
        // prepending copies of the first sample. Number of extra ticks is
        // ceil(secondsBeforeStart / frequencyS) so we always cover at least
        // the requested duration even when it isn't a multiple of the tick.
        int extraTicks = secondsBeforeStart == 0
                ? 0
                : (int) Math.ceil(secondsBeforeStart / (double) frequencyS);
        if (extraTicks > 0) {
            java.util.ArrayList<KmzFile.Point> padded =
                    new java.util.ArrayList<>(samples.size() + extraTicks);
            KmzFile.Point first = samples.get(0);
            for (int i = 0; i < extraTicks; i++) padded.add(first);
            padded.addAll(samples);
            samples = padded;
        }

        float speedMs = mockSpeed ? (float) (speedKmh * 1000d / 3600d) : 0f;
        binder.startRoute(samples, frequencyS * 1000L, speedMs);
        endTime = System.currentTimeMillis() + (samples.size() - 1L) * frequencyS * 1000L;
        saveSettings();
    }

    /**
     * Shows a snackbar
     */
    void showSnackbar(String str) {
        Snackbar.make(findViewById(R.id.main_layout), str, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Shows a snackbar
     */
    void showSnackbar(@StringRes int strRes) {
        Snackbar.make(findViewById(R.id.main_layout), strRes, Snackbar.LENGTH_SHORT).show();
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
        if (webView == null || webView.getUrl() == null) return;
        webView.loadUrl("javascript:setOnMap(" + lat + "," + lng + ");");
    }

    /**
     * Changes the button to Apply, and its behavior.
     */
    void changeButtonToApply() {
        buttonApplyStop.setText(context.getResources().getString(R.string.ActivityMain_Apply));
        buttonApplyStop.setOnClickListener(view -> {
            if (binder == null) {
                startAndBindMockService();
            } else {
                binder.continueMock();
            }
        });
    }

    /**
     * Changes the button to Stop, and its behavior.
     */
    void changeButtonToStop() {
        buttonApplyStop.setText(context.getResources().getString(R.string.ActivityMain_Stop));
        buttonApplyStop.setOnClickListener(view -> {
            try {
                unbindService(this);
            } catch (Throwable ignored) { /* not bound */ }
            stopService(new Intent(this, MockedLocationService.class));
            disconnectService();
        });
    }

    /**
     * Starts the mocking service as a foreground service (so it survives the
     * activity going to background / being destroyed) and binds to it to
     * receive state and location updates.
     */
    private void startAndBindMockService() {
        Intent intent = new Intent(this, MockedLocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, this, BIND_AUTO_CREATE);
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
        saveSettings();
    }

    /**
     * Sets latitude and longitude
     *
     * @param mLat      latitude
     * @param mLng      longitude
     * @param srcChange CHANGE_FROM_EDITTEXT or CHANGE_FROM_MAP, indicates from where comes the change
     */
    void setLatLng(double mLat, double mLng, SourceChange srcChange) {
        lat = mLat;
        lng = mLng;

        if (srcChange == CHANGE_FROM_EDITTEXT || srcChange == LOAD) {
            setMapMarker(lat, lng);
        }
        if (srcChange == CHANGE_FROM_MAP || srcChange == LOAD) {
            this.srcChange = CHANGE_FROM_MAP;
            editTextLat.setText(DECIMAL_FORMAT.format(lat));
            editTextLng.setText(DECIMAL_FORMAT.format(lng));
            this.srcChange = NONE;
        }

        saveSettings();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder = (MockedLocationService.MockedBinder) service;
        binder.mockState.observe(this, this::onMockedStateChange);
        binder.mockedLocation.observe(this, this::onMockedLocationChange);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        disconnectService();
    }

    private void disconnectService() {
        if (binder == null) return;
        binder.mockState.removeObservers(this);
        binder.mockedLocation.removeObservers(this);
        binder = null;
        indicateMockStop();
    }

    private void onMockedStateChange(MockState state) {
        switch (state) {
            case NOT_MOCKED -> indicateMockStop();
            case SERVICE_BOUND -> applyLocation();
            case MOCKED -> {
                changeButtonToStop();
                showSnackbar(R.string.MainActivity_MockApplied);
            }
            case MOCK_ERROR -> showSnackbar(R.string.MainActivity_MockNotApplied);
        }

    }

    private void indicateMockStop() {
        showSnackbar(R.string.MainActivity_MockStopped);
        changeButtonToApply();
    }

    private void onMockedLocationChange(Location location) {
        // LOAD updates both the map marker and the Lat/Lon EditTexts (using
        // srcChange = CHANGE_FROM_MAP internally so the text-watchers don't
        // treat this as a manual edit and loop back into setLatLng).
        setLatLng(location.getLatitude(), location.getLongitude(), LOAD);
    }

    public enum SourceChange {
        NONE, LOAD, CHANGE_FROM_EDITTEXT, CHANGE_FROM_MAP
    }

    // ---------------------- Tab handling ----------------------

    private void showSearchTab() {
        mapContainer.setVisibility(View.VISIBLE);
        latLonControls.setVisibility(View.GONE);
        settingsContainer.setVisibility(View.GONE);
        setMapSearchBarVisible(true);
    }

    private void showLatLonTab() {
        mapContainer.setVisibility(View.VISIBLE);
        latLonControls.setVisibility(View.VISIBLE);
        settingsContainer.setVisibility(View.GONE);
        setMapSearchBarVisible(false);
    }

    private void showSettingsTab() {
        mapContainer.setVisibility(View.GONE);
        latLonControls.setVisibility(View.GONE);
        settingsContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Toggles the map.html in-page search box (since the Search tab uses it and the
     * Lat/Lon tab should not show it).
     */
    private void setMapSearchBarVisible(boolean visible) {
        if (webView == null) return;
        String display = visible ? "flex" : "none";
        webView.evaluateJavascript(
                "(function(){var s=document.getElementById('search');if(s){s.style.display='" + display + "';}})();",
                null);
    }

    // ---------------------- Settings tab ----------------------

    private void initSettingsTab() {
        SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);

        View pointSettings = findViewById(R.id.point_settings);
        View fileSettings = findViewById(R.id.file_settings);
        RadioGroup rgMode = findViewById(R.id.rg_Mode);
        boolean isFileMode = "file".equals(sharedPref.getString("mode", "point"));
        if (isFileMode) {
            rgMode.check(R.id.rb_ModeFile);
            pointSettings.setVisibility(View.GONE);
            fileSettings.setVisibility(View.VISIBLE);
        } else {
            rgMode.check(R.id.rb_ModePoint);
            pointSettings.setVisibility(View.VISIBLE);
            fileSettings.setVisibility(View.GONE);
        }
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            boolean pointChecked = checkedId == R.id.rb_ModePoint;
            pointSettings.setVisibility(pointChecked ? View.VISIBLE : View.GONE);
            fileSettings.setVisibility(pointChecked ? View.GONE : View.VISIBLE);
            SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
            sp.edit().putString("mode", pointChecked ? "point" : "file").apply();
        });

        tvKmzFileSize = findViewById(R.id.tv_KmzFileSize);
        tvKmzPlacemarkName = findViewById(R.id.tv_KmzPlacemarkName);
        MaterialButton buttonOpenKmz = findViewById(R.id.button_openKmz);
        buttonOpenKmz.setOnClickListener(v -> openKmzLauncher.launch(KmzFile.MIME_TYPES));

        EditText etFileMockFrequency = findViewById(R.id.et_FileMockFrequency);
        etFileMockFrequency.setText(String.format(Locale.ROOT, "%d",
                sharedPref.getInt("mockFrequency", 10)));
        etFileMockFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                if (etFileMockFrequency.getText().toString().isBlank()) {
                    editor.putInt("mockFrequency", 10);
                } else {
                    try {
                        editor.putInt("mockFrequency",
                                Integer.parseInt(etFileMockFrequency.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MainActivity.class.toString(), "Could not parse mockFrequency (file)!", t);
                    }
                }
                editor.apply();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        EditText etFileMovementSpeed = findViewById(R.id.et_FileMovementSpeed);
        etFileMovementSpeed.setText(DECIMAL_FORMAT.format(
                getDouble(sharedPref, "movementSpeedKmh", 60)));
        etFileMovementSpeed.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                if (etFileMovementSpeed.getText().toString().isBlank()) {
                    putDouble(editor, "movementSpeedKmh", 60);
                } else {
                    try {
                        putDouble(editor, "movementSpeedKmh",
                                Double.parseDouble(etFileMovementSpeed.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MainActivity.class.toString(), "Could not parse movementSpeedKmh!", t);
                    }
                }
                editor.apply();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        EditText etFileSecondsBeforeStart = findViewById(R.id.et_FileSecondsBeforeStart);
        etFileSecondsBeforeStart.setText(String.format(Locale.ROOT, "%d",
                sharedPref.getInt("secondsBeforeStart", 5)));
        etFileSecondsBeforeStart.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                if (etFileSecondsBeforeStart.getText().toString().isBlank()) {
                    editor.putInt("secondsBeforeStart", 5);
                } else {
                    try {
                        editor.putInt("secondsBeforeStart",
                                Integer.parseInt(etFileSecondsBeforeStart.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MainActivity.class.toString(), "Could not parse secondsBeforeStart!", t);
                    }
                }
                editor.apply();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // Restore previously opened KMZ info, if any
        KmzFile savedKmz = KmzFile.openPersisted(context, sharedPref.getString("kmzUri", null));
        if (savedKmz != null) {
            displayKmzInfo(savedKmz);
        }

        TextView tvLeafletLicense = findViewById(R.id.tv_LeafletLicense);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvLeafletLicense.setText(Html.fromHtml(getString(R.string.Settings_LeafletLicense),
                    Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvLeafletLicense.setText(Html.fromHtml(getString(R.string.Settings_LeafletLicense)));
        }
        tvLeafletLicense.setMovementMethod(LinkMovementMethod.getInstance());

        EditText etDMockLat = findViewById(R.id.et_DMockLat);
        etDMockLat.setText(DECIMAL_FORMAT.format(getDouble(sharedPref, "dLat", 0)));
        etDMockLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                if (etDMockLat.getText().toString().isBlank()) {
                    putDouble(editor, "dLat", 0);
                } else {
                    try {
                        putDouble(editor, "dLat", Double.parseDouble(etDMockLat.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MainActivity.class.toString(), "Could not parse dLat!", t);
                    }
                }
                editor.apply();
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        EditText etDMockLon = findViewById(R.id.et_DMockLon);
        etDMockLon.setText(DECIMAL_FORMAT.format(getDouble(sharedPref, "dLng", 0)));
        etDMockLon.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                if (etDMockLon.getText().toString().isBlank()) {
                    putDouble(editor, "dLng", 0);
                } else {
                    try {
                        putDouble(editor, "dLng", Double.parseDouble(etDMockLon.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MainActivity.class.toString(), "Could not parse dLng!", t);
                    }
                }
                editor.apply();
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        EditText etMockCount = findViewById(R.id.et_MockCount);
        etMockCount.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("mockCount", 0)));
        etMockCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                if (etMockCount.getText().toString().isBlank()) {
                    editor.putInt("mockCount", 0);
                } else {
                    try {
                        editor.putInt("mockCount", Integer.parseInt(etMockCount.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MainActivity.class.toString(), "Could not parse mockCount!", t);
                    }
                }
                editor.apply();
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        EditText etMockFrequency = findViewById(R.id.et_MockFrequency);
        etMockFrequency.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("mockFrequency", 10)));
        etMockFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                if (etMockFrequency.getText().toString().isBlank()) {
                    editor.putInt("mockFrequency", 10);
                } else {
                    try {
                        editor.putInt("mockFrequency", Integer.parseInt(etMockFrequency.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MainActivity.class.toString(), "Could not parse mockFrequency!", t);
                    }
                }
                editor.apply();
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        CheckBox cbMockSpeed = findViewById(R.id.cb_MockSpeed);
        cbMockSpeed.setChecked(sharedPref.getBoolean("mockSpeed", true));
        cbMockSpeed.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
            Editor editor = sp.edit();
            editor.putBoolean("mockSpeed", cbMockSpeed.isChecked());
            editor.apply();
        });

        EditText etMapProvider = findViewById(R.id.et_MapProvider);
        etMapProvider.setText(sharedPref.getString("mapProvider",
                MapProviderUtil.getDefaultMapProvider(Locale.getDefault())));
        etMapProvider.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sp = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                if (etMapProvider.getText().toString().isBlank()) {
                    editor.putString("mapProvider", MapProviderUtil.getDefaultMapProvider(Locale.getDefault()));
                } else {
                    editor.putString("mapProvider", etMapProvider.getText().toString());
                }
                editor.apply();
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void onKmzFileSelected(@NonNull KmzFile kmz) {
        kmz.takePersistablePermission();
        context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE)
                .edit().putString("kmzUri", kmz.getUri().toString()).apply();
        displayKmzInfo(kmz);
    }

    private void displayKmzInfo(@NonNull KmzFile kmz) {
        displayKmzFileSize(kmz);
        loadKmzAsyncAndUpdateUi(kmz);
    }

    private void displayKmzFileSize(@NonNull KmzFile kmz) {
        if (tvKmzFileSize == null) return;
        long size = kmz.getSize();
        if (size >= 0) {
            tvKmzFileSize.setText(getString(R.string.Settings_KmzFileSize, humanReadableSize(size)));
            tvKmzFileSize.setVisibility(View.VISIBLE);
        } else {
            tvKmzFileSize.setVisibility(View.GONE);
        }
    }

    /**
     * Reads the placemark name and the first LineString point off the UI
     * thread; then, on the UI thread, updates the placemark-name label and
     * repositions the map/lat-lon inputs to that first point.
     */
    private void loadKmzAsyncAndUpdateUi(@NonNull KmzFile kmz) {
        final TextView nameView = tvKmzPlacemarkName;
        if (nameView != null) nameView.setVisibility(View.GONE);
        new Thread(() -> {
            String name = kmz.getPlacemarkName();
            List<KmzFile.Point> points = kmz.getLinePoints();
            runOnUiThread(() -> {
                currentKmzLine = points;
                if (nameView != null) {
                    if (name != null && !name.isBlank()) {
                        nameView.setText(getString(R.string.Settings_KmzPlacemarkName, name));
                        nameView.setVisibility(View.VISIBLE);
                    } else {
                        nameView.setVisibility(View.GONE);
                    }
                }
                if (!points.isEmpty()) {
                    KmzFile.Point first = points.get(0);
                    setLatLng(first.lat(), first.lon(), LOAD);
                }
            });
        }, "KmzReader").start();
    }

    @NonNull
    private static String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.ROOT, "%.2f %sB", bytes / Math.pow(1024, exp), unit);
    }

}
