package co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * Created by laudbruce-tagoe on 1/5/17.
 */

public class CurrencySearchableSpinner extends Spinner implements View.OnTouchListener,
        CurrencySearchableListDialog.SearchableItem {
    private Context mContext;
    private boolean isDirty;
    private CurrencySpinnerAdapter mCurrencySpinnerAdapter;
    private Currency mSelectedCurrency;
    private CurrenciesFetcher.CurrencyList mCurrencies;
    CurrencySearchableListDialog mCurrencySearchableListDialog;


    public CurrencySearchableSpinner(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public CurrencySearchableSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    public CurrencySearchableSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init();
    }

    private void init() {
        mCurrencies = CurrenciesFetcher.getCurrencies(getContext());
        mCurrencySpinnerAdapter = new CurrencySpinnerAdapter(mContext, 0, mCurrencies);
        setAdapter(mCurrencySpinnerAdapter);
        mCurrencySearchableListDialog = CurrencySearchableListDialog.newInstance
                (mCurrencies);
        mCurrencySearchableListDialog.setOnSearchableItemClickListener(this);
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

            if (null != mCurrencySpinnerAdapter) {
                mCurrencySearchableListDialog.show(scanForActivity(mContext).getSupportFragmentManager(), mCurrencySearchableListDialog.getTag());
            }
        }
        return true;
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        super.setAdapter(adapter);
    }

    public void setTitle(String strTitle) {
        mCurrencySearchableListDialog.setTitle(strTitle);
    }

    public void setPositiveButton(String strPositiveButtonText) {
        mCurrencySearchableListDialog.setPositiveButton(strPositiveButtonText);
    }

    public void setPositiveButton(String strPositiveButtonText, DialogInterface.OnClickListener onClickListener) {
        mCurrencySearchableListDialog.setPositiveButton(strPositiveButtonText, onClickListener);
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
    public void onSearchableItemClicked(Object object, int position) {
        Currency currency = (Currency) object;
        mSelectedCurrency = currency;
        mCurrencySearchableListDialog.onQueryTextChange("");
        isDirty = true;
        setAdapter(mCurrencySpinnerAdapter);
        setSelection(mCurrencies.indexOf(currency));
    }

    @Override
    public int getSelectedItemPosition() {
        return super.getSelectedItemPosition();
    }

    @Override
    public Object getSelectedItem() {
        return super.getSelectedItem();
    }

    public String getCurrencyCode() {
        return mSelectedCurrency.getCode();
    }

    public boolean isValid() {
        return isDirty;
    }

    public void setCurrency(String isoCode) {
        //we set the firstTime instance variable to false so the correct convertView is returned;
        mCurrencySpinnerAdapter.firstTime = false;
        int currencyIndex = mCurrencies.indexOfIsoCode(isoCode);
        mSelectedCurrency = mCurrencies.getCurrency(isoCode);
        isDirty = true;
        setAdapter(mCurrencySpinnerAdapter);
        setSelection(currencyIndex);
    }
}
