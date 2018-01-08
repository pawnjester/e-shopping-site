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
import co.loystar.loystarbusiness.models.pojos.BusinessType;

/**
 * Created by ordgen on 11/20/17.
 */

public class BusinessTypesFetcher {
    private static BusinessTypesList mBusinessTypes;
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

    public static BusinessTypesList getBusinessTypes(Context context) {
        if (mBusinessTypes != null) {
            return mBusinessTypes;
        }

        mBusinessTypes = new BusinessTypesList();

        try {
            JSONArray businessTypes = new JSONArray(getJsonFromRaw(context, R.raw.business_types));
            for (int i=0; i<businessTypes.length(); i++) {
                JSONObject businessType = (JSONObject) businessTypes.get(i);
                mBusinessTypes.add(new BusinessType(businessType.getInt("id"), businessType.getString("tag"), businessType.getString("title")));
            }

        } catch (JSONException e) {
        }

        return mBusinessTypes;
    }

    public static class BusinessTypesList extends ArrayList<BusinessType> {
        /**
         * Fetch item index on the list by ID
         *
         * @param id BusinessType's ID
         * @return index of the item in the list
         */
        int indexOfBusinessTypeId(int id) {
            for (int i=0; i < this.size(); i++) {
                if (this.get(i).getId() == id) {
                    return i;
                }
            }
            return  -1;
        }

        /**
         * Fetch item index on the list by Title
         *
         * @param title BusinessType's Title
         * @return index of the item in the list
         */
        int indexOfBusinessTypeTitle(String title) {
            for (int i=0; i < this.size(); i++) {
                if (this.get(i).getTitle().equals(title)) {
                    return i;
                }
            }
            return  -1;
        }

        /**
         * Get BusinessType by ID
         * @param id BusinessType's ID
         * @return BusinessType
         * */
        public BusinessType getBusinessTypeById(int id) {
            int index = indexOfBusinessTypeId(id);
            if (index > -1) {
                return this.get(index);
            }
            return null;
        }

        /**
         * Get BusinessType by Title
         * @param title BusinessType's Title
         * @return BusinessType
         * */
        public BusinessType getBusinessTypeByTitle(String title) {
            int index = indexOfBusinessTypeTitle(title);
            if (index > -1) {
                return this.get(index);
            }
            return null;
        }
    }
}
