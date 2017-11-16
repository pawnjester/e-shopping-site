package co.loystar.loystarbusiness.auth;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.firebase.auth.FirebaseAuth;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.AppIntro;
import co.loystar.loystarbusiness.activities.SplashActivity;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.models.entities.SalesTransaction;

/**
 * Created by ordgen on 11/1/17.
 */

public class SessionManager {
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String EMAIL = "email";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String CLIENT_KEY = "clientKey";
    private static final String CONTACT_NUMBER = "contactNumber";
    private static final String IS_LOGGED_IN = "isLoggedIn";
    private static  final String BUSINESS_NAME = "businessName";
    private static final String MERCHANT_ID = "merchantId";
    private static final String CURRENCY = "currency";
    private static final String BUSINESS_TYPE = "businessType";
    private static final int PRIVATE_MODE = 0;
    private static final String TAG = SessionManager.class.getSimpleName();

    private static SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context mContext;

    @SuppressLint("CommitPrefEdits")
    public SessionManager(Context context) {
        this.mContext = context;
        sharedPreferences = mContext.getSharedPreferences(context.getString(R.string.preference_file_key), PRIVATE_MODE);
        editor = sharedPreferences.edit();
    }

    /**
     * Set Merchant Session Data
     * */
    public void setMerchantSessionData(
            int id,
            String email,
            String firstName,
            String lastName,
            String contactNumber,
            String businessName,
            String businessType,
            String currency,
            String accessToken,
            String clientKey
    ) {
        editor.putInt(MERCHANT_ID, id);
        editor.putString(EMAIL, email);
        editor.putString(FIRST_NAME, firstName);
        editor.putString(LAST_NAME, lastName);
        editor.putString(CONTACT_NUMBER, contactNumber);
        editor.putString(BUSINESS_NAME, businessName);
        editor.putString(CURRENCY, currency);
        editor.putString(ACCESS_TOKEN, accessToken);
        editor.putString(CLIENT_KEY, clientKey);
        editor.putBoolean(IS_LOGGED_IN, true);
        editor.commit();
    }

    public String getAccessToken() {
        return sharedPreferences.getString(ACCESS_TOKEN, "");
    }

    public String getFirstName() {
        return sharedPreferences.getString(FIRST_NAME, "");
    }

    public String getLastName() {
        return sharedPreferences.getString(LAST_NAME, "");
    }

    public String getEmail() {
        return sharedPreferences.getString(EMAIL, "");
    }

    public String getContactNumber() {
        return sharedPreferences.getString(CONTACT_NUMBER, "");
    }

    public String getClientKey() {
        return sharedPreferences.getString(CLIENT_KEY, "");
    }

    public String getFullName() {
        String lastName = getLastName();
        if (!lastName.isEmpty()) {
            lastName = " " + lastName;
        }
        return getFirstName() + lastName;
    }

    public String getBusinessName() {
        return sharedPreferences.getString(BUSINESS_NAME, "");
    }

    public String getCurrency() {
        return sharedPreferences.getString(CURRENCY, "");
    }

    public String getBusinessType() {
        return sharedPreferences.getString(BUSINESS_TYPE, "");
    }

    public int getMerchantId() {
        return sharedPreferences.getInt(MERCHANT_ID, 0);
    }

    /**
     * Clear session details
     * */
    public void signOutMerchant() {
        new ClearSessionData(mContext).execute();
    }

    private class ClearSessionData extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        private Context mContext;


        ClearSessionData(Context mContext) {
            this.mContext = mContext;
            dialog = new ProgressDialog(mContext);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            FirebaseAuth.getInstance().signOut();
            sharedPreferences = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_key), PRIVATE_MODE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            return null;
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Signing out...");
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void v) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Intent intent = new Intent(mContext, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mContext.startActivity(intent);
        }
    }

    /**
     * Quick check for login
     * Get Login State
     * **/
    public boolean isLoggedIn(){
        return sharedPreferences.getBoolean(IS_LOGGED_IN, false);
    }
}
