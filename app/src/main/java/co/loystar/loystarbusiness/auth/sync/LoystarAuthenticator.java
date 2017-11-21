package co.loystar.loystarbusiness.auth.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.Callable;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.AuthenticatorActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.databinders.MerchantWrapper;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import io.reactivex.Completable;
import io.requery.BlockingEntityStore;
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
                // authenticate merchant by email and password
                Call<MerchantWrapper> call = mApiClient.getLoystarApi(false).signInMerchant(account.name, password);
                try {
                    Response<MerchantWrapper> response = call.execute();
                    if (response.isSuccessful()) {
                        authToken = response.headers().get("Access-Token");
                        String client = response.headers().get("Client");
                        Merchant merchant = response.body().getMerchant();
                        final MerchantEntity merchantEntity = new MerchantEntity();
                        merchantEntity.setId(merchant.getId());
                        merchantEntity.setFirstName(merchant.getFirst_name());
                        merchantEntity.setLastName(merchant.getLast_name());
                        merchantEntity.setBusinessName(merchant.getBusiness_name());
                        merchantEntity.setEmail(merchant.getEmail());
                        merchantEntity.setBusinessType(merchant.getBusiness_type());
                        merchantEntity.setContactNumber(merchant.getContact_number());
                        merchantEntity.setCurrency(merchant.getCurrency());
                        if (merchant.getSubscription_expires_on() != null) {
                            merchantEntity.setSubscriptionExpiresOn(new Timestamp(merchant.getSubscription_expires_on().getMillis()));
                        }

                        final BlockingEntityStore mDataStore = DatabaseManager.getDataStore(mContext).toBlocking();
                        Completable completable = Completable.fromCallable(new Callable<Void>() {

                            @Override
                            public Void call() throws Exception {
                                mDataStore.runInTransaction(new Callable() {
                                    @Override
                                    public Void call() throws Exception {
                                        mDataStore.upsert(merchantEntity);
                                        return null;
                                    }
                                });
                                return null;
                            }
                        });
                        completable.subscribe();
                        mSessionManager.setMerchantSessionData(
                                merchant.getId(),
                                merchant.getEmail(),
                                merchant.getFirst_name(),
                                merchant.getLast_name(),
                                merchant.getContact_number(),
                                merchant.getBusiness_name(),
                                merchant.getBusiness_type(),
                                merchant.getCurrency(),
                                authToken,
                                client
                        );

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(mContext.getString(R.string.pref_turn_on_pos_key), merchant.isTurn_on_point_of_sale() != null && merchant.isTurn_on_point_of_sale());
                        editor.apply();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // authenticate merchant by phone number and firebase_uid
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
