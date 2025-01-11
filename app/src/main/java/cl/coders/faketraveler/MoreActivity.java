package cl.coders.faketraveler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.ConfigurationCompat;

import java.util.Locale;

public class MoreActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);
        EditText editText2;
        EditText editText3;
        TextView textView3;
        Context context;
        SharedPreferences sharedPref;
        context = getApplicationContext();
        sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);

        EditText et_DMockLat;
        et_DMockLat  = findViewById(R.id.et_DMockLat);
        et_DMockLat.setText(sharedPref.getString("DMockLat", "0"));
        et_DMockLat.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                EditText et_DMockLat = findViewById(R.id.et_DMockLat);
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (et_DMockLat.getText().toString().isEmpty()) {
                    editor.putString("DMockLat", "0");
                } else {
                    editor.putString("DMockLat", et_DMockLat.getText().toString());
                }

                editor.apply();
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        EditText  et_DMockLon;
        et_DMockLon  = findViewById(R.id.et_DMockLon);
        et_DMockLon.setText(sharedPref.getString("DMockLon", "0"));
        et_DMockLon.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                EditText et_DMockLon = findViewById(R.id.et_DMockLon);
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (et_DMockLon.getText().toString().isEmpty()) {
                    editor.putString("DMockLon", "0");
                } else {
                    editor.putString("DMockLon", et_DMockLon.getText().toString());
                }
                editor.apply();
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }

        });



        textView3 = findViewById(R.id.textView3);
        textView3.setMovementMethod(LinkMovementMethod.getInstance());

        editText2 = findViewById(R.id.editText2);
        editText3 = findViewById(R.id.editText3);
        editText2.setText(sharedPref.getString("howManyTimes", "1"));
        editText3.setText(sharedPref.getString("timeInterval", "10"));

        editText2.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                EditText editText2 = findViewById(R.id.editText2);
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (editText2.getText().toString().isEmpty()) {
                    editor.putString("howManyTimes", "1");
                    MainActivity.howManyTimes = 1;
                } else {
                    editor.putString("howManyTimes", editText2.getText().toString());
                    MainActivity.howManyTimes = Integer.parseInt(editText2.getText().toString());
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        editText3.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {

                EditText editText3 = findViewById(R.id.editText3);
                Context context = getApplicationContext();
                SharedPreferences mSharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = mSharedPref.edit();

                if (editText3.getText().toString().isEmpty()) {
                    editor.putString("timeInterval", "10");
                    MainActivity.timeInterval = 10;
                } else {
                    editor.putString("timeInterval", editText3.getText().toString());
                    MainActivity.timeInterval = Integer.parseInt(editText3.getText().toString());
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
    }
}
