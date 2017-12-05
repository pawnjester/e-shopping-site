package co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput;

import android.content.Context;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.jakewharton.rxbinding2.widget.RxTextView;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;

/**
 * Created by ordgen on 11/13/17.
 */

public class InternationalPhoneInput extends RelativeLayout implements CountryPhoneSpinner.OnCountrySelectedListener {
    private static final String TAG = InternationalPhoneInput.class.getSimpleName();
    private CountryPhoneSpinner mCountrySpinner;
    private EditText mPhoneEdit;
    private PhoneNumberUtil mPhoneUtil = PhoneNumberUtil.getInstance();
    private InternationalPhoneInputListener internationalPhoneInputListener;
    private CountriesFetcher.CountryList mCountries;
    private Country mSelectedCountry;
    private Context mContext;

    public InternationalPhoneInput(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public InternationalPhoneInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public InternationalPhoneInput(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        inflate(mContext, R.layout.international_phone_input, this);
        mCountries = CountriesFetcher.getCountries(getContext());
        mCountrySpinner = findViewById(R.id.country_phone_spinner);
        mCountrySpinner.setListener(this);
        mPhoneEdit = findViewById(R.id.international_phone_edit_text);

        try {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                String iso = telephonyManager.getNetworkCountryIso();
                setSelectedCountry(iso);
            }
        } catch (SecurityException e) {
            SessionManager sessionManager = new SessionManager(mContext);
            String merchantCurrency = sessionManager.getCurrency();
            if (merchantCurrency == null) {
                // set default country to US
                setSelectedCountry("us");
            } else {
                String iso = merchantCurrency.substring(0, 2);
                mSelectedCountry = mCountries.get(mCountries.indexOfIso(iso));
                setSelectedCountry(iso);
            }
        }

        RxTextView.textChangeEvents(mPhoneEdit).subscribe(textViewTextChangeEvent -> {
            CharSequence s = textViewTextChangeEvent.text();
            try {
                String iso = null;
                if (mSelectedCountry != null) {
                    iso = mSelectedCountry.getIso();
                }

                Phonenumber.PhoneNumber phoneNumber = mPhoneUtil.parse(s.toString(), iso);
                iso = mPhoneUtil.getRegionCodeForNumber(phoneNumber);
                if (iso != null) {
                    int countryIdx = mCountries.indexOfIso(iso);
                    mCountrySpinner.setSelection(countryIdx);
                }
            } catch (NumberParseException ignored) {
            }

            if (internationalPhoneInputListener != null) {
                internationalPhoneInputListener.done(
                        InternationalPhoneInput.this,
                        isValid(),
                        isUnique());
            }
        });
    }

    /**
    * Set selected country
    * @param iso iso of selected country
    */
    private void setSelectedCountry(@NonNull String iso) {
        int countryIdx = mCountries.indexOfIso(iso);
        mSelectedCountry = mCountries.get(countryIdx);
    }

    @Override
    public void onCountrySelected(Country country) {
        mSelectedCountry = country;
    }

    /**
     * Set Number
     *
     * @param number E.164 format or national format(for selected country)
     */
    public void setNumber(String number) {
        try {
            String iso = null;
            if (mSelectedCountry != null) {
                iso = mSelectedCountry.getIso();
            }
            Phonenumber.PhoneNumber phoneNumber = mPhoneUtil.parse(number, iso);
            String regionCode = mPhoneUtil.getRegionCodeForNumber(phoneNumber);
            if (regionCode != null) {
                int countryIdx = mCountries.indexOfIso(regionCode);
                mPhoneEdit.setText(mPhoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
                mSelectedCountry = mCountries.get(countryIdx);
                mCountrySpinner.setSelection(countryIdx, false);
            }
        } catch (NumberParseException e) {
            Log.e(TAG, "setNumber:NumberParseException " +  e.getMessage());
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
            if (mSelectedCountry != null) {
                iso = mSelectedCountry.getIso();
            }
            return mPhoneUtil.parse(mPhoneEdit.getText().toString(), iso);
        } catch (NumberParseException e) {
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
        return mSelectedCountry;
    }

    /**
     * Check if number is valid
     *
     * @return boolean
     */
    @SuppressWarnings("unused")
    public boolean isValid() {
        Phonenumber.PhoneNumber phoneNumber = getPhoneNumber();
        return phoneNumber != null && mPhoneUtil.isValidNumber(phoneNumber);
    }

    /*set error text*/
    public void setErrorText(String errorText) {
        if (mPhoneEdit != null && !errorText.trim().isEmpty()) {
            mPhoneEdit.setError(errorText);
            mPhoneEdit.requestFocus();
        }
    }

    public void setInternationalPhoneInputListener(InternationalPhoneInputListener listener) {
        this.internationalPhoneInputListener = listener;
    }

    /**
     * Simple validation listener
     */
    private interface InternationalPhoneInputListener {
        void done(View view, boolean isValid,  boolean isUnique);
    }

    /**
     * Check if number is unique
     * @return boolean
     */
    public boolean isUnique() {
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(mContext);
        MerchantEntity merchantEntity = mDatabaseManager.getMerchantByPhone(getNumber());
        CustomerEntity customerEntity = mDatabaseManager.getCustomerByPhone(getNumber());
        return merchantEntity == null && customerEntity == null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mPhoneEdit.setEnabled(enabled);
        mCountrySpinner.setEnabled(enabled);
    }
}
