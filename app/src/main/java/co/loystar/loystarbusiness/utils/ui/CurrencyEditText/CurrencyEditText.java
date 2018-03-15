package co.loystar.loystarbusiness.utils.ui.CurrencyEditText;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;

import java.util.Currency;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import timber.log.Timber;

/**
 * Created by ordgen on 11/16/17.
 */

public class CurrencyEditText extends AppCompatEditText {
    private Currency currency;
    private String currencyCode;
    private boolean defaultHintEnabled = true;
    private boolean allowNegativeValues = false;
    private long valueInLowestDenominator = 0L;
    private Context context;
    private boolean formatWithTextFormatter = true;

    private CurrencyTextWatcher textWatcher;
    private String hintCache = null;

    public CurrencyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.setInputType(InputType.TYPE_CLASS_NUMBER| InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        SessionManager sessionManager = new SessionManager(context);
        currencyCode = sessionManager.getCurrency();
        processAttributes(context, attrs);
        init();
    }

    private void init(){
        initCurrency();
        initCurrencyTextWatcher();
    }


    private void initCurrencyTextWatcher(){
        if(textWatcher != null){
            this.removeTextChangedListener(textWatcher);
        }
        textWatcher = new CurrencyTextWatcher(this, currencyCode, context, getFormatWithTextFormatter());
        this.addTextChangedListener(textWatcher);
    }

    private void initCurrency(){
        try {
            currency = Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void setFormatWithTextFormatter(boolean formatWithTextFormatter) {
        this.formatWithTextFormatter = formatWithTextFormatter;
    }

    public boolean getFormatWithTextFormatter() {
        return this.formatWithTextFormatter;
    }

    /**
     * Sets whether or or not the Default Hint will be shown in the textBox when no value has yet been entered.
     * @param useDefaultHint - true to enable default hint, false to disable
     */
    public void setDefaultHintEnabled(boolean useDefaultHint) {
        this.defaultHintEnabled = useDefaultHint;
    }

    /**
     * Determine whether or not the default hint is currently enabled for this view.
     * @return true if the default hint is enabled, false if it is not.
     */
    public boolean getDefaultHintEnabled(){
        return this.defaultHintEnabled;
    }

    /**
     * Enable the user to input negative values
     */
    public void setAllowNegativeValues(boolean negativeValuesAllowed){
        allowNegativeValues = negativeValuesAllowed;
    }

    /**
     * Returns whether or not negative values have been allowed for this CurrencyEditText field
     */
    public boolean areNegativeValuesAllowed(){
        return allowNegativeValues;
    }

    /**
     * Retrieve the raw value that was input by the user in their currencies lowest denomination (e.g. pennies).
     *
     * IMPORTANT: Remember that the location of the decimal varies by currency/Locale. This method
     *  returns the raw given value, and does not account for locality of the user. It is up to the
     *  calling application to handle that level of conversion.
     *  For example, if the text of the field is $13.37, this method will return a long with a
     *  value of 1337, as penny is the lowest denomination for USD. It will be up to the calling
     *  application to know that it needs to handle this value as pennies and not some other denomination.
     *
     * @return The raw value that was input by the user, in the lowest denomination of that users
     *  locale.
     */
    public long getRawValue() {
        return valueInLowestDenominator;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
        init();
        updateHint();
    }

    public Currency getCurrency() {
        return currency;
    }


    private void updateHint() {
        if(hintCache != null){
            setHint(hintCache);
        }
        else{
            if(defaultHintEnabled){
                setHint(getDefaultHintValue());
            }
        }
    }



    /**
     * Pass in a value to have it formatted using the same rules used during data entry.
     * @param val A string which represents the value you'd like formatted. It is expected that this string will be in the same format returned by the getRawValue() method (i.e. a series of digits, such as
     *            "1000" to represent "$10.00"). Note that formatCurrency will take in ANY string, and will first strip any non-digit characters before working on that string. If the result of that processing
     *            reveals an empty string, or a string whose number of digits is greater than the max number of digits, an exception will be thrown.
     * @return A formatted string of the passed in value, represented as currency.
     */
    public String formatCurrency(String val){
        return CurrencyTextFormatter.formatText(val, currency, currencyCode, context, true);
    }

    /**
     * Pass in a value to have it formatted using the same rules used during data entry.
     * @param rawVal A long which represents the value you'd like formatted. It is expected that this value will be in the same format returned by the getRawValue() method (i.e. a series of digits, such as
     *            "1000" to represent "$10.00").
     * @return A formatted string of the passed in value, represented as currency.
     */
    public String formatCurrency(long rawVal){
        return CurrencyTextFormatter.formatText(String.valueOf(rawVal), currency, currencyCode, context, true);
    }

    /**
     * Pass in a value to have it formatted using the same rules used during data entry without currency symbol.
     * @param rawVal A long which represents the value you'd like formatted.
     * @return A formatted string of the passed in value, without currency symbol.
     * */
    public String getFormattedValue(long rawVal) {
        if (getFormatWithTextFormatter()) {
            return CurrencyTextFormatter.formatText(String.valueOf(rawVal), currency, currencyCode, context, false).replace(",", "");
        }

        return String.valueOf(rawVal);
    }

    protected void setValueInLowestDenominator(Long mValueInLowestDenom) {
        this.valueInLowestDenominator = mValueInLowestDenom;
    }


    /*
    PRIVATE HELPER METHODS
     */

    private void processAttributes(Context context, AttributeSet attrs){
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CurrencyEditText);

        boolean defaultHintAttrVal = array.getBoolean(R.styleable.CurrencyEditText_enableDefaultHint, true);

        this.setAllowNegativeValues(array.getBoolean(R.styleable.CurrencyEditText_allowNegativeValues, false));

        this.setFormatWithTextFormatter(array.getBoolean(R.styleable.CurrencyEditText_formatWithTextFormatter, true));

        configureHint(defaultHintAttrVal);
        array.recycle();
    }

    private void configureHint(boolean defaultHintAttrVal){

        if(hintAlreadySet()){
            this.setDefaultHintEnabled(false);
            this.hintCache = getHint().toString();
            return;
        }
        else{
            this.setDefaultHintEnabled(defaultHintAttrVal);
        }

        if(getDefaultHintEnabled()) {
            this.setText(getDefaultHintValue());
        }
        else{
            Timber.i("configureHint: Default Hint disabled; ignoring request.");
        }
    }

    private boolean hintAlreadySet(){
        return (this.getHint() != null && !this.getHint().equals(""));
    }

    private String getDefaultHintValue() {
        if (getFormatWithTextFormatter()) {
            return CurrenciesFetcher.getCurrencies(context).getCurrency(currencyCode).getSymbol() + " 0.00";
        }
        return CurrenciesFetcher.getCurrencies(context).getCurrency(currencyCode).getSymbol() + " ";
    }
}
