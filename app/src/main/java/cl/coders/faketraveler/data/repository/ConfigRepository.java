package cl.coders.faketraveler.data.repository;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Locale;
import java.util.Map;

import cl.coders.faketraveler.domain.repository.Config;
import cl.coders.faketraveler.presentation.MapProviderUtil;

public class ConfigRepository implements Config {
    private static final String TAG = ConfigRepository.class.getName();
    /**
     * Separation Application version。
     * when change Config version plus one
     */
    private static final int VERSION = 211;
    private static final String sharedName = "cl.coders.faketraveler.sharedprefs";

    private final SharedPreferences mSharedPref;

    public ConfigRepository(Context context) {
        mSharedPref = context.getSharedPreferences(sharedName, MODE_PRIVATE);
        migrateOldPreferences(context);
        checkItemsType();
        // addMargin( implement );
    }

    private final String KEY_VERSION = "version";
    private final String KEY_MAP_PROVIDER = "mapProvider";
    private final String KEY_LAT = "lat";
    private final String KEY_LNG = "lng";
    private final String KEY_DELTA_LAT = "dLat";
    private final String KEY_DELTA_LNG = "dLng";
    private final String KEY_MOCK_COUNT = "mockCount";
    private final String KEY_MOCK_FREQUENCY = "mockFrequency";
    private final String KEY_ZOOM = "zoom";
    private final String KEY_END_TIME = "endTime";

    @Override
    public Integer getVersion() {
        if (mSharedPref.contains(KEY_VERSION)) {
            return mSharedPref.getInt(KEY_VERSION, 0);
        } else {
            return null;
        }
    }

    @Override
    public String getMapProvider() {
        return mSharedPref.getString(KEY_MAP_PROVIDER, MapProviderUtil.getDefaultMapProvider(Locale.getDefault()));
    }

    @Override
    public double getLongitude(double defaultValue) {
        return getDouble(KEY_LNG, defaultValue);
    }

    @Override
    public double getLatitude(double defaultValue) {
        return getDouble(KEY_LAT, defaultValue);
    }

    @Override
    public double getDeltaLongitude() {
        return getDouble(KEY_DELTA_LNG, 0);
    }

    @Override
    public double getDeltaLatitude() {
        return getDouble(KEY_DELTA_LAT, 0);
    }

    @Override
    public int getMockedCount() {
        return mSharedPref.getInt(KEY_MOCK_COUNT, 0);
    }

    @Override
    public int getMockFrequency() {
        return mSharedPref.getInt(KEY_MOCK_FREQUENCY, 10);
    }

    @Override
    public long getEndTime() {
        return mSharedPref.getLong(KEY_END_TIME, 0);
    }

    @Override
    public double geZoom() {
        return getDouble(KEY_ZOOM, 12);
    }

    @Override
    public ConfigEdit edit() {
        return new ConfigRepositoryEdit(mSharedPref.edit());
    }

