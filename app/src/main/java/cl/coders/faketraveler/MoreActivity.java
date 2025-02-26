package cl.coders.faketraveler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);

        TextView textView3 = findViewById(R.id.textView3);
        textView3.setMovementMethod(LinkMovementMethod.getInstance());

        EditText etDMockLat = findViewById(R.id.et_DMockLat);
        etDMockLat.setText(sharedPref.getString("DMockLat", "0"));
        etDMockLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etDMockLat.getText().toString().isBlank()) {
                    editor.putString("DMockLat", "0");
                } else {
                    editor.putString("DMockLat", etDMockLat.getText().toString());
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

        EditText etDMockLon = findViewById(R.id.et_DMockLon);
        etDMockLon.setText(sharedPref.getString("DMockLon", "0"));
        etDMockLon.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etDMockLon.getText().toString().isBlank()) {
                    editor.putString("DMockLon", "0");
                } else {
                    editor.putString("DMockLon", etDMockLon.getText().toString());
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

        EditText etMockCount = findViewById(R.id.et_MockCount);
        etMockCount.setText(sharedPref.getString("howManyTimes", "1"));
        etMockCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etMockCount.getText().toString().isBlank()) {
                    editor.putString("howManyTimes", "1");
                    MainActivity.setMockCount(1);
                } else {
                    editor.putString("howManyTimes", etMockCount.getText().toString());
                    MainActivity.setMockCount(Integer.parseInt(etMockCount.getText().toString()));
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

        EditText etMockFrequency = findViewById(R.id.et_MockFrequency);
        etMockFrequency.setText(sharedPref.getString("timeInterval", "10"));
        etMockFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences mSharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = mSharedPref.edit();

                if (etMockFrequency.getText().toString().isBlank()) {
                    editor.putString("timeInterval", "10");
                    MainActivity.setTimeInterval(10);
                } else {
                    editor.putString("timeInterval", etMockFrequency.getText().toString());
                    MainActivity.setTimeInterval(Integer.parseInt(etMockFrequency.getText().toString()));
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
