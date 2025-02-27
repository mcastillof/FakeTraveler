package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.SourceChange.CHANGE_FROM_MAP;

import android.util.Log;
import android.webkit.JavascriptInterface;


public class WebAppInterface {

    private final MainActivity mainActivity;

    public WebAppInterface(MainActivity mA) {
        mainActivity = mA;
    }

    /**
     * Set position in GUI. This method is called by javascript when there is a long press in the map.
     *
     * @param str String containing lat and lng
     */
    @JavascriptInterface
    public void setPosition(final String str) {
        mainActivity.runOnUiThread(() -> {
            String lat = str.substring(str.indexOf('(') + 1, str.indexOf(','));
            String lng = str.substring(str.indexOf(',') + 2, str.indexOf(')'));

            try {
                mainActivity.setLatLng(Double.parseDouble(lat), Double.parseDouble(lng), CHANGE_FROM_MAP);
            } catch (Throwable t) {
                Log.e(WebAppInterface.class.toString(), t.toString());
            }
        });
    }

    /**
     * Get last latitude. This method is called by javascript at page load.
     *
     * @return The last latitude or 15 if it haven't been set.
     */
    @JavascriptInterface
    public double getLat() {
        return mainActivity.getLat();
    }

    /**
     * Get last longitude. This method is called by javascript at page load.
     *
     * @return The last longitude or 12 if it haven't been set.
     */
    @JavascriptInterface
    public double getLng() {
        return mainActivity.getLng();
    }

}
