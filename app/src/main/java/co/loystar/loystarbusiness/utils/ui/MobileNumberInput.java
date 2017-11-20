package co.loystar.loystarbusiness.utils.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;

/**
 * Created by ordgen on 11/20/17.
 */

public class MobileNumberInput extends RelativeLayout{
    private EditText mPhoneEdit;
    private PhoneNumberUtil mPhoneUtil = PhoneNumberUtil.getInstance();
    public PhoneNumberWatcher mPhoneNumberWatcher;
    private MobileNumberInputInputListener mNumberInputInputListener;
    private DatabaseManager mDatabaseManager;

    public MobileNumberInput(Context context) {
        super(context);
        mDatabaseManager = DatabaseManager.getInstance(context);
        init();
    }

    public MobileNumberInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDatabaseManager = DatabaseManager.getInstance(context);
        init();
    }

    public MobileNumberInput(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDatabaseManager = DatabaseManager.getInstance(context);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.mobile_number_input, this);

        mPhoneNumberWatcher = new PhoneNumberWatcher("GH");
        mPhoneEdit = findViewById(R.id.number_input);
        mPhoneEdit.addTextChangedListener(mPhoneNumberWatcher);
    }

    private class PhoneNumberWatcher extends PhoneNumberFormattingTextWatcher {
        private boolean lastValidity;

        @SuppressWarnings("unused")
        public PhoneNumberWatcher() {
            super();
        }

        //TODO solve it! support for android kitkat
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private PhoneNumberWatcher(String countryCode) {
            super(countryCode);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            super.onTextChanged(s, start, before, count);
            try {
                String iso = "GH";

                Phonenumber.PhoneNumber phoneNumber = mPhoneUtil.parse(s.toString(), iso);
                iso = mPhoneUtil.getRegionCodeForNumber(phoneNumber);
            } catch (NumberParseException ignored) {}

            if (mNumberInputInputListener != null) {
                boolean validity = isValid();
                boolean isUnique = isUnique();
                if (validity) {
                    mPhoneEdit.setError(null);
                }

                if (validity != lastValidity) {
                    mNumberInputInputListener.done(
                            MobileNumberInput.this,
                            validity,
                            isUnique
                    );
                }
                lastValidity = validity;
            }
        }
    }

    /**
     * Set Number
     *
     * @param number E.164 format or national format(for selected country)
     */
    public void setNumber(String number) {
        try {
            String iso = "GH";
            Phonenumber.PhoneNumber phoneNumber = mPhoneUtil.parse(number, iso);
            String regionCode = mPhoneUtil.getRegionCodeForNumber(phoneNumber);
            if (regionCode != null) {
                mPhoneEdit.setText(mPhoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
            }
        } catch (NumberParseException err) {
            Log.e("PARSE_ERROR", "err: " + err.getMessage());
        }
    }

    /**
     * Get PhoneNumber object
     *
     * @return PhoneNumber | null on error
     */
    @SuppressWarnings("unused")
    public Phonenumber.PhoneNumber getPhoneNumber() {
        try {
            String iso = "GH";
            return mPhoneUtil.parse(mPhoneEdit.getText().toString(), iso);
        } catch (NumberParseException ignored) {
            return null;
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
     * Check if number is valid
     *
     * @return boolean
     */
    @SuppressWarnings("unused")
    public boolean isValid() {
        Phonenumber.PhoneNumber phoneNumber = getPhoneNumber();
        return phoneNumber != null && mPhoneUtil.isValidNumber(phoneNumber);
    }

    /**
     * Check if number is unique
     * @return boolean
     */
    public boolean isUnique() {
        MerchantEntity merchantEntity = mDatabaseManager.getMerchantByPhone(getNumber());
        CustomerEntity customerEntity = mDatabaseManager.getCustomerByPhone(getNumber());
        return merchantEntity == null && customerEntity == null;
    }

    /**
     * Simple validation listener
     */
    private interface MobileNumberInputInputListener {
        void done(View view, boolean isValid, boolean isUnique);
    }

    /**
     * Add validation listener
     *
     * @param listener MobileNumberInputInputListener
     */
    public void setOnValidityChange(MobileNumberInputInputListener listener) {
        mNumberInputInputListener = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mPhoneEdit.setEnabled(enabled);
    }

    /*set error text*/
    public void setErrorText(String errorText) {
        if (mPhoneEdit != null && !errorText.trim().isEmpty()) {
            mPhoneEdit.setError(errorText);
            mPhoneEdit.requestFocus();
        }
    }
}
