package co.loystar.loystarbusiness.utils;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.activities.MerchantLoginActivity;
import co.loystar.loystarbusiness.activities.SplashActivity;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import io.smooch.core.Smooch;

/**
 * Created by ordgen on 7/4/17.
 */

public class SessionManager {
    private static SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context _context;
    private static int PRIVATE_MODE = 0;
    // SharedPref file name
    private static final String PREF_NAME = "MerchantPref";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";
    private static final String KEY_BUSINESS_NAME = "businessName";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ID = "merchantId";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_BUSINESS_TYPE = "businessType";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_CLIENT_KEY = "clientKey";
    private static final String KEY_TOKEN_EXPIRY = "tokenExpiry";
    // Constructor
    @SuppressLint("CommitPrefEdits")
    public SessionManager(Context context) {
        this._context = context;
        int PRIVATE_MODE = 0;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Set Merchant Session Data
     * */
    public void setMerchantSessionData(
            String businessName,
            String email,
            String merchantId,
            String currency,
            String firstName,
            String lastName,
            String businessType,
            String phone,
            String accessToken,
            String clientKey,
            String tokenExpiry
    ) {

        editor.putBoolean(IS_LOGIN, true);
        editor.putString(KEY_BUSINESS_NAME, businessName);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ID, merchantId);
        editor.putString(KEY_CURRENCY, currency);
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.putString(KEY_LAST_NAME, lastName);
        editor.putString(KEY_BUSINESS_TYPE, businessType);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_CLIENT_KEY, clientKey);
        editor.putString(KEY_TOKEN_EXPIRY, tokenExpiry);
        editor.commit();
    }

    public String getMerchantEmail() {
        return pref.getString(KEY_EMAIL, "").replace("\"", "");
    }

    public Long getMerchantId() {
        Long defaultId = 1L;
        String merchantId = pref.getString(KEY_ID, String.valueOf(defaultId)).replace("\"", "");
        return Long.valueOf(merchantId);
    }

    public String getBusinessName() {
        return pref.getString(KEY_BUSINESS_NAME, "").replace("\"", "");
    }

    public String getMerchantFirstName() {
        return pref.getString(KEY_FIRST_NAME, "").replace("\"", "");
    }

    public String getMerchantLastName() {
        return pref.getString(KEY_LAST_NAME, "").replace("\"", "");
    }

    public String getFullMerchantName() {
        String fname = pref.getString(KEY_FIRST_NAME, "").replace("\"", "");
        String lname = pref.getString(KEY_LAST_NAME, null);
        return fname += lname != null ? " " + lname.replace("\"", "") : "";
    }

    public String getMerchantBusinessType() {
        return pref.getString(KEY_BUSINESS_TYPE, "").replace("\"", "");
    }

    public String getMerchantPhone() {
        return pref.getString(KEY_PHONE, "").replace("\"", "");
    }

    public String getMerchantCurrency() {
        return pref.getString(KEY_CURRENCY, "").replace("\"", "");
    }

    public String getAccessToken() {
        return pref.getString(KEY_ACCESS_TOKEN, "").replace("\"", "");
    }

    public String getClientKey() {
        return pref.getString(KEY_CLIENT_KEY, "").replace("\"", "");
    }

    public boolean isTokenValid() {
        if (!pref.getString(KEY_TOKEN_EXPIRY, "").isEmpty()) {
            /*the value from the server is already in Epoch seconds so convert to millis*/
            DateTime dateTime = new DateTime(
                    TimeUnit.SECONDS.toMillis(Long.parseLong(pref.getString(KEY_TOKEN_EXPIRY, ""))),
                    DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT")));
            return dateTime.isAfter(Calendar.getInstance().getTime().getTime());
        }
        return false;
    }

    public String getKeyTokenExpiry() {
        return pref.getString(KEY_TOKEN_EXPIRY, "").replace("\"", "");
    }

    public void setKeyTokenExpiry(String keyTokenExpiry) {
        editor.putString(KEY_TOKEN_EXPIRY, keyTokenExpiry);
        editor.apply();
    }

    /**
     * Check login method wil check merchant login status
     * If false it will redirect merchant to login page
     * Else won't do anything
     * */
    public void checkLogin() {
        if (!this.isLoggedIn()) {
            Intent i = new Intent(_context, MerchantLoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            _context.startActivity(i);
        }
    }

    /**
     * Clear session details
     * */
    public void logoutMerchant() {
        new ClearSessionData(_context).execute();
    }

    public void setKeyAccessToken(String keyAccessToken) {
        editor.putString(KEY_ACCESS_TOKEN, keyAccessToken);
        editor.apply();
    }

    public void setKeyClientKey(String keyClientKey) {
        editor.putString(KEY_CLIENT_KEY, keyClientKey);
        editor.apply();
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
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
                AccountManager accountManager = AccountManager.get(mContext);
                Account[] accounts = accountManager.getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
                for (Account account : accounts) {
                    if (getMerchantEmail().equals(account.name)) {

                        if (Build.VERSION.SDK_INT >= 22) {
                            accountManager.removeAccount(account, null, null, null);
                        }
                        else {
                            //noinspection deprecation
                            accountManager.removeAccount(account, null, null);
                        }
                    }
                }
            }
            editor.clear();
            editor.apply();
            FirebaseAuth.getInstance().signOut();
            Smooch.logout();
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
            SharedPreferences.Editor defaultSharedPreferencesEditor = defaultSharedPreferences.edit();
            defaultSharedPreferencesEditor.clear();
            defaultSharedPreferencesEditor.apply();
            return null;
        }

        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Signing out...");
            this.dialog.show();
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
     * **/
    // Get Login State
    public boolean isLoggedIn(){
        return pref.getBoolean(IS_LOGIN, false);
    }
}
