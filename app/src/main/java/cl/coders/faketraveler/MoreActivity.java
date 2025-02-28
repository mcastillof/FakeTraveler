package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.DECIMAL_FORMAT;
import static cl.coders.faketraveler.MainActivity.sharedPrefKey;
import static cl.coders.faketraveler.SharedPrefsUtil.getDouble;
import static cl.coders.faketraveler.SharedPrefsUtil.putDouble;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);

        EditText etDMockLat = findViewById(R.id.et_DMockLat);
        etDMockLat.setText(DECIMAL_FORMAT.format(getDouble(sharedPref, "dLat", 0)));
        etDMockLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etDMockLat.getText().toString().isBlank()) {
                    putDouble(editor, "dLat", 0);
                } else {
                    try {
                        putDouble(editor, "dLat", Double.parseDouble(etDMockLat.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse dLat!", t);
                    }
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
        etDMockLon.setText(DECIMAL_FORMAT.format(getDouble(sharedPref, "dLng", 0)));
        etDMockLon.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etDMockLon.getText().toString().isBlank()) {
                    putDouble(editor, "dLng", 0);
                } else {
                    try {
                        putDouble(editor, "dLng", Double.parseDouble(etDMockLon.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse dLng!", t);
                    }
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
        etMockCount.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("mockCount", 0)));
        etMockCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etMockCount.getText().toString().isBlank()) {
                    editor.putInt("mockCount", 0);
                } else {
                    try {
                        editor.putInt("mockCount", Integer.parseInt(etMockCount.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse mockCount!", t);
                    }
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
        etMockFrequency.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("mockFrequency", 10)));
        etMockFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etMockFrequency.getText().toString().isBlank()) {
                    editor.putInt("mockFrequency", 10);
                } else {
                    try {
                        editor.putInt("mockFrequency", Integer.parseInt(etMockFrequency.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse mockFrequency!", t);
                    }
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
