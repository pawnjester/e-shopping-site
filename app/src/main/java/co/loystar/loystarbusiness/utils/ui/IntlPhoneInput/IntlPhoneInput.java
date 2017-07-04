package co.loystar.loystarbusiness.utils.ui.IntlPhoneInput;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 6/17/16.
 */

public class IntlPhoneInput extends RelativeLayout {

    //private AuthConfig.Builder authConfigBuilder;
    // UI Views
    private CountryPhoneSearchableSpinner mCountrySpinner;
    private EditText mPhoneEdit;

    //Util
    private PhoneNumberUtil mPhoneUtil = PhoneNumberUtil.getInstance();
    public PhoneNumberWatcher mPhoneNumberWatcher;

    // Fields
    private CountriesFetcher.CountryList mCountries;
    private IntlPhoneInputListener mIntlPhoneInputListener;
    private boolean launchDigitsAuth = false;

    /**
     * Constructor
     *
     * @param context Context
     */
    public IntlPhoneInput(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor
     *
     * @param context Context
     * @param attrs   AttributeSet
     */
    public IntlPhoneInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Init after constructor
     */
    private void init() {
        inflate(getContext(), R.layout.intl_phone_input, this);

        mCountries = CountriesFetcher.getCountries(getContext());
        mCountrySpinner = (CountryPhoneSearchableSpinner) findViewById(R.id.intl_phone_edit__country);
        mPhoneNumberWatcher = new PhoneNumberWatcher(mCountrySpinner.defaultCountry);
        mPhoneEdit = (EditText) findViewById(R.id.intl_phone_edit__phone);
        mPhoneEdit.addTextChangedListener(mPhoneNumberWatcher);
       /* authConfigBuilder = new AuthConfig.Builder()
                .withAuthCallBack(new AuthCallback() {
                    @Override
                    public void success(DigitsSession session, String phoneNumber) {
                        if (!getNumber().equals(phoneNumber)) {
                            setNumber(phoneNumber);
                        }
                    }
                    @Override
                    public void failure(DigitsException error) {
                        mPhoneEdit.setError(getContext().getString(R.string.error_phone_not_verified));
                        mPhoneEdit.requestFocus();
                    }
                })
                .withPhoneNumber(getNumber());*/
    }

    /**
     * Hide keyboard from phoneEdit field
     */
    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mPhoneEdit.getWindowToken(), 0);
    }

    /**
     * Phone number watcher
     */
    class PhoneNumberWatcher extends PhoneNumberFormattingTextWatcher {
        private boolean lastValidity;

        @SuppressWarnings("unused")
        public PhoneNumberWatcher() {
            super();
        }

        //TODO solve it! support for android kitkat
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public PhoneNumberWatcher(String countryCode) {
            super(countryCode);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            super.onTextChanged(s, start, before, count);
            try {
                String iso = null;
                if (mCountrySpinner.mSelectedCountry != null) {
                    iso = mCountrySpinner.mSelectedCountry.getIso();
                }

                Phonenumber.PhoneNumber phoneNumber = mPhoneUtil.parse(s.toString(), iso);
                iso = mPhoneUtil.getRegionCodeForNumber(phoneNumber);
                if (iso != null) {
                    int countryIdx = mCountries.indexOfIso(iso);
                    mCountrySpinner.setSelection(countryIdx);
                }
            } catch (NumberParseException ignored) {
            }

            if (mIntlPhoneInputListener != null) {
                boolean validity = isValid();

                if (validity != lastValidity) {
                    mIntlPhoneInputListener.done(IntlPhoneInput.this, validity);
                }
                lastValidity = validity;
            }
        }

        /*@Override
        public void afterTextChanged(Editable s) {
            if (isValid()) {
                if (launchDigitsAuth) {
                    Digits.authenticate(authConfigBuilder.build());
                }
            }
        }*/
    }

    /**
     * Set Number
     *
     * @param number E.164 format or national format(for selected country)
     */
    public void setNumber(String number) {
        try {
            String iso = null;
            if (mCountrySpinner.mSelectedCountry != null) {
                iso = mCountrySpinner.mSelectedCountry.getIso();
            }
            Phonenumber.PhoneNumber phoneNumber = mPhoneUtil.parse(number, iso);
            String regionCode = mPhoneUtil.getRegionCodeForNumber(phoneNumber);
            if (regionCode != null) {
                int countryIdx = mCountries.indexOfIso(regionCode);
                mPhoneEdit.setText(mPhoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
                mCountrySpinner.mSelectedCountry = mCountries.get(countryIdx);
                mCountrySpinner.setSelection(countryIdx, false);
            }
        } catch (NumberParseException err) {
            Log.e("PARSE_ERROR", "err: " + err.getMessage());
        }
    }

    /**
     * Get number
     *
     * @return Phone number in E.164 format | null on error
     */
    @SuppressWarnings("unused")
    public String getNumber() {
        Phonenumber.PhoneNumber phoneNumber = getPhoneNumber();

        if (phoneNumber == null) {
            return "";
        }

        return mPhoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    public String getText() {
        return getNumber();
    }

    /**
     * Get PhoneNumber object
     *
     * @return PhoneNumber | null on error
     */
    @SuppressWarnings("unused")
    public Phonenumber.PhoneNumber getPhoneNumber() {
        try {
            String iso = null;
            if (mCountrySpinner.mSelectedCountry != null) {
                iso = mCountrySpinner.mSelectedCountry.getIso();
            }
            return mPhoneUtil.parse(mPhoneEdit.getText().toString(), iso);
        } catch (NumberParseException ignored) {
            return null;
        }
    }

    /**
     * Get selected country
     *
     * @return Country
     */
    @SuppressWarnings("unused")
    public Country getSelectedCountry() {
        return mCountrySpinner.mSelectedCountry;
    }

    /**
     * Check if number is valid
     *
     * @return boolean
     */
    @SuppressWarnings("unused")
    public boolean isValid() {
        Phonenumber.PhoneNumber phoneNumber = getPhoneNumber();
        boolean noDigitsError = true;
        if (mPhoneEdit.getError() != null && mPhoneEdit.getError().equals(getContext().getString(R.string.error_phone_not_verified))) {
            noDigitsError = false;
        }
        return phoneNumber != null && mPhoneUtil.isValidNumber(phoneNumber) && noDigitsError;
    }

    /*set error text*/
    public void setErrorText(String errorText) {
        if (mPhoneEdit != null && !errorText.trim().isEmpty()) {
            mPhoneEdit.setError(errorText);
            mPhoneEdit.requestFocus();
        }
    }

    /**
     * Add validation listener
     *
     * @param listener IntlPhoneInputListener
     */
    public void setOnValidityChange(IntlPhoneInputListener listener) {
        mIntlPhoneInputListener = listener;
    }


    /**
     * Simple validation listener
     */
    public interface IntlPhoneInputListener {
        void done(View view, boolean isValid);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mPhoneEdit.setEnabled(enabled);
        mCountrySpinner.setEnabled(enabled);
    }

    /**
     * Set keyboard done listener to detect when the user click "DONE" on his keyboard
     *
     * @param listener IntlPhoneInputListener
     */
    public void setOnKeyboardDone(final IntlPhoneInputListener listener) {
        mPhoneEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    listener.done(IntlPhoneInput.this, isValid());
                }
                return false;
            }
        });
    }

    /**
     * Launch Digits.authenticate() to verify phone number
     * */

    public void launchDigitsAuth(boolean launch) {
        launchDigitsAuth = launch;
    }
}
