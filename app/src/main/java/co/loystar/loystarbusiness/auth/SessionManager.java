package co.loystar.loystarbusiness.auth;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.SplashActivity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

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
    private static  final String ADDRESS_LINE1 = "addressLine1";
    private static  final String ADDRESS_LINE2 = "addressLine2";
    private static final String MERCHANT_ID = "merchantId";
    private static final String CURRENCY = "currency";
    private static final String BUSINESS_TYPE = "businessType";
    private static final int PRIVATE_MODE = 0;

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
            String clientKey,
            String address_line1,
            String address_line2) {
        editor.putInt(MERCHANT_ID, id);
        editor.putString(EMAIL, email);
        editor.putString(FIRST_NAME, firstName);
        editor.putString(LAST_NAME, lastName);
        editor.putString(CONTACT_NUMBER, contactNumber);
        editor.putString(BUSINESS_NAME, businessName);
        editor.putString(ADDRESS_LINE1, address_line1);
        editor.putString(ADDRESS_LINE2, address_line2);
        editor.putString(BUSINESS_TYPE, businessType);
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

        if (TextUtils.isEmpty(lastName)) {
            lastName = "";
        } else {
            lastName = " " + TextUtilsHelper.capitalize(lastName);
        }

        return TextUtilsHelper.capitalize(getFirstName()) + lastName;
    }

    public String getBusinessName() {
        return sharedPreferences.getString(BUSINESS_NAME, "");
    }

    public String getAddressLine1() {
        return sharedPreferences.getString(ADDRESS_LINE1, "");
    }

    public String getAddressLine2() {
        return sharedPreferences.getString(ADDRESS_LINE2, "");
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
    public void signOutMerchant(Context context) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("Signing out...");

        Observable.fromCallable(() -> {
            FirebaseAuth.getInstance().signOut();
            sharedPreferences = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_key), PRIVATE_MODE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            return true;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe(disposable -> dialog.show())
        .subscribe(o -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Intent intent = new Intent(context, SplashActivity.class);
            intent.putExtra(Constants.SKIP_INTRO, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }

    /**
     * Quick check for login
     * Get Login State
     * **/
    public boolean isLoggedIn(){
        return sharedPreferences.getBoolean(IS_LOGGED_IN, false);
    }
}
