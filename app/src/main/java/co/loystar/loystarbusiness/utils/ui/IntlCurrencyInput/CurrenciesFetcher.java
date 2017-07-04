package co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 1/5/17.
 */

public class CurrenciesFetcher {
    private static CurrencyList mCurrencies;

    /**
     * Fetch JSON from RAW resource
     *
     * @param context  Context
     * @param resource Resource int of the RAW file
     * @return JSON
     */
    private static String getJsonFromRaw(Context context, int resource) {
        String json;
        try {
            InputStream inputStream = context.getResources().openRawResource(resource);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    /**
     * Import CurrencyList from RAW resource
     *
     * @param context Context
     * @return CurrencyList
     */

    public static CurrencyList getCurrencies(Context context) {
        if (mCurrencies != null) {
            return mCurrencies;
        }
        mCurrencies = new CurrencyList();
        try {
            JSONArray currencies = new JSONArray(getJsonFromRaw(context, R.raw.currencies));
            for (int i = 0; i < currencies.length(); i++) {
                JSONObject currency = (JSONObject) currencies.get(i);
                mCurrencies.add(new Currency(currency.getString("name"), currency.getString("symbol"), currency.getString("code")));
            }
        } catch (JSONException e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
        return mCurrencies;
    }

    public static class CurrencyList extends ArrayList<Currency> {
        /**
         * Fetch item index on the list by ISO code
         *
         * @param code Currency's ISO code
         * @return index of the item in the list
         */
        int indexOfIsoCode(String code) {
            for (int i = 0; i < this.size(); i++) {
                if (this.get(i).getCode().toUpperCase().equals(code.toUpperCase())) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Get currency by ISO code
         * @param code Currency's ISO code
         * @return Currency
         * */
        public Currency getCurrency(String code) {
            if (code != null && code.isEmpty()) {
                int index = indexOfIsoCode("USD");
                return this.get(index);
            }
            int index = indexOfIsoCode(code);
            return this.get(index);
        }
    }
}
