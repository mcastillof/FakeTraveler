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
                Log.e(WebAppInterface.class.toString(), "Could not set new position from map!", t);
            }
        });
    }

    @JavascriptInterface
    public void setZoom(final String str) {
        mainActivity.runOnUiThread(() -> {
            try {
                mainActivity.setZoom(Double.parseDouble(str));
            } catch (Throwable t) {
                Log.e(WebAppInterface.class.toString(), "Could not save zoom!", t);
            }
        });
    }

}
