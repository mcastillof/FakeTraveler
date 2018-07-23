package cl.coders.faketraveler;

import android.app.Activity;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.EditText;


public class WebAppInterface {
    Context mContext;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void setPosition(String str) {

        EditText editText0 = ((Activity)mContext).getWindow().getDecorView().findViewById(R.id.editText0);
        EditText editText1 = ((Activity)mContext).getWindow().getDecorView().findViewById(R.id.editText1);

        editText0.setText(str.substring(str.indexOf('(') + 1, str.indexOf(',')));
        editText1.setText(str.substring(str.indexOf(',') + 1, str.indexOf(')')));
    }

    @JavascriptInterface
    public double getLat(){

        EditText editText0 = ((Activity)mContext).getWindow().getDecorView().findViewById(R.id.editText0);

        if (editText0.getText().toString().isEmpty())
            return(0);
        else
            return(Double.parseDouble(editText0.getText().toString()));

    }

    @JavascriptInterface
    public double getLng() {

        EditText editText1 = ((Activity) mContext).getWindow().getDecorView().findViewById(R.id.editText1);

        if (editText1.getText().toString().isEmpty())
            return (0);
        else
            return (Double.parseDouble(editText1.getText().toString()));

    }

}