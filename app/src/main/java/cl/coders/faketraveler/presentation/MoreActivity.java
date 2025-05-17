package cl.coders.faketraveler.presentation;

import static cl.coders.faketraveler.presentation.MainActivity.DECIMAL_FORMAT;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.Objects;

import cl.coders.faketraveler.R;
import cl.coders.faketraveler.data.repository.ConfigRepository;
import cl.coders.faketraveler.domain.TypeParser;

public class MoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);
        ConfigRepository config = new ConfigRepository(this);

        TextView tvLeafletLicense = findViewById(R.id.tv_LeafletLicense);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvLeafletLicense.setText(Html.fromHtml(getString(R.string.ActivityMore_LeafletLicense),
                    Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvLeafletLicense.setText(Html.fromHtml(getString(R.string.ActivityMore_LeafletLicense)));
        }
        tvLeafletLicense.setMovementMethod(LinkMovementMethod.getInstance());

        EditText etDMockLat = findViewById(R.id.et_DMockLat);
        etDMockLat.setText(DECIMAL_FORMAT.format(config.getDeltaLatitude()));
        etDMockLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Double dLatitude = TypeParser.parseDouble(s.toString());
                config.edit().setDeltaLatitude(Objects.requireNonNullElse(dLatitude, 0D)).save();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText etDMockLon = findViewById(R.id.et_DMockLon);
        etDMockLon.setText(DECIMAL_FORMAT.format(config.getDeltaLongitude()));
        etDMockLon.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Double dLongitude = TypeParser.parseDouble(s.toString());
                if (dLongitude == null) {
                    config.edit().setDeltaLongitude(0D);
                } else {
                    config.edit().setDeltaLongitude(dLongitude).save();
                }
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
        etMockCount.setText(String.format(Locale.ROOT, "%d", config.getMockedCount()));
        etMockCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Integer mockCount = TypeParser.parseInteger(s.toString());
                config.edit().setMockedCount(Objects.requireNonNullElse(mockCount, 0)).save();
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
        etMockFrequency.setText(String.format(Locale.ROOT, "%d", config.getMockFrequency()));
        etMockFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Integer mockFrequency = TypeParser.parseInteger(s.toString());
                config.edit().setMockFrequency(Objects.requireNonNullElse(mockFrequency, 10)).save();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText etMapProvider = findViewById(R.id.et_MapProvider);
        etMapProvider.setText(config.getMapProvider());
        etMapProvider.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (etMapProvider.getText().toString().isBlank()) {
                    config.edit().setMapProvider(MapProviderUtil.getDefaultMapProvider(Locale.getDefault())).save();
                } else {
                    config.edit().setMapProvider(s.toString()).save();
                }
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
