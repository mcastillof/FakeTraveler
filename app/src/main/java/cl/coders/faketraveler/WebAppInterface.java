package cl.coders.faketraveler;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.EditText;


public class WebAppInterface {
    Context mContext;
    MainActivity mainActivity;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c, MainActivity mA) {
        mainActivity = mA;
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void setPosition(final String str) {

        mainActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                MainActivity.setLat(str.substring(str.indexOf('(') + 1, str.indexOf(',')));
                MainActivity.setLng(str.substring(str.indexOf(',') + 1, str.indexOf(')')));
            }
        });
    }

    @JavascriptInterface
    public double getLat(){

        String lat = MainActivity.getLat();

        if (lat.isEmpty())
            return(0);
        else
            return(Double.parseDouble(lat));

    }

    @JavascriptInterface
    public double getLng() {

        String lng = MainActivity.getLng();

        if (lng.isEmpty())
            return(0);
        else
            return(Double.parseDouble(lng));

    }

}