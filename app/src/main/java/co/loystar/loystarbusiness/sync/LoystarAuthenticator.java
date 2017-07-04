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

import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.MerchantSignInSuccessResponse;
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
            AccountAuthenticatorResponse response,
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

        /*Extract the username and password from the Account Manager, and ask
        the server for an appropriate AuthToken.*/
        final AccountManager am = AccountManager.get(mContext);

        final String[] authToken = {am.peekAuthToken(account, authTokenType)};

        Log.d("loystar", TAG + "> peekAuthToken returned - " + authToken[0]);

        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken[0])) {
            final String password = am.getPassword(account);
            if (password != null) {
                try {
                    Log.d("loystar", TAG + "> re-authenticating with the existing password");
                    mApiClient.getLoystarApi().signInMerchant(account.name, password).enqueue(new Callback<MerchantSignInSuccessResponse>() {
                        @Override
                        public void onResponse(Call<MerchantSignInSuccessResponse> call, Response<MerchantSignInSuccessResponse> response) {
                            if (response.isSuccessful()) {
                                authToken[0] = response.headers().get("Access-Token");
                            }
                        }

                        @Override
                        public void onFailure(Call<MerchantSignInSuccessResponse> call, Throwable t) {
                            //Crashlytics.log(2, TAG, t.getMessage());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken[0])) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken[0]);
            return result;
        }

        /*If we get here, then we couldn't access the user's password - so we
         need to re-prompt them for their credentials. We do that by creating
         an intent to display our AuthenticatorActivity.*/
        final Intent intent = new Intent(mContext, MerchantLoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
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
