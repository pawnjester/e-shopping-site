package co.loystar.loystarbusiness.utils.ui.IntlPhoneInput;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import co.loystar.loystarbusiness.utils.SessionManager;

/**
 * Created by laudbruce-tagoe on 1/3/17.
 */

public class CountryPhoneSearchableSpinner extends AppCompatSpinner implements View.OnTouchListener,
        CountryPhoneSearchableListDialog.SearchableItem {

    private Context mContext;
    private CountryPhoneSearchableListDialog mCountryPhoneSearchableListDialog;
    public String defaultCountry;
    private CountriesFetcher.CountryList mCountries;
    private CountrySpinnerAdapter mCountrySpinnerAdapter;

    private boolean isDirty;
    public Country mSelectedCountry;

    public CountryPhoneSearchableSpinner(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public CountryPhoneSearchableSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    public CountryPhoneSearchableSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init();
    }

    private void init() {
        mCountries = CountriesFetcher.getCountries(getContext());
        mCountrySpinnerAdapter = new CountrySpinnerAdapter(mContext, 0, mCountries);
        setAdapter(mCountrySpinnerAdapter);
        mCountryPhoneSearchableListDialog = CountryPhoneSearchableListDialog.newInstance
                (mCountries);
        mCountryPhoneSearchableListDialog.setOnSearchableItemClickListener(this);
        setOnTouchListener(this);

        SessionManager sessionManager = new SessionManager(mContext);
        String merchantCurrency = sessionManager.getMerchantCurrency();

        if (TextUtils.isEmpty(merchantCurrency)) {
            defaultCountry = "us";
        }
        else {
            //when merchant is logged in
            defaultCountry = merchantCurrency.substring(0, 2);
        }

        setDefault();
    }

    public void setDefault() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String iso = telephonyManager.getNetworkCountryIso();
            setEmptyDefault(iso);
        } catch (SecurityException e) {
            setEmptyDefault();
        }
    }

    public void setEmptyDefault(String iso) {
        if (iso == null || iso.isEmpty()) {
            iso = defaultCountry;
        }
        int defaultIdx = mCountries.indexOfIso(iso);
        try {
            mSelectedCountry = mCountries.get(defaultIdx);
            setSelection(defaultIdx);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            //set default to the US if device network iso is not in the list
            mSelectedCountry = mCountries.get(6);
            setSelection(6);
        }
    }

    private void setEmptyDefault() {
        setEmptyDefault(null);
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        super.setAdapter(adapter);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (null != mCountrySpinnerAdapter) {
                mCountryPhoneSearchableListDialog.show(scanForActivity(mContext).getSupportFragmentManager(), "TAG");
            }
        }
        return true;
    }

    @Override
    public void onSearchableItemClicked(Object object, int position) {
        Country country = (Country) object;
        mSelectedCountry = mCountries.get(mCountries.indexOf(country));
        mCountryPhoneSearchableListDialog.onQueryTextChange("");
        setSelection(mCountries.indexOf(country));
        if (!isDirty) {
            isDirty = true;
            setAdapter(mCountrySpinnerAdapter);
            setSelection(mCountries.indexOf(country));
        }
    }

    public void setTitle(String strTitle) {
        mCountryPhoneSearchableListDialog.setTitle(strTitle);
    }

    public void setPositiveButton(String strPositiveButtonText) {
        mCountryPhoneSearchableListDialog.setPositiveButton(strPositiveButtonText);
    }

    public void setPositiveButton(String strPositiveButtonText, DialogInterface.OnClickListener onClickListener) {
        mCountryPhoneSearchableListDialog.setPositiveButton(strPositiveButtonText, onClickListener);
    }

    private AppCompatActivity scanForActivity(Context cont) {
        if (cont == null)
            return null;
        else if (cont instanceof AppCompatActivity)
            return (AppCompatActivity) cont;
        else if (cont instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) cont).getBaseContext());

        return null;
    }

    @Override
    public int getSelectedItemPosition() {
        return super.getSelectedItemPosition();
    }

    @Override
    public Object getSelectedItem() {
        return super.getSelectedItem();
    }
}
