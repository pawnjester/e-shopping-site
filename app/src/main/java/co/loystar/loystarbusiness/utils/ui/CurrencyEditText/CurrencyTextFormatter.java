package co.loystar.loystarbusiness.utils.ui.CurrencyEditText;

import android.content.Context;

import java.text.DecimalFormat;
import java.util.Currency;

import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;

/**
 * Created by ordgen on 11/16/17.
 */

public class CurrencyTextFormatter {
    //Setting a max length because after this length, java represents doubles in scientific notation which breaks the formatter
    private static final int MAX_RAW_INPUT_LENGTH = 15;
    private CurrencyTextFormatter(){}

    static String formatText(String val, Currency currency, String currencyCode, Context context, boolean returnValueWithSymbol){
        if(val.equals("-")) return val;

        double CURRENCY_DECIMAL_DIVISOR;
        DecimalFormat currencyFormatter = null;
        try{
            CURRENCY_DECIMAL_DIVISOR = (int) Math.pow(10, currency.getDefaultFractionDigits());
            currencyFormatter = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        }
        catch(IllegalArgumentException e){
            CURRENCY_DECIMAL_DIVISOR = (int) Math.pow(10, Currency.getInstance(currencyCode).getDefaultFractionDigits());
            currencyFormatter = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        }

        //if there's nothing left, that means we were handed an empty string. Also, cap the raw input so the formatter doesn't break.
        if(!val.equals("") && val.length() < MAX_RAW_INPUT_LENGTH && !val.equals("-")) {
            //Convert the string into a double, which will later be passed into the currency formatter
            double newTextValue = Double.valueOf(val);

            /** Despite having a formatter, we actually need to place the decimal ourselves.
             * IMPORTANT: This double division does have a small potential to introduce rounding errors (though the likelihood is very small for two digits)
             * Therefore, do not attempt to pull the numerical value out of the String text of this object. Instead, call getRawValue to retrieve
             * the actual number input by the user. See CurrencyEditText.getRawValue() for more information.
             */

            currencyFormatter.setCurrency(currency);
            String pattern = (currencyFormatter).toPattern();
            /*Format currency without currency symbol*/
            String newPattern = pattern.replace("\u00A4", "").trim();
            DecimalFormat newCurrencyFormatter = new DecimalFormat(newPattern);
            newTextValue = newTextValue / CURRENCY_DECIMAL_DIVISOR;

            if (returnValueWithSymbol) {
                String currencySymbol = CurrenciesFetcher.getCurrencies(context).getCurrency(currencyCode).getSymbol();
                val =  currencySymbol + " " + newCurrencyFormatter.format(newTextValue);
            }
            else {
                val = newCurrencyFormatter.format(newTextValue);
            }
        }
        else {
            throw new IllegalArgumentException("Invalid amount of digits found (either zero or too many) in argument val");
        }
        return val;
    }
}
