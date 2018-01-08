package co.loystar.loystarbusiness.models;

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
import co.loystar.loystarbusiness.models.pojos.OrderPrintOption;
import timber.log.Timber;

/**
 * Created by ordgen on 1/7/18.
 */

public class OrderPrintOptionsFetcher {

    private static OrderPrintOptionList mPrintOptions;
    /**
     * Fetch JSON from RAW resource
     *
     * @param context  Context
     * @param resource Resource int of the RAW file
     * @return JSON
     */
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

    public static OrderPrintOptionList getOrderPrintOptions(Context context) {
        if (mPrintOptions != null) {
            return mPrintOptions;
        }

        mPrintOptions = new OrderPrintOptionList();

        try {
            JSONArray printOptions = new JSONArray(getJsonFromRaw(context, R.raw.order_print_options));
            for (int i=0; i<printOptions.length(); i++) {
                JSONObject printOption = (JSONObject) printOptions.get(i);
                mPrintOptions.add(new OrderPrintOption(printOption.getString("id"), printOption.getString("title")));
            }

        } catch (JSONException e) {
            Timber.e(e);
        }

        return mPrintOptions;
    }

    public static class OrderPrintOptionList extends ArrayList<OrderPrintOption> {
        /**
         * Fetch item index on the list by ID
         *
         * @param id OrderPrintOption's ID
         * @return index of the item in the list
         */
        int indexOfOrderPrintOptionId(String id) {
            for (int i=0; i < this.size(); i++) {
                if (this.get(i).getId().equals(id)) {
                    return i;
                }
            }
            return  -1;
        }

        /**
         * Get OrderPrintOption by ID
         * @param id OrderPrintOption's ID
         * @return OrderPrintOption
         * */
        public OrderPrintOption getOrderPrintOptionById(String id) {
            int index = indexOfOrderPrintOptionId(id);
            if (index > -1) {
                return this.get(index);
            }
            return null;
        }
    }
}
