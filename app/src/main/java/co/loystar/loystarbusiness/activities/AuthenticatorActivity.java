package co.loystar.loystarbusiness.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.accountkit.AccessToken;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.PhoneNumber;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.Auth;
import com.google.firebase.auth.FirebaseAuth;

import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.databinders.MerchantWrapper;
import co.loystar.loystarbusiness.models.databinders.PhoneNumberAvailability;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * Account Authenticator Activity
 * we cannot extend AccountAuthenticatorActivity on this activity because
 * AccountAuthenticatorActivity extends Activity and not AppCompatActivity
 * We want to use the support library hence AppCompatActivity
 * We use setAccountAuthenticatorResult() to set the result of adding an account
 * */
public class AuthenticatorActivity extends BaseActivity implements LoaderCallbacks<Cursor> {
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    private final int REQ_SIGN_UP = 101;
    private static final int REQ_VERIFY_PHONE_NUMBER = 120;

    private Context mContext;
    private ApiClient mApiClient;
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private AccountManager mAccountManager;
    private Bundle mResultBundle = null;
    private FirebaseAuth mAuth;

    private static final int REQUEST_READ_CONTACTS = 0;
    private SessionManager mSessionManager;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private View mLayout;
    PhoneNumber verifiedPhoneNo;


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
        mAuth = FirebaseAuth.getInstance(); // for firebase



        mEmailView = findViewById(R.id.email);
        mLayout = findViewById(R.id.auth_root_layout);
        populateAutoComplete();

        mPasswordView = findViewById(R.id.password);
        TextInputLayout passwordLayout = findViewById(R.id.passwordLayout);
        passwordLayout.setPasswordVisibilityToggleEnabled(true);

        BrandButtonNormal mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(view -> attemptLogin());

        mLoginFormView = findViewById(R.id.email_login_form);
        mProgressView = findViewById(R.id.login_progress);

        TextView forgot_pass = findViewById(R.id.forgot_password);
        if (forgot_pass != null) {
            forgot_pass.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }

        //binding Sign up button with firebase login
       /* findViewById(R.id.sign_up_btn).setOnClickListener(view -> startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(Collections.singletonList(
                                new AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build()
                        ))
                        .build(),
                REQ_VERIFY_PHONE_NUMBER));*/

        //binding Sign up button with Facebook Account kit
        findViewById(R.id.sign_up_btn).setOnClickListener(view -> startSignupPage(LoginType.PHONE));



        //Firebase login check if phone is already verified...later change to facebook Ak
        /*if (mAuth.getCurrentUser() != null && !mSessionManager.isLoggedIn()) {
            Intent signUp = new Intent(mContext, MerchantSignUpActivity.class);
            if (getIntent().getExtras() != null) {
                signUp.putExtras(getIntent().getExtras());
            }
            signUp.putExtra(Constants.PHONE_NUMBER, mAuth.getCurrentUser().getPhoneNumber());
            startActivityForResult(signUp, REQ_SIGN_UP);
        }*/

        //Account kit  login check if phone is already verified.
        AccessToken accountKitAccessToken = AccountKit.getCurrentAccessToken();


