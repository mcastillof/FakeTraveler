package cl.coders.faketraveler.presentation;

import static cl.coders.faketraveler.presentation.MainActivity.SourceChange.CHANGE_FROM_EDITTEXT;
import static cl.coders.faketraveler.presentation.MainActivity.SourceChange.CHANGE_FROM_MAP;
import static cl.coders.faketraveler.presentation.MainActivity.SourceChange.LOAD;
import static cl.coders.faketraveler.presentation.MainActivity.SourceChange.NONE;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import cl.coders.faketraveler.data.repository.ConfigRepository;
import cl.coders.faketraveler.data.service.MockedLocationService;
import cl.coders.faketraveler.domain.MockedState;
import cl.coders.faketraveler.R;
import cl.coders.faketraveler.domain.repository.Config;


public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = MainActivity.class.getName();
    private Config config;

    @Deprecated
    public static final String sharedPrefKey = "cl.coders.faketraveler.sharedprefs";
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private MaterialButton buttonApplyStop;
    private WebView webView;
    private EditText editTextLat;
    private EditText editTextLng;
    private Context context;

    private SourceChange srcChange = NONE;

    @Override
    @SuppressLint("SetJavaScriptEnabled") // XSS unlikely an issue here...
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = new ConfigRepository(this);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        webView = findViewById(R.id.webView0);
        WebAppInterface webAppInterface = new WebAppInterface(this);

        buttonApplyStop = findViewById(R.id.button_applyStop);
        MaterialButton buttonSettings = findViewById(R.id.button_settings);
        editTextLat = findViewById(R.id.editTextLat);
        editTextLng = findViewById(R.id.editTextLng);

        buttonApplyStop.setOnClickListener(view -> {
            Intent intent = new Intent(this, MockedLocationService.class);
            bindService(intent, this, BIND_AUTO_CREATE);
        });
        buttonSettings.setOnClickListener(view -> {
            Intent myIntent = new Intent(getBaseContext(), MoreActivity.class);
            startActivity(myIntent);
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(webAppInterface, "Android");

        setLatLng(getLatitude(), getLongitude(), LOAD);

        webView.loadUrl(Uri.parse("file:///android_asset/map.html").buildUpon().appendQueryParameter("lat", "" + getLatitude()).appendQueryParameter("lng", "" + getLongitude()).appendQueryParameter("zoom", "" + config.geZoom()).appendQueryParameter("provider", config.getMapProvider()).build().toString());

        editTextLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (srcChange == CHANGE_FROM_MAP) return;
                try {
                    double latitude = Double.parseDouble(s.toString());
                    config.edit().setLatitude(latitude);
                } catch (Throwable t) {
                    Log.e(TAG, "Could not read latitude!", t);

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
                    // TODO ADD Map location View
                    if (srcChange != CHANGE_FROM_MAP) {
                        try {
                            config.edit().setLongitude(Double.parseDouble(editTextLng.getText().toString())).save();
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
    }

    private double getLongitude() {
        return config.getLongitude(15);
    }

    private double getLatitude() {
        return config.getLatitude(12);
    }

    @Override
    protected void onResume() {
        super.onResume();
        context = getApplicationContext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Apply a mocked location, and start an alarm to keep doing it if mockCount is > 1
     * This method is called when "Apply" button is pressed.
     */
    protected void applyLocation() {
        Double longitude = getInputLongitude();
        Double latitude = getInputLatitude();
        if (longitude == null || latitude == null) {
            return;
        }

        config.edit().setLatitude(latitude).setLongitude(longitude).save();
        toast(context.getResources().getString(R.string.MainActivity_MockApplied));
        changeButtonToStop();
        binder.startMocked(longitude, latitude, config.getDeltaLongitude() / 1000000, config.getDeltaLatitude() / 1000000, config.getMockFrequency() * 1000L, config.getMockedCount());
    }

    /**
     * Shows a toast
     */
    void toast(String str) {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a toast
     */
    void toast(@StringRes int strRes) {
        Toast.makeText(context, strRes, Toast.LENGTH_SHORT).show();
    }

    /**
     * @return Edit textview double longitude else return null
     */
    private Double getInputLongitude() {
        try {
            return Double.parseDouble(editTextLng.getText().toString());
        } catch (NumberFormatException e) {
            Log.w(TAG, "parse longitude error", e);
            return null;
        }
    }

    /**
     * @return Edit textview double latitude else return null
     */
    private Double getInputLatitude() {
        try {
            return Double.parseDouble(editTextLat.getText().toString());
        } catch (NumberFormatException e) {
            Log.w(TAG, "parse latitude error", e);
            return null;
        }
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
            Intent intent = new Intent(this, MockedLocationService.class);
            bindService(intent, this, BIND_AUTO_CREATE);
        });
    }

    /**
     * Changes the button to Stop, and its behavior.
     */
    void changeButtonToStop() {
        buttonApplyStop.setText(context.getResources().getString(R.string.ActivityMain_Stop));
        buttonApplyStop.setOnClickListener(view -> unbindService(this));
    }

    public void setZoom(double zoom) {
        config.edit().setZoom(zoom).save();
    }

    /**
     * Sets latitude and longitude
     *
     * @param mLat      latitude
     * @param mLng      longitude
     * @param srcChange CHANGE_FROM_EDITTEXT or CHANGE_FROM_MAP, indicates from where comes the change
     */
    public void setLatLng(double mLat, double mLng, SourceChange srcChange) {
        config.edit().setLatitude(mLat).setLongitude(mLng).save();

        if (srcChange == CHANGE_FROM_EDITTEXT || srcChange == LOAD) {
            setMapMarker(mLat, mLng);
        }
        if (srcChange == CHANGE_FROM_MAP || srcChange == LOAD) {
            this.srcChange = CHANGE_FROM_MAP;
            editTextLat.setText(DECIMAL_FORMAT.format(mLat));
            editTextLng.setText(DECIMAL_FORMAT.format(mLng));
            this.srcChange = NONE;
        }
    }

    private MockedLocationService.MockedBinder binder = null;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder = (MockedLocationService.MockedBinder) service;
        binder.mockedState.observe(this, this::onMockedStateChange);
        binder.mockedLocation.observe(this, this::onMockedLocationChange);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        binder.mockedState.removeObservers(this);
        binder.mockedLocation.removeObservers(this);
        binder = null;
        changeButtonToStop();
    }

    private void onMockedStateChange(MockedState state) {
        switch (state) {
            case NO_MOCKED -> {
                toast(R.string.MainActivity_MockStopped);
                changeButtonToApply();
            }
            case CAN_MOCKED -> applyLocation();
            case MOCKED -> {
                changeButtonToStop();
                toast(R.string.MainActivity_MockApplied);
            }
            case MOCKED_ERROR -> toast(R.string.MainActivity_MockNotApplied);
        }

    }

    private void onMockedLocationChange(Location location) {
        setMapMarker(location.getLatitude(), location.getLongitude());
    }

    public enum SourceChange {
        NONE, LOAD, CHANGE_FROM_EDITTEXT, CHANGE_FROM_MAP
    }

}
