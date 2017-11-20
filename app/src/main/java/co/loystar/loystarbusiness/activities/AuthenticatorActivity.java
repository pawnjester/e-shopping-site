package co.loystar.loystarbusiness.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.ui.Constants;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.reactivex.Completable;
import io.requery.BlockingEntityStore;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;
import retrofit2.Call;
import retrofit2.Response;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * Account Authenticator Activity
 * we cannot extend AccountAuthenticatorActivity on this activity because
 * AccountAuthenticatorActivity extends Activity and not AppCompatActivity
 * We want to use the support library hence AppCompatActivity
 * We use setAccountAuthenticatorResult() to set the result of adding an account
 * */
public class AuthenticatorActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {
    private static final String TAG = AuthenticatorActivity.class.getSimpleName();
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public final static String PARAM_USER_PASS = "USER_PASS";
    private static final int RC_SIGN_IN = 100;
    public static final String ARG_ACCOUNT_TYPE = "ARG_ACCOUNT_TYPE";
    public static final String ARG_AUTH_TYPE = "ARG_AUTH_TYPE";
    private final int REQ_SIGN_UP = 101;
    private static final int REQ_VERIFY_PHONE_NUMBER = 120;

    private Context mContext;
    private ApiClient mApiClient;
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private AccountManager mAccountManager;
    private Bundle mResultBundle = null;
    private FirebaseAuth mAuth;

    private static final int REQUEST_READ_CONTACTS = 0;

    private UserLoginTask mAuthTask = null;
    private SessionManager mSessionManager;
    private DatabaseManager mDatabaseManager;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);

        mAccountAuthenticatorResponse = getIntent().getParcelableExtra( AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE );
        if( mAccountAuthenticatorResponse != null ) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
        mAccountManager = AccountManager.get(this);
        mContext = this;
        mSessionManager = new SessionManager(this);
        mApiClient = new ApiClient(this);
        mDatabaseManager = DatabaseManager.getInstance(this);
        mAuth = FirebaseAuth.getInstance();

        mEmailView = findViewById(R.id.email);
        mLayout = findViewById(R.id.auth_root_layout);
        populateAutoComplete();

        mPasswordView = findViewById(R.id.password);
        TextInputLayout passwordLayout = findViewById(R.id.passwordLayout);
        passwordLayout.setPasswordVisibilityToggleEnabled(true);

        BrandButtonNormal mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.email_login_form);
        mProgressView = findViewById(R.id.login_progress);

        findViewById(R.id.sign_up_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(Collections.singletonList(
                                        new AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build()
                                ))
                                .build(),
                        REQ_VERIFY_PHONE_NUMBER);
            }
        });

        TextView forgot_pass = findViewById(R.id.forgot_password);
        if (forgot_pass != null) {
            forgot_pass.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, ForgotPasswordActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (mAuth.getCurrentUser() != null && !mSessionManager.isLoggedIn()) {
            Intent signUp = new Intent(mContext, MerchantSignUpActivity.class);
            if (getIntent() != null) {
                signUp.putExtras(getIntent().getExtras());
            }
            startActivityForResult(signUp, REQ_SIGN_UP);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SIGN_UP && resultCode == RESULT_OK) {
            finishLogin(data);
        }
        if (requestCode == REQ_VERIFY_PHONE_NUMBER) {
            handlePhoneVerificationResponse(resultCode, data);
        }
    }

    private void handlePhoneVerificationResponse(int resultCode, Intent data) {
        IdpResponse response = IdpResponse.fromResultIntent(data);
        if (resultCode == RESULT_OK && response != null) {
            Intent signUp = new Intent(mContext, MerchantSignUpActivity.class);
            signUp.putExtras(getIntent().getExtras());
            signUp.putExtra(Constants.CUSTOMER_PHONE_NUMBER, response.getPhoneNumber());
            startActivityForResult(signUp, REQ_SIGN_UP);
        } else {
            /*Verification failed*/
            if (response == null) {
                /*User pressed back button*/
                showSnackbar(R.string.sign_up_cancelled);
                return;
            }

            if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackbar(R.string.no_internet_connection);
                return;
            }

            if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                showSnackbar(R.string.unknown_error);
            }
        }
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!TextUtilsHelper.isValidEmailAddress(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            closeKeyBoard();
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 5;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(AuthenticatorActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, Intent> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Intent doInBackground(Void... params) {
            Call<Merchant> call = mApiClient.getLoystarApi(true).signInMerchant(mEmail, mPassword);
            try {
                Response<Merchant> response = call.execute();
                if (response.isSuccessful()) {
                    String authToken = response.headers().get("Access-Token");
                    String client = response.headers().get("Client");
                    Merchant merchant = response.body();
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

                    Bundle bundle = new Bundle();
                    Intent res = new Intent();

                    bundle.putString(AccountManager.KEY_ACCOUNT_NAME, merchant.getEmail());
                    bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                    bundle.putString(PARAM_USER_PASS, mPassword);

                    res.putExtras(bundle);
                    return res;

                } else {
                    Intent res = new Intent();
                    res.putExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE, getString(R.string.error_login_credentials));
                    return res;
                }
            } catch (IOException e) {
                Intent res = new Intent();
                if (e instanceof SocketTimeoutException) {
                    res.putExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE, getString(R.string.error_internet_connection_timed_out));
                } else {
                    res.putExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE, getString(R.string.no_internet_connection));
                }
                return res;
            }
        }

        @Override
        protected void onPostExecute(final Intent intent) {
            mAuthTask = null;
            showProgress(false);

            if (intent != null) {
                finishLogin(intent);
            } else {
                showSnackbar(R.string.something_went_wrong);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    private void finishLogin(Intent intent) {
        if (intent.hasExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE)) {
            setResult(RESULT_CANCELED);
            Snackbar.make(mLayout, intent.getStringExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE), Snackbar.LENGTH_LONG).show();
        } else {
            String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
            String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            // Creating the account on the device and setting the auth token we got
            final Account account = AccountGeneral.getAccount(mContext, accountName);
            AccountGeneral.createSyncAccount(mContext, account);
            if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
                String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
                String authTokenType = AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS;

                mAccountManager.setPassword(account, accountPassword);
                // (Not setting the auth token will cause another call to the server to authenticate the user)
                mAccountManager.setAuthToken(account, authTokenType, authToken);
            } else {
                mAccountManager.setPassword(account, accountPassword);
            }
            setResult(RESULT_OK, intent);
            Intent homeIntent = new Intent(mContext, MerchantBackOfficeActivity.class);
            startActivity(homeIntent);
            finish();
            setAccountAuthenticatorResult(intent.getExtras());
        }
    }

    public final void setAccountAuthenticatorResult( Bundle result ) {
        mResultBundle = result;
    }

    public void finish() {
        if( mAccountAuthenticatorResponse != null ) {
            // send the result bundle back if set, otherwise send an error.
            if( mResultBundle != null ) {
                mAccountAuthenticatorResponse.onResult( mResultBundle );
            } else {
                mAccountAuthenticatorResponse.onError( AccountManager.ERROR_CODE_CANCELED, "canceled" );
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    private void closeKeyBoard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}

