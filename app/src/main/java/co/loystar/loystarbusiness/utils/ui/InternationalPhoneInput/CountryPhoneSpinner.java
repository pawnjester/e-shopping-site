package co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;

import java.util.ArrayList;

import co.loystar.loystarbusiness.auth.SessionManager;

/**
 * Created by ordgen on 11/13/17.
 */

public class CountryPhoneSpinner extends AppCompatSpinner implements
        View.OnTouchListener, CountryPhoneSpinnerDialog.OnItemSelectedListener {
    private static final String TAG = CountryPhoneSpinner.class.getSimpleName();
    public String defaultCountry = "us";
    private CountriesFetcher.CountryList mCountries;
    private CountryPhoneSpinnerDialog countryPhoneSpinnerDialog;
    private Context mContext;
    private Country mSelectedCountry;
    private OnCountrySelectedListener mListener;

    public CountryPhoneSpinner(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public CountryPhoneSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public CountryPhoneSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        mCountries = CountriesFetcher.getCountries(getContext());
        CountryPhoneSpinnerAdapter mAdapter = new CountryPhoneSpinnerAdapter(mContext, 0, mCountries);
        setAdapter(mAdapter);
        setOnTouchListener(this);

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
            Log.e(TAG, "setDefault: " + e.getMessage() );
            SessionManager sessionManager = new SessionManager(mContext);
            String merchantCurrency = sessionManager.getCurrency();
            if (merchantCurrency == null) {
                setCountrySelection();
            } else {
                String iso = merchantCurrency.substring(0, 2);
                mSelectedCountry = mCountries.get(mCountries.indexOfIso(iso));
                setCountrySelection(iso);
            }
        }
    }

    public void setCountrySelection(String iso) {
        if (iso == null || iso.isEmpty()) {
            iso = defaultCountry;
        }
        int defaultIdx = mCountries.indexOfIso(iso);
        try {
            mSelectedCountry = mCountries.get(defaultIdx);
            setSelection(defaultIdx);

            if (mListener != null) {
                mListener.onCountrySelected(mSelectedCountry);
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            //set default to the US if device network iso is not in the list
            mSelectedCountry = mCountries.get(6);
            setSelection(6);

            if (mListener != null) {
                mListener.onCountrySelected(mSelectedCountry);
            }
        }
    }

    private void setCountrySelection() {
        setCountrySelection(null);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            countryPhoneSpinnerDialog.show(scanForActivity(mContext).getSupportFragmentManager(), CountryPhoneSpinnerDialog.TAG);
        }
        return true;
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
    public void onItemSelected(Country country) {
        mSelectedCountry = country;
        setCountrySelection(mSelectedCountry.getIso());
    }

    private class CountryPhoneSpinnerAdapter extends ArrayAdapter<Country> implements SpinnerAdapter {

        public CountryPhoneSpinnerAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Country> countries) {
            super(context, resource, countries);
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
                convertView = new ImageView(getContext());
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
            return getContext().getResources().getIdentifier("country_" + country.getIso().toLowerCase(), "drawable", getContext().getPackageName());
        }
    }

    public void setListener(OnCountrySelectedListener mListener) {
        this.mListener = mListener;
    }

    public interface OnCountrySelectedListener {
        void onCountrySelected(Country country);
    }
}
