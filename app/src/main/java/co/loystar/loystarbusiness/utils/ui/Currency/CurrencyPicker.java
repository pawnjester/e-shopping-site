package co.loystar.loystarbusiness.utils.ui.Currency;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;

/**
 * Created by ordgen on 11/20/17.
 */

public class CurrencyPicker extends AppCompatSpinner
        implements View.OnTouchListener, CurrencyPickerDialog.OnItemSelectedListener {
    private static final String TAG = CurrencyPicker.class.getSimpleName();
    private CurrenciesFetcher.CurrencyList mCurrencies;
    private CurrencyPickerDialog currencyPickerDialog;
    private Context mContext;
    private Currency mSelectedCurrency;
    private OnCurrencySelectedListener mListener;

    public CurrencyPicker(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public CurrencyPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public CurrencyPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        mCurrencies = CurrenciesFetcher.getCurrencies(getContext());
        CurrencyPickerAdapter currencyPickerAdapter = new CurrencyPickerAdapter(mContext, 0, mCurrencies);
        setAdapter(currencyPickerAdapter);
        currencyPickerDialog = CurrencyPickerDialog.newInstance();
        currencyPickerDialog.setListener(this);
        setOnTouchListener(this);

        setDefault();
    }

    public void setListener(OnCurrencySelectedListener mListener) {
        this.mListener = mListener;
    }

    public void setDefault() {
        SessionManager sessionManager = new SessionManager(mContext);
        String merchantCurrency = sessionManager.getCurrency();
        if (TextUtils.isEmpty(merchantCurrency)) {
            setCurrencySelection();
        } else {
            setCurrencySelection(merchantCurrency);
        }
    }

    public void setCurrencySelection(String iso) {
        if (iso == null || iso.isEmpty()) {
            iso = "USD";
        }
        int defaultIdx = mCurrencies.indexOfIsoCode(iso);
        mSelectedCurrency = mCurrencies.get(defaultIdx);
        setSelection(defaultIdx);

        if (mListener != null) {
            mListener.onCurrencySelected(mSelectedCurrency);
        }
    }

    private void setCurrencySelection() {
        setCurrencySelection(null);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            currencyPickerDialog.show(scanForActivity(mContext).getSupportFragmentManager(), CurrencyPickerDialog.TAG);
        }
        return true;
    }

    @Override
    public void onItemSelected(Currency currency) {
        mSelectedCurrency = currency;
        setSelection(mCurrencies.indexOf(currency));
        if (mListener != null) {
            mListener.onCurrencySelected(currency);
        }
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

    public interface OnCurrencySelectedListener {
        void onCurrencySelected(Currency currency);
    }

    /**
     * View holder for caching
     */
    private static class ViewHolder {
        private TextView mCurrencySymbol;
        private TextView mCurrencyName;
        private TextView mCurrencyIsoCode;
    }

    private class CurrencyPickerAdapter extends ArrayAdapter<Currency> implements SpinnerAdapter {
        private LayoutInflater mLayoutInflater;
        CurrencyPickerAdapter(@NonNull Context context, int resource, ArrayList<Currency> currencies) {
            super(context, resource, currencies);
            mLayoutInflater = LayoutInflater.from(context);
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
            ViewHolder viewHolder;
            Currency currency = getItem(position);

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.currency_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.mCurrencySymbol = convertView.findViewById(R.id.currency_symbol);
                viewHolder.mCurrencyName = convertView.findViewById(R.id.currency_name);
                viewHolder.mCurrencyIsoCode = convertView.findViewById(R.id.currency_iso_code);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            if (currency != null) {
                viewHolder.mCurrencySymbol.setText(currency.getSymbol());
                viewHolder.mCurrencyName.setText(currency.getName());
                viewHolder.mCurrencyIsoCode.setText(currency.getCode());
            }
            return convertView;
        }
    }
}
