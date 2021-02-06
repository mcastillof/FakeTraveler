package cl.coders.faketraveler;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.widget.EditText;
import android.widget.TextView;

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

                editor.commit();
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

                editor.commit();
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
