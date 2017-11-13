package co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;

import java.util.ArrayList;

/**
 * Created by ordgen on 11/13/17.
 */

public class CountryPhoneSpinner extends AppCompatSpinner implements View.OnTouchListener {
    private CountryPhoneSpinnerAdapter mAdapter;
    public String defaultCountry = "gh";
    private CountriesFetcher.CountryList mCountries;
    private CountryPhoneSpinnerDialog countryPhoneSpinnerDialog;
    private Context mContext;

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
        mAdapter = new CountryPhoneSpinnerAdapter(mContext, 0, mCountries);
        setAdapter(mAdapter);
        setOnTouchListener(this);

        countryPhoneSpinnerDialog = CountryPhoneSpinnerDialog.newInstance();
    }

    public void setDefaultCountry(String defaultCountry) {
        this.defaultCountry = defaultCountry;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
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
}
