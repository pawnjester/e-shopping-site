package co.loystar.loystarbusiness.auth.sync;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import co.loystar.loystarbusiness.activities.AuthenticatorActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

import static co.loystar.loystarbusiness.auth.sync.AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS;
import static co.loystar.loystarbusiness.auth.sync.AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS_LABEL;
import static co.loystar.loystarbusiness.auth.sync.AccountGeneral.AUTH_TOKEN_TYPE_READ_ONLY;
import static co.loystar.loystarbusiness.auth.sync.AccountGeneral.AUTH_TOKEN_TYPE_READ_ONLY_LABEL;

/**
 * Created by ordgen on 11/1/17.
 */

public class LoystarAuthenticator extends AbstractAccountAuthenticator {
    private Context mContext;
    private ApiClient mApiClient;
    private SessionManager mSessionManager;
    private static final String TAG = LoystarAuthenticator.class.getSimpleName();

    public LoystarAuthenticator(Context context) {
        super(context);
        this.mContext = context;
        mApiClient = new ApiClient(context);
        mSessionManager = new SessionManager(context);
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        return null;
    }

    @Override
    public Bundle addAccount(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            String accountType,
            String authTokenType,
            String[] requiredFeatures,
            Bundle bundle
    ) throws NetworkErrorException {
        Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);

        Bundle accountBundle = new Bundle();
        accountBundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return accountBundle;
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
            Bundle bundle
    ) throws NetworkErrorException {
        /*If the caller requested an authToken type we don't support, then
        return an error*/
        if (!authTokenType.equals(AUTH_TOKEN_TYPE_READ_ONLY) && !authTokenType.equals(AUTH_TOKEN_TYPE_FULL_ACCESS)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        /*Extract the username and password from the Account Manager, and ask
        the server for an appropriate AuthToken.*/
        final AccountManager am = AccountManager.get(mContext);
        String authToken = am.peekAuthToken(account, authTokenType);

        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            String password = am.getPassword(account);
            if (password != null && !TextUtils.isEmpty(password)) {
                Call<Merchant> call = mApiClient.getLoystarApi(true).signInMerchant(account.name, password);
                try {
                    Response<Merchant> response = call.execute();
                    if (response.isSuccessful()) {
                        Log.e(TAG, "getAuthToken: " + response.body().getContact_number() );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //get user by email and password
                /*JSONObject data = new JSONObject();
                JSONObject requestData = new JSONObject();
                try {
                    data.put("email", account.name);
                    data.put("password", password);
                    requestData.put("data", data);
                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                    Call<User> call = mApiClient.getLoystarApi().LoginUser(requestBody);
                    Response<User> response = call.execute();
                    if (response.isSuccessful()) {
                        User user = response.body();
                        UserEntity userEntity = new UserEntity();
                        userEntity.setId(user.getId());
                        userEntity.setEmail(user.getEmail());
                        userEntity.setFirstName(user.getFirst_name());
                        userEntity.setLastName(user.getLast_name());
                        userEntity.setPhoneNumber(user.getPhone_number());
                        userEntity.setSex(user.getGender());
                        userEntity.setCreatedAt(user.getCreated_at().toDate());
                        userEntity.setDateOfBirth(user.getDate_of_birth());

                        DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
                        databaseManager.addUser(userEntity);

                        authToken = response.headers().get("Access-Token");
                        mSessionManager.setUserSessionData(
                                user.getId(),
                                user.getEmail(),
                                user.getFirst_name(),
                                user.getLast_name(),
                                user.getPhone_number(),
                                authToken
                        );
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }*/
            } else {
                //login user by phone number and firebase_uid
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                /*if (firebaseUser != null) {
                    Call<User> call = mApiClient.getLoystarApi().LoginUserByPhone(mSessionManager.getPhoneNumber(), firebaseUser.getUid());
                    Response<User> response = null;
                    try {
                        response = call.execute();
                        if (response.isSuccessful()) {
                            User user = response.body();
                            UserEntity userEntity = new UserEntity();
                            userEntity.setId(user.getId());
                            userEntity.setEmail(user.getEmail());
                            userEntity.setFirstName(user.getFirst_name());
                            userEntity.setLastName(user.getLast_name());
                            userEntity.setPhoneNumber(user.getPhone_number());
                            userEntity.setSex(user.getGender());
                            userEntity.setCreatedAt(user.getCreated_at().toDate());
                            userEntity.setDateOfBirth(user.getDate_of_birth());

                            DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
                            databaseManager.addUser(userEntity);

                            authToken = response.headers().get("Access-Token");
                            mSessionManager.setUserSessionData(
                                    user.getId(),
                                    user.getEmail(),
                                    user.getFirst_name(),
                                    user.getLast_name(),
                                    user.getPhone_number(),
                                    authToken
                            );
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }*/
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

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        final Bundle accountBundle = new Bundle();
        accountBundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return accountBundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (AUTH_TOKEN_TYPE_FULL_ACCESS.equals(authTokenType))
            return AUTH_TOKEN_TYPE_FULL_ACCESS_LABEL;
        else if (AUTH_TOKEN_TYPE_READ_ONLY.equals(authTokenType))
            return AUTH_TOKEN_TYPE_READ_ONLY_LABEL;
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
