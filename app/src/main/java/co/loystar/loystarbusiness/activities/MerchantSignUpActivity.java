package co.loystar.loystarbusiness.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.MerchantSignInSuccessResponse;
import co.loystar.loystarbusiness.fragments.MerchantSignUpFinalStepFragment;
import co.loystar.loystarbusiness.fragments.MerchantSignUpStepOneFragment;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static co.loystar.loystarbusiness.activities.MerchantLoginActivity.ARG_ACCOUNT_TYPE;
import static co.loystar.loystarbusiness.activities.MerchantLoginActivity.AUTH_TOKEN;
import static co.loystar.loystarbusiness.activities.MerchantLoginActivity.PARAM_USER_PASS;

/**
 * Created by ordgen on 7/4/17.
 */

public class MerchantSignUpActivity extends AppCompatActivity implements
        MerchantSignUpStepOneFragment.OnMerchantSignUpStepOneInteractionListener,
        MerchantSignUpFinalStepFragment.OnMerchantSignUpFinalFragmentInteractionListener {

    private FragmentManager frgManager = getSupportFragmentManager();
    private static final String TAG = MerchantSignUpActivity.class.getCanonicalName();
    private SharedPreferences sharedPref;
    private ProgressDialog progressDialog;
    public static final String KEY_SIGN_UP = "isSignUp";
    private View mLayout;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_signup);
        mLayout = findViewById(R.id.sign_up_main_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        context = this;
        String digitsPhoneNumber = getIntent().getStringExtra(MerchantBackOffice.CUSTOMER_PHONE_NUMBER);
        Bundle data = new Bundle();

        data.putString(MerchantBackOffice.CUSTOMER_PHONE_NUMBER, digitsPhoneNumber);
        MerchantSignUpStepOneFragment stepOneFragment = new MerchantSignUpStepOneFragment();
        stepOneFragment.setArguments(data);
        frgManager.beginTransaction().replace(R.id.sign_up_container, stepOneFragment).commit();
    }

    @Override
    public void onMerchantSignUpStepOneInteraction(String uri) {
        if (uri.equals(getString(R.string.already_have_account))) {
            setResult(RESULT_CANCELED);
            finish();
        }
        else if (uri.equals(getString(R.string.go))) {
            MerchantSignUpFinalStepFragment finalStepFragment = new MerchantSignUpFinalStepFragment();
            frgManager.beginTransaction().replace(R.id.sign_up_container, finalStepFragment).addToBackStack(null).commit();
        }
    }

    @Override
    public void onMerchantSignUpFinalFragmentInteraction(String uri) {
        if (uri.equals(getString(R.string.already_have_account))) {
            setResult(RESULT_CANCELED);
            finish();
        }
        else if (uri.equals(getString(R.string.go))) {
            createAccount();
        }
    }

    private void createAccount() {
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        progressDialog = new ProgressDialog(MerchantSignUpActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.signing_up_msg));
        progressDialog.show();

        String businessName = sharedPref.getString("business_name", null);
        final String businessEmail = sharedPref.getString("business_email", null);
        final String accountPassword = sharedPref.getString("password", null);
        String businessPhone = sharedPref.getString("business_phone", null);
        String businessType = sharedPref.getString("business_type", null);
        String currency = sharedPref.getString("currency", null);
        String firstName = sharedPref.getString("first_name", null);

        ApiClient apiClient = LoystarApplication.getInstance().getApiClient();
        apiClient.getLoystarApi().signUpMerchant(firstName, businessEmail, businessName, businessPhone, businessType, currency, accountPassword).enqueue(new Callback<MerchantSignInSuccessResponse>() {
            @Override
            public void onResponse(Call<MerchantSignInSuccessResponse> call, Response<MerchantSignInSuccessResponse> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (response.isSuccessful()) {
                    MerchantSignInSuccessResponse signInSuccessResponse = response.body();
                    DBMerchant merchant = signInSuccessResponse.getData();
                    databaseHelper.insertMerchant(merchant);

                    String token = response.headers().get("Access-Token");
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MerchantSignUpActivity.this);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.pref_turn_on_pos_key), merchant.getTurn_on_point_of_sale() != null ? merchant.getTurn_on_point_of_sale() : false);
                    editor.apply();

                    sessionManager.setMerchantSessionData(
                            merchant.getBusiness_name(),
                            merchant.getEmail(),
                            String.valueOf(merchant.getId()),
                            merchant.getCurrency(),
                            merchant.getFirst_name(),
                            merchant.getLast_name(),
                            merchant.getBusiness_type(),
                            merchant.getContact_number(),
                            token,
                            response.headers().get("Client"),
                            response.headers().get("Expiry")
                    );

                    final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
                    final Intent intent = new Intent();
                    Bundle data = new Bundle();


                    data.putString(KEY_ACCOUNT_NAME, merchant.getEmail());
                    data.putString(KEY_ACCOUNT_TYPE, accountType);
                    data.putString(PARAM_USER_PASS, sharedPref.getString("password", null));
                    data.putString(AUTH_TOKEN, token);
                    data.putBoolean(KEY_SIGN_UP, true);

                    intent.putExtras(data);
                    setResult(RESULT_OK, intent);
                    SharedPreferences.Editor sign_up_pref = sharedPref.edit();
                    sign_up_pref.clear();
                    sign_up_pref.apply();
                    finish();
                }
                else {
                    Snackbar.make(mLayout, getString(R.string.error_signup), Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<MerchantSignInSuccessResponse> call, Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                //Crashlytics.log(2, TAG, t.getMessage());
                Snackbar.make(mLayout, getString(R.string.error_internet_connection), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                }
                else {
                    onBackPressed();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
