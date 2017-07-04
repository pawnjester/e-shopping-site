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
 * Created by laudbruce-tagoe on 4/15/17.
 */

public class LoyaltyProgramsFetcher {

    private static LoyaltyProgramsList mPrograms;

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


    public static LoyaltyProgramsList getLoyaltyPrograms(Context context) {
        if (mPrograms != null) {
            return mPrograms;
        }

        mPrograms = new LoyaltyProgramsList();

        try {
            JSONArray programs = new JSONArray(getJsonFromRaw(context, R.raw.loyalty_programs));
            for (int i=0; i<programs.length(); i++) {
                JSONObject program = (JSONObject) programs.get(i);
                mPrograms.add(new LoyaltyProgram(program.getString("id"), program.getString("title"), program.getString("description")));
            }

        } catch (JSONException e) {
            //Crashlytics.logException(e);
        }

        return mPrograms;
    }



    public static class LoyaltyProgramsList extends ArrayList<LoyaltyProgram> {
        /**
         * Fetch item index on the list by ID
         *
         * @param id LoyaltyProgram's ID
         * @return index of the item in the list
         */
        int indexOfLoyaltyProgramId(String id) {
            for (int i=0; i < this.size(); i++) {
                if (this.get(i).getId().equals(id)) {
                    return i;
                }
            }

            return  -1;
        }

        /**
         * Get LoyaltyProgram by ID
         * @param id LoyaltyProgram's ID
         * @return LoyaltyProgram
         * */
        public LoyaltyProgram getLoyaltyProgram(String id) {
            int index = indexOfLoyaltyProgramId(id);
            if (index > -1) {
                return this.get(index);
            }
            return null;
        }
    }
}
