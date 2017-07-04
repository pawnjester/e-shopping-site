package co.loystar.loystarbusiness.models;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 5/11/17.
 */

public class BusinessTypesFetcher {

    public static BusinessTypesList mBusinessTypes;

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
            //Crashlytics.logException(e);
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
