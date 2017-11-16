package co.loystar.loystarbusiness.utils;

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
import co.loystar.loystarbusiness.models.pojos.LoyaltyProgram;

/**
 * Created by ordgen on 11/16/17.
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
