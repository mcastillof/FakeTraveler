package cl.coders.faketraveler.presentation;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * simplify TextWatcher,only implement {@link TextWatcher#afterTextChanged(Editable)}
 */
public abstract class AfterTextChangeListener implements TextWatcher {
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }
}
