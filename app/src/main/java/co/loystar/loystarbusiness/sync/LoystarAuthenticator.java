package co.loystar.loystarbusiness.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import co.loystar.loystarbusiness.activities.MerchantLoginActivity;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.MerchantSignInSuccessResponse;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 7/4/17.
 */

public class LoystarAuthenticator extends AbstractAccountAuthenticator {

    private final Context mContext;
    private String TAG = "LoystarAuthenticator";
    private ApiClient mApiClient;
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();

    public LoystarAuthenticator(Context context) {
        super(context);
        this.mContext = context;
        mApiClient = new ApiClient(context);
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        return null;
    }

    @Override
    public Bundle addAccount(
            AccountAuthenticatorResponse response,
            String accountType,
            String authTokenType,
            String[] requiredFeatures,
            Bundle options) throws NetworkErrorException {
        Log.d("loystar", TAG + "> addAccount");

        final Intent intent = new Intent(mContext, MerchantLoginActivity.class);
        intent.putExtra(MerchantLoginActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(MerchantLoginActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(MerchantLoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            String authTokenType,
            Bundle options) throws NetworkErrorException {
        Log.d("loystar", TAG + "> getAuthToken");

        /*If the caller requested an authToken type we don't support, then
        return an error*/
        if (!authTokenType.equals(AccountGeneral.AUTHTOKEN_TYPE_READ_ONLY) && !authTokenType.equals(AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        final AccountManager am = AccountManager.get(mContext);
        String authToken = am.peekAuthToken(account, authTokenType);

        // authToken will be empty if we invalidate the token
        // in that case we don't want to show the login screen immediately
        // we give another try to authenticate the user
        // by extracting the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        if (TextUtils.isEmpty(authToken)) {
            String password = am.getPassword(account);
            if (password != null) {
                Call<MerchantSignInSuccessResponse> call = mApiClient.getLoystarApi().signInMerchant(account.name, password);
                try {
                    Response<MerchantSignInSuccessResponse> response = call.execute();
                    if (response.isSuccessful()) {
                        authToken = response.headers().get("Access-Token");
                        String client = response.headers().get("Client");
                        String expiry = response.headers().get("Expiry");
                        DBMerchant merchant = response.body().getData();

                        sessionManager.setMerchantSessionData(
                                merchant.getBusiness_name(),
                                merchant.getEmail(),
                                merchant.getId().toString(),
                                merchant.getCurrency(),
                                merchant.getFirst_name(),
                                merchant.getLast_name(),
                                merchant.getBusiness_type(),
                                merchant.getContact_number(),
                                authToken,
                                client,
                                expiry
                        );
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        /*If we get here, then we couldn't access the user's password - so we
         need to re-prompt them for their credentials. We do that by creating
         an intent to display our AuthenticatorActivity.*/
        final Intent intent = new Intent(mContext, MerchantLoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        intent.putExtra(MerchantLoginActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(MerchantLoginActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(MerchantLoginActivity.ARG_ACCOUNT_NAME, account.name);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS.equals(authTokenType))
            return AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS_LABEL;
        else if (AccountGeneral.AUTHTOKEN_TYPE_READ_ONLY.equals(authTokenType))
            return AccountGeneral.AUTHTOKEN_TYPE_READ_ONLY_LABEL;
        else
            return authTokenType + " (Label)";
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) throws NetworkErrorException {
        return null;
    }
}
