package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.SourceChange.CHANGE_FROM_MAP;

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

            mainActivity.setLatLng(lat, lng, CHANGE_FROM_MAP);
        });
    }

    /**
     * Get last latitude. This method is called by javascript at page load.
     *
     * @return The last latitude or 0 if it haven't been set.
     */
    @JavascriptInterface
    public double getLat() {
        String lat = mainActivity.getLat();
        return lat.isBlank() ? 0 : Double.parseDouble(lat);
    }

    /**
     * Get last longitude. This method is called by javascript at page load.
     *
     * @return The last longitude or 0 if it haven't been set.
     */
    @JavascriptInterface
    public double getLng() {
        String lng = mainActivity.getLng();
        return lng.isBlank() ? 0 : Double.parseDouble(lng);
    }

}
