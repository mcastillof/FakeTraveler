package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.sharedPrefKey;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * Copied from <a href="https://stackoverflow.com/a/18098090/5894824">StackOverflow</a>.
 */
public final class SharedPrefsUtil {

    private SharedPrefsUtil() {
        throw new UnsupportedOperationException();
    }

    public static Editor putDouble(Editor edit, String key, double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    public static double getDouble(SharedPreferences prefs, String key, double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    static void migrateOldPreferences(Context context) {
        SharedPreferences oldPrefs = context.getSharedPreferences("cl.coders.mockposition.sharedpreferences", Context.MODE_PRIVATE);
        if (!oldPrefs.contains("version")) return; // Either non-existent or already migrated

        Log.i(SharedPrefsUtil.class.toString(), "Migrating old config to new format...");

        // Migrate old values to new preferences
        Editor editor = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE).edit();

        editor.putInt("version", oldPrefs.getInt("version", 0));
        try {
            putDouble(editor, "lat", Double.parseDouble(oldPrefs.getString("lat", "12")));
        } catch (Throwable ignored) {
        }
        try {
            putDouble(editor, "lng", Double.parseDouble(oldPrefs.getString("lng", "15")));
        } catch (Throwable ignored) {
        }
        try {
            editor.putInt("mockCount", Integer.parseInt(oldPrefs.getString("howManyTimes", "0")));
        } catch (Throwable ignored) {
        }
        try {
            editor.putInt("mockFrequency", Integer.parseInt(oldPrefs.getString("timeInterval", "10")));
        } catch (Throwable ignored) {
        }
        editor.putLong("endTime", oldPrefs.getLong("endTime", 0));

        editor.apply();

        // Remove old preferences once and for all
        oldPrefs.edit().clear().apply();

        Log.i(SharedPrefsUtil.class.toString(), "Migration done - deleted old config!");
    }

}
