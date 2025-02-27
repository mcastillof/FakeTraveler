package cl.coders.faketraveler;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

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

}
