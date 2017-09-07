package co.loystar.loystarbusiness.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Collections;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.MerchantSignInSuccessResponse;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_ERROR_MESSAGE;
import static co.loystar.loystarbusiness.sync.AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS;

/**
 * Account Authenticator Activity
 * we cannot extend AccountAuthenticatorActivity on this activity because
 * AccountAuthenticatorActivity extends Activity and not AppCompatActivity
 * We want to use the support library hence AppCompatActivity
 * We use setAccountAuthenticatorResult() to set the result of adding an account
 * */
public class MerchantLoginActivity extends AppCompatActivity {

    /*Constants*/
    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public final static String PARAM_USER_PASS = "USER_PASS";
    public final static String IS_NEW_LOGIN = "isNewLogin";
    public final static String AUTH_TOKEN = "authToken";
    private static final int REQ_VERIFY_PHONE_NUMBER = 100;
    private static final String TAG = MerchantLoginActivity.class.getSimpleName();

    private final Context mContext = MerchantLoginActivity.this;
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private final int REQ_SIGN_UP = 10;
    private AccountManager mAccountManager;
    private String mAuthTokenType;
    private Bundle mResultBundle = null;
    private ApiClient mApiClient = LoystarApplication.getInstance().getApiClient();
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mLayout;
    private String password;
    private String email;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountAuthenticatorResponse = getIntent().getParcelableExtra( AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE );
        if( mAccountAuthenticatorResponse != null ) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
        setContentView(R.layout.activity_merchant_login);
        mLayout = findViewById(R.id.login_root_layout);
        mAccountManager = AccountManager.get(mContext);
        mEmailView = findViewById(R.id.email);