        if (accountKitAccessToken != null && !mSessionManager.isLoggedIn())  // Number verified, but not loggedin,
        {
            Toast.makeText(this,"verified user, not loggedin ", Toast.LENGTH_SHORT).show();

            Toast.makeText(this,"existing verified number. Token-"+accountKitAccessToken.toString()+"loggedin? - ", Toast.LENGTH_SHORT).show();
            //load sign up form with number pre-filled, and unedittable.
            Intent signUp = new Intent(mContext, MerchantSignUpActivity.class);
            if (getIntent().getExtras() != null) {
                signUp.putExtras(getIntent().getExtras());
            }
                if(verifiedPhoneNo != null)
                {
                    Toast.makeText(this,"VerifiedPhne is not null", Toast.LENGTH_SHORT).show();
                    signUp.putExtra(Constants.PHONE_NUMBER, verifiedPhoneNo.toString()); //get number to pass to reg page
                    startActivityForResult(signUp, REQ_SIGN_UP);
                }
                else
                {
                    Toast.makeText(this,"VerifiedPhne IS null", Toast.LENGTH_SHORT).show();

                }


        }
        else{
            Toast.makeText(this,"New user ", Toast.LENGTH_SHORT).show();
        }





    } // End on onCreate

    //First point of call for Facebook account kit
    private void startSignupPage(LoginType loginType)
    {
        Toast.makeText(this,"First point!", Toast.LENGTH_SHORT).show();

        Intent verifyPhone = new Intent(mContext, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder builder = new AccountKitConfiguration.AccountKitConfigurationBuilder(loginType,AccountKitActivity.ResponseType.TOKEN);
        verifyPhone.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,builder.build());
        startActivityForResult(verifyPhone,REQ_VERIFY_PHONE_NUMBER); // launch account kit activity to verify no
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SIGN_UP && resultCode == RESULT_OK)
        {
            Toast.makeText(this,"Phone number verified, sign  up users", Toast.LENGTH_SHORT).show(); //signup complete
            finishLogin(data);


        }
        if (requestCode == REQ_VERIFY_PHONE_NUMBER) // Verify number on Loystar server..
        {
            Toast.makeText(this,"New user, verify phone", Toast.LENGTH_SHORT).show();
            //Facebook Acccount kit to verify no
            initializeAccountKit(data,resultCode);


        }
    }

    private void initializeAccountKit(Intent data, int resultCode)
    {

        AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
        if(result.getError() !=null)
        {
            Toast.makeText(this,""+result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();

        }
        else if(result.wasCancelled())
        {
            Toast.makeText(this,"cancel",Toast.LENGTH_SHORT).show();
        }
        else
        {
            if (result.getAccessToken() != null)
            {
                // Use the AlertDialog.Builder to configure the AlertDialog.
               /* AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                        .setTitle("Please wait")
                        .setMessage("");
                AlertDialog alertDialog = alertDialogBuilder.show();*/

                //showProgress(true);



                //get user phone and check if on server
                AccountKit.getCurrentAccount(new AccountKitCallback<com.facebook.accountkit.Account>() {
                    @Override
                    public void onSuccess(com.facebook.accountkit.Account account) {
                        //Phone number verified successfully, now check if merchant exist
                        verifiedPhoneNo = account.getPhoneNumber(); // for passing phone no to signup form


                        //Check if verified phone is  new merchant or not.
                        handlePhoneVerificationResponse(resultCode, data);



                        Log.d("staatus", "On success of AccountKit.getCurrentAccount"+verifiedPhoneNo.toString());
                    }

                    @Override
                    public void onError(AccountKitError accountKitError) {
                        Log.d("Error",accountKitError.getErrorType().getMessage());
                    }
                });

            }
        }
    }
    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
    }


    //Check results a phone verification server call
    private void handlePhoneVerificationResponse(int resultCode, Intent data) {
        Snackbar.make(mLayout, "handlePhoneVerificationResponse()", Snackbar.LENGTH_LONG).show();

        final IdpResponse idpResponse = IdpResponse.fromResultIntent(data);
        // above is Some firebase stuff.. needs to change to fbac. Checking if init. so we can grab phone no to send


        if (resultCode == RESULT_OK && AccountKit.getCurrentAccessToken() != null)
        {
            Snackbar.make(mLayout, "Would you ever get here.", Snackbar.LENGTH_LONG).show();

            final ProgressDialog progressDialog = new ProgressDialog(mContext);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.a_moment));
            progressDialog.show();

            //Lets go to the server and check if the phone no is new or not
            mApiClient.getLoystarApi(false)
                    .checkMerchantPhoneNumberAvailability(verifiedPhoneNo.toString())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(bindToLifecycle())
                    .subscribe(
                            response -> {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                if (response.isSuccessful()) {
                                    PhoneNumberAvailability phoneNumberAvailability = response.body();
                                    if (phoneNumberAvailability == null) {
                                        showSnackbar(R.string.unknown_error);
                                    } else {
                                        if (phoneNumberAvailability.isPhoneAvailable()) //Actual checking of phone number
                                        {
                                            Intent signUp = new Intent(mContext, MerchantSignUpActivity.class);
                                            if (getIntent().getExtras() != null) {
                                                signUp.putExtras(getIntent().getExtras());
                                            }
                                            signUp.putExtra(Constants.PHONE_NUMBER, verifiedPhoneNo.toString());
                                            startActivityForResult(signUp, REQ_SIGN_UP); //Phone number is available to register
                                        } else {
                                            showSnackbar(R.string.account_with_phone_exists);
                                            //mAuth.signOut();
                                            AccountKit.logOut();

                                        }
                                    }
                                }
                            },
                            e -> {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                showSnackbar(R.string.error_internet_connection_timed_out);
                            });
        } else {
            /*Verification failed*/
            if (idpResponse == null) {
                /*User pressed back button*/
                return;
            }

            if (idpResponse.getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackbar(R.string.no_internet_connection);
                return;
            }

            if (idpResponse.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
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
                    .setAction(android.R.string.ok, v -> requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS));
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
            loginMerchant(email, password);
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
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    //Actual Authentication
    private void loginMerchant(String email, String password) { //Actual authentication
        mApiClient.getLoystarApi(false).signInMerchant(email, password)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(
                response -> {
                    showProgress(false);
                    if (response.isSuccessful()) {
                        String authToken = response.headers().get("Access-Token");
                        String client = response.headers().get("Client");
                        MerchantWrapper merchantWrapper = response.body();
                        if (merchantWrapper == null) {
                            showSnackbar(R.string.unknown_error);
                        } else {
                            Merchant merchant = merchantWrapper.getMerchant();
                            final MerchantEntity merchantEntity = new MerchantEntity();
                            merchantEntity.setId(merchant.getId());
                            merchantEntity.setFirstName(merchant.getFirst_name());
                            merchantEntity.setLastName(merchant.getLast_name());
                            merchantEntity.setBusinessName(merchant.getBusiness_name());
                            merchantEntity.setEmail(merchant.getEmail());
                            merchantEntity.setBusinessType(merchant.getBusiness_type());
                            merchantEntity.setContactNumber(merchant.getContact_number());
                            merchantEntity.setSyncFrequency(merchant.getSync_frequency());
                            merchantEntity.setBluetoothPrintEnabled(merchant.getEnable_bluetooth_printing());
                            merchantEntity.setCurrency(merchant.getCurrency());
                            merchantEntity.setAddressLine1(merchant.getAddress_line1());
                            merchantEntity.setAddressLine2(merchant.getAddress_line2());

                            if (merchant.getSubscription_expires_on() != null) {
                                merchantEntity.setSubscriptionExpiresOn(new Timestamp(merchant.getSubscription_expires_on().getMillis()));
                            }

                            DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
                            databaseManager.insertNewMerchant(merchantEntity);
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
                                    client,
                                    merchant.getAddress_line1(), merchant.getAddress_line2());

                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(getString(R.string.pref_turn_on_pos_key), merchant.isTurn_on_point_of_sale() != null && merchant.isTurn_on_point_of_sale());
                            editor.putBoolean(mContext.getString(R.string.pref_enable_bluetooth_print_key), merchant.getEnable_bluetooth_printing() != null && merchant.getEnable_bluetooth_printing());
                            editor.putString("sync_frequency", String.valueOf(merchant.getSync_frequency()));
                            editor.apply();

                            Bundle bundle = new Bundle();
                            Intent intent = new Intent();

                            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, merchant.getEmail());
                            bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                            bundle.putString(AccountManager.KEY_PASSWORD, password);

                            intent.putExtras(bundle);
                            finishLogin(intent);
                        }

                    } else if (response.code() == 401) {
                        Intent intent = new Intent();
                        intent.putExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE, getString(R.string.error_login_credentials));
                        finishLogin(intent);
                    } else {
                        Intent intent = new Intent();
                        intent.putExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE, getString(R.string.unknown_error));
                        finishLogin(intent);
                    }
                },
                e -> {
                    showProgress(false);
                    Intent intent = new Intent();
                    if (e instanceof SocketTimeoutException) {
                        intent.putExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE, getString(R.string.error_internet_connection_timed_out));
                    } else {
                        intent.putExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE, getString(R.string.unknown_error));
                    }
                    finishLogin(intent);
                });
    }

    private void finishLogin(Intent intent) {
        if (intent.hasExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE)) {
            setResult(RESULT_CANCELED);
            Snackbar.make(mLayout, intent.getStringExtra(AccountManager.KEY_AUTH_FAILED_MESSAGE), Snackbar.LENGTH_LONG).show();
        }
        else //successful authentication login and new signup
            {
            String accountPassword = intent.getStringExtra(AccountManager.KEY_PASSWORD);
            String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

            // Creating the account on the device and setting the auth token we got
            final Account account = AccountGeneral.addOrFindAccount(mContext, accountName, accountPassword);
            AccountGeneral.SetSyncAccount(mContext, account);
            if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
                String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
                // (Not setting the auth token will cause another call to the server to authenticate the user)
                mAccountManager.setAuthToken(account, AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS, authToken);
            }

            setResult(RESULT_OK, intent);
            Intent homeIntent = new Intent(AuthenticatorActivity.this, MerchantBackOfficeActivity.class);
            homeIntent.putExtra(Constants.IS_NEW_LOGIN, true);
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