    public double getDouble(String key, double defaultValue) {
        return Double.longBitsToDouble(mSharedPref.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    private class ConfigRepositoryEdit implements Config.ConfigEdit {

        private final SharedPreferences.Editor editor;

        private ConfigRepositoryEdit(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        @Override
        public ConfigEdit setVersion(int version) {
            editor.putInt(KEY_VERSION, version);
            return this;
        }

        @Override
        public ConfigEdit setMapProvider(String provider) {
            editor.putString(KEY_MAP_PROVIDER, provider);
            return this;
        }

        @Override
        public ConfigEdit setEndTime(long endTime) {
            editor.putLong(KEY_END_TIME, endTime);
            return this;
        }

        @Override
        public ConfigEdit setMockFrequency(int mockFrequency) {
            editor.putInt(KEY_MOCK_FREQUENCY, mockFrequency);
            return this;
        }

        @Override
        public ConfigEdit setMockedCount(int mockedCount) {
            editor.putInt(KEY_MOCK_COUNT, mockedCount);
            return this;
        }

        @Override
        public ConfigEdit setLatitude(double latitude) {
            putDouble(KEY_LAT, latitude);
            return this;
        }

        @Override
        public ConfigEdit setLongitude(double longitude) {
            putDouble(KEY_LNG, longitude);
            return this;
        }

        @Override
        public ConfigEdit setDeltaLatitude(double latitude) {
            putDouble(KEY_DELTA_LAT, latitude);
            return this;
        }

        @Override
        public ConfigEdit setDeltaLongitude(double longitude) {
            putDouble(KEY_DELTA_LNG, longitude);
            return this;
        }

        @Override
        public ConfigEdit setZoom(double zoom) {
            putDouble(KEY_ZOOM, zoom);
            return this;
        }

        @Override
        public void save() {
            editor.apply();
        }

        public void putDouble(String key, double value) {
            editor.putLong(key, Double.doubleToRawLongBits(value));
        }
    }

    private void addMargin(ConfigMargin margin) {
        if (margin.fromVersion == getVersion()) {
            margin.margin(mSharedPref);
        }
    }

    private abstract static class ConfigMargin {
        public int fromVersion;
        public int toVersion;

        public ConfigMargin(int fromVersion, int toVersion) {
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
        }

        abstract void margin(SharedPreferences preferences);
    }


    private void checkItemsType() {
        Log.i(TAG, "Checking Types...");
        // Migrate old values to new preferences

        SharedPreferences.Editor editor = mSharedPref.edit();
        Map<String, ?> data = mSharedPref.getAll();
        if (!(data.get(KEY_VERSION) instanceof Integer)) {
            editor.putInt(KEY_VERSION, VERSION);
        }
        if (!(data.get(KEY_LAT) instanceof Long)) {
            editor.remove(KEY_LAT);
        }
        if (!(data.get(KEY_LNG) instanceof Long)) {
            editor.remove(KEY_LNG);
        }
        if (!(data.get(KEY_MOCK_COUNT) instanceof Integer)) {
            editor.remove(KEY_MOCK_COUNT);
        }
        if (!(data.get(KEY_MOCK_FREQUENCY) instanceof Integer)) {
            editor.remove(KEY_MOCK_FREQUENCY);
        }
        if (!(data.get(KEY_END_TIME) instanceof Long)) {
            editor.remove(KEY_END_TIME);
        }
        editor.apply();
        Log.i(TAG, "Checking Types done");
    }

    public void migrateOldPreferences(Context context) {
        String oldPrefsName = "cl.coders.mockposition.sharedpreferences";
        SharedPreferences oldPrefs = context.getSharedPreferences(oldPrefsName, Context.MODE_PRIVATE);
        if (!oldPrefs.contains("version")) return; // Either non-existent or already migrated

        Log.i(TAG, "Migrating old config to new format...");

        // Migrate old values to new preferences
        ConfigEdit editor = edit();
        editor.setVersion(oldPrefs.getInt("version", 0));

        try {
            editor.setLatitude(Double.parseDouble(oldPrefs.getString("lat", "12")));
        } catch (Throwable ignored) {
        }
        try {
            editor.setLongitude(Double.parseDouble(oldPrefs.getString("lng", "15")));
        } catch (Throwable ignored) {
        }
        try {
            editor.setMockedCount(Integer.parseInt(oldPrefs.getString("howManyTimes", "0")));
        } catch (Throwable ignored) {
        }
        try {
            editor.setMockFrequency(Integer.parseInt(oldPrefs.getString("timeInterval", "10")));
        } catch (Throwable ignored) {
        }
        editor.setEndTime(oldPrefs.getLong("endTime", 0));
        editor.save();
        // Remove old preferences once and for all
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences(oldPrefsName);
        } else {
            oldPrefs.edit().clear().apply();
        }
        Log.i(TAG, "Migration done - deleted old config!");
    }
}