        Button signUp = findViewById(R.id.sign_up_btn);
        signUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    Intent signUp = new Intent(mContext, MerchantSignUpActivity.class);
                    signUp.putExtras(getIntent().getExtras());
                    signUp.putExtra(MerchantBackOffice.CUSTOMER_PHONE_NUMBER, auth.getCurrentUser().getPhoneNumber());
                    startActivityForResult(signUp, REQ_SIGN_UP);
                } else {
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(Collections.singletonList(
                                            new AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build()
                                    ))
                                    .build(),
                            REQ_VERIFY_PHONE_NUMBER);
                }
            }
        });

        TextView forgot_pass = findViewById(R.id.forgot_password);
        TextInputLayout passwordLayout = findViewById(R.id.passwordLayout);
        passwordLayout.setPasswordVisibilityToggleEnabled(true);
        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        if (forgot_pass != null) {
            forgot_pass.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                Intent intent = new Intent(mContext, ForgotPasswordActivity.class);
                startActivity(intent);
                }
            });
        }

        //Account authentication section
        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (mAuthTokenType == null)
            mAuthTokenType = AUTHTOKEN_TYPE_FULL_ACCESS;
        if (accountName != null) {
            mEmailView.setText(accountName);
        }

        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);

        if (mEmailSignInButton != null) {
            mEmailSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });
        }

        mLoginFormView = findViewById(R.id.email_login_form);
        mProgressView = findViewById(R.id.login_progress);
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
            signUp.putExtra(MerchantBackOffice.CUSTOMER_PHONE_NUMBER, response.getPhoneNumber());
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

    private void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        email = mEmailView.getText().toString();
        password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError("Password is required!");
            focusView = mPasswordView;
            cancel = true;
        }
        else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_email_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            //hide keyboard
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            merchantLogin(email, password);
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 5;
    }

    private void merchantLogin(final String email, final String password) {

        mApiClient.getLoystarApi().signInMerchant(email, password).enqueue(new Callback<MerchantSignInSuccessResponse>() {
            @Override
            public void onResponse(Call<MerchantSignInSuccessResponse> call, Response<MerchantSignInSuccessResponse> response) {
                showProgress(false);

                if (response.isSuccessful()) {
                    MerchantSignInSuccessResponse signInSuccessResponse = response.body();
                    dispatchSignInSuccess(response, signInSuccessResponse.getData(), true);
                }
                else {
                    Intent intent = new Intent();
                    int statusCode = response.code();
                    switch (statusCode) {
                        case 401:
                            intent.putExtra(KEY_ERROR_MESSAGE, getString(R.string.error_login));
                            break;
                        default:
                            intent.putExtra(KEY_ERROR_MESSAGE, getString(R.string.error_failed_to_login));
                            break;
                    }
                    finishLogin(intent);
                }
            }

            @Override
            public void onFailure(Call<MerchantSignInSuccessResponse> call, Throwable t) {
                showProgress(false);
                Snackbar.make(mLayout, getString(R.string.error_internet_connection_timed_out), Snackbar.LENGTH_LONG).show();
            }
        });

    }

    private void finishLogin(Intent intent) {
        if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
            setResult(RESULT_CANCELED);
            showProgress(false);
            Snackbar.make(mLayout, intent.getStringExtra(KEY_ERROR_MESSAGE), Snackbar.LENGTH_LONG).show();
        } else {
            String accountName = intent.getStringExtra(KEY_ACCOUNT_NAME);
            String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
            final Account account = new Account(accountName, intent.getStringExtra(KEY_ACCOUNT_TYPE));

            if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
                String authTokenType = mAuthTokenType;
                // Creating the account on the device and setting the auth token we got
                // (Not setting the auth token will cause another call to the server to authenticate the user)
                mAccountManager.addAccountExplicitly(account, accountPassword, null);
                mAccountManager.setAuthToken(account, authTokenType, intent.getStringExtra(AUTH_TOKEN));
                SyncAdapter.onAccountLogin(mContext, account);
                setAccountAuthenticatorResult(intent.getExtras());
                setResult(RESULT_OK, intent);
                showProgress(false);
                Intent merchantBackOfficeIntent = new Intent(mContext, MerchantBackOffice.class);
                merchantBackOfficeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                merchantBackOfficeIntent.putExtra(MerchantBackOffice.NEW_LOGIN, intent.getBooleanExtra(IS_NEW_LOGIN, false));
                merchantBackOfficeIntent.putExtra(
                        MerchantSignUpActivity.KEY_SIGN_UP, intent.getBooleanExtra(MerchantSignUpActivity.KEY_SIGN_UP, false)
                );
                startActivity(merchantBackOfficeIntent);
                clearBackStack();
                finish();

            } else {
                mAccountManager.setPassword(account, accountPassword);
            }
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

    private void clearBackStack() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry first = manager.getBackStackEntryAt(0);
            manager.popBackStack(first.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void dispatchSignInSuccess(retrofit2.Response response, DBMerchant merchant, boolean isPassword) {

        if (databaseHelper.getMerchantById(merchant.getId()) == null) {
            databaseHelper.insertMerchant(merchant);
        }
        else {
            databaseHelper.updateMerchant(merchant);
        }

        String token = response.headers().get("Access-Token");
        String client = response.headers().get("Client");
        String expiry = response.headers().get("Expiry");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.pref_turn_on_pos_key), merchant.getTurn_on_point_of_sale() != null ? merchant.getTurn_on_point_of_sale() : false);
        editor.apply();

        sessionManager.setMerchantSessionData(
            merchant.getBusiness_name(),
            merchant.getEmail(),
            merchant.getId().toString(),
            merchant.getCurrency(),
            merchant.getFirst_name(),
            merchant.getLast_name(),
            merchant.getBusiness_type(),
            merchant.getContact_number(),
            token,
            client,
            expiry
        );

        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
        final Intent intent = new Intent();
        Bundle data = new Bundle();

        if (isPassword) {
            data.putString(KEY_ACCOUNT_NAME, email);
            data.putString(PARAM_USER_PASS, password);
        }

        data.putString(KEY_ACCOUNT_TYPE, accountType);
        data.putString(AUTH_TOKEN, token);
        data.putBoolean(IS_NEW_LOGIN, true);
        intent.putExtras(data);
        finishLogin(intent);
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
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

}

