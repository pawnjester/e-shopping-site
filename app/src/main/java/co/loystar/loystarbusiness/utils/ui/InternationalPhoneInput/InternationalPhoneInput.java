package co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import timber.log.Timber;

/**
 * Created by ordgen on 11/13/17.
 */

public class InternationalPhoneInput extends RelativeLayout
        implements CountryPhoneSpinnerDialog.OnItemSelectedListener{
    private static final String TAG = InternationalPhoneInput.class.getSimpleName();
    private Spinner mSpinner;
    private EditText mPhoneEdit;
    private PhoneNumberUtil mPhoneUtil = PhoneNumberUtil.getInstance();
    private CountriesFetcher.CountryList mCountries;
    private CountryPhoneSpinnerDialog countryPhoneSpinnerDialog;
    private Country mSelectedCountry;
    private Context mContext;
    private SessionManager mSessionManager;

    private View.OnTouchListener mSpinnerOnTouch = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                countryPhoneSpinnerDialog.show(scanForActivity(mContext).getSupportFragmentManager(), CountryPhoneSpinnerDialog.TAG);
            }
            return true;
        }
    };

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
        mSessionManager = new SessionManager(mContext);
        inflate(mContext, R.layout.international_phone_input, this);
        mPhoneEdit = findViewById(R.id.international_phone_edit_text);
        mSpinner = findViewById(R.id.country_phone_spinner);
        setupSpinner();

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
                    mSpinner.setSelection(countryIdx);
                }
            } catch (NumberParseException ignored) {}
        });
    }

    private void setupSpinner() {
        mCountries = CountriesFetcher.getCountries(getContext());
        CountryPhoneSpinnerAdapter mAdapter = new CountryPhoneSpinnerAdapter(mContext, 0, mCountries);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnTouchListener(mSpinnerOnTouch);

        countryPhoneSpinnerDialog = CountryPhoneSpinnerDialog.newInstance();
        countryPhoneSpinnerDialog.setListener(this);

        setDefault();
    }

    public void setDefault() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                String iso = telephonyManager.getNetworkCountryIso();
                setCountrySelection(iso);
            }
        } catch (SecurityException e) {
            String merchantCurrency = mSessionManager.getCurrency();
            if (TextUtils.isEmpty(merchantCurrency)) {
                setCountrySelection();
            } else {
                try {
                    String iso = merchantCurrency.substring(0, 2);
                    mSelectedCountry = mCountries.get(mCountries.indexOfIso(iso));
                    setCountrySelection(iso);
                } catch (ArrayIndexOutOfBoundsException ignored) {}
            }
        }
    }

    public void setCountrySelection(String iso) {
        if (iso == null || iso.isEmpty()) {
            // set default country iso to US
            iso = "us";
        }
        try {
            int defaultIdx = mCountries.indexOfIso(iso);
            mSelectedCountry = mCountries.get(defaultIdx);
            mSpinner.setSelection(defaultIdx);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            //set default to the US if device network iso is not in the list
            mSelectedCountry = mCountries.get(229);
            mSpinner.setSelection(229);
        }
    }

    private void setCountrySelection() {
        setCountrySelection(null);
    }

    private AppCompatActivity scanForActivity(Context context) {
        if (context == null)
            return null;
        else if (context instanceof AppCompatActivity)
            return (AppCompatActivity) context;
        else if (context instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) context).getBaseContext());

        return null;
    }

    @Override
    public void onCountryItemSelected(Country country) {
        mSelectedCountry = country;
        mSpinner.setSelection(mCountries.indexOf(country));
        mPhoneEdit.setError(null);
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

            if (iso == null) {
                String merchantCurrency = mSessionManager.getCurrency();
                if (!TextUtils.isEmpty(merchantCurrency)) {
                    try {
                        iso = merchantCurrency.substring(0, 2);
                        mSelectedCountry = mCountries.get(mCountries.indexOfIso(iso));
                    } catch (ArrayIndexOutOfBoundsException ignored){}
                }
            }

            Phonenumber.PhoneNumber phoneNumber = mPhoneUtil.parse(number, iso);
            String regionCode = mPhoneUtil.getRegionCodeForNumber(phoneNumber);
            if (regionCode != null) {
                int countryIdx = mCountries.indexOfIso(regionCode);
                mPhoneEdit.setText(mPhoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
                mSelectedCountry = mCountries.get(countryIdx);
                mSpinner.setSelection(countryIdx, false);
            }
        } catch (NumberParseException e) {
            Timber.e("setNumber:NumberParseException %s", e.getMessage());
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
        mSpinner.setEnabled(enabled);
    }

    private class CountryPhoneSpinnerAdapter extends ArrayAdapter<Country> implements SpinnerAdapter {

        private Context context;

        CountryPhoneSpinnerAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Country> countries) {
            super(context, resource, countries);
            this.context = context;
        }

        /**
         * Drop down selected view
         *
         * @param position    position of selected item
         * @param convertView View of selected item
         * @param parent      parent of selected view
         * @return convertView
         */
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            Country country = getItem(position);

            if (convertView == null) {
                convertView = new ImageView(context);
            }

            ((ImageView) convertView).setImageResource(getFlagResource(country));

            return convertView;
        }

        /**
         * Fetch flag resource by Country
         *
         * @param country Country
         * @return int of resource | 0 value if not exists
         */
        private int getFlagResource(Country country) {
            return context.getResources().getIdentifier("country_" + country.getIso().toLowerCase(), "drawable", context.getPackageName());
        }
    }
}
