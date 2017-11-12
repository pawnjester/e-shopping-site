package co.loystar.loystarbusiness.utils.ui.Currency;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;

import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/11/17.
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
    @SuppressWarnings("")
    private static String getJsonFromRaw(Context context, int resource) {
        InputStream inputStream = context.getResources().openRawResource(resource);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return  writer.toString();
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
