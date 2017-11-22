package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.Callable;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.api.ApiUtils;
import co.loystar.loystarbusiness.fragments.MerchantSignUpStepOneFragment;
import co.loystar.loystarbusiness.fragments.MerchantSignUpStepTwoFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.databinders.MerchantWrapper;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import io.reactivex.Completable;
import io.requery.BlockingEntityStore;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MerchantSignUpActivity extends AppCompatActivity implements
        MerchantSignUpStepOneFragment.OnMerchantSignUpStepOneFragmentInteractionListener,
        MerchantSignUpStepTwoFragment.OnMerchantSignUpStepTwoFragmentInteractionListener{

    private SharedPreferences sharedPref;
    private ProgressDialog progressDialog;
    private View mLayout;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_sign_up);
        Toolbar toolbar = findViewById(R.id.activity_merchant_sign_up_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mLayout = findViewById(R.id.activity_merchant_sign_up_wrapper);
        sharedPref = getSharedPreferences(getString(R.string.merchant_sign_up_pref), Context.MODE_PRIVATE);
        String merchantPhoneNumber = getIntent().getStringExtra(Constants.PHONE_NUMBER);
        Bundle data = new Bundle();

        data.putString(Constants.PHONE_NUMBER, merchantPhoneNumber);
        MerchantSignUpStepOneFragment stepOneFragment = new MerchantSignUpStepOneFragment();
        stepOneFragment.setArguments(data);
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_merchant_sign_up_container, stepOneFragment).commit();

    }

    @Override
    public void onMerchantSignUpStepOneFragmentInteraction() {
        MerchantSignUpStepTwoFragment signUpStepTwoFragment = new MerchantSignUpStepTwoFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_merchant_sign_up_container, signUpStepTwoFragment).addToBackStack(null).commit();
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
    public void onMerchantSignUpStepTwoFragmentInteraction() {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.signing_up_msg));
        progressDialog.show();

        final String mPassword = sharedPref.getString(Constants.PASSWORD, "");
        ApiClient apiClient = new ApiClient(mContext);
        apiClient.getLoystarApi(false)
                .signUpMerchant(
                        sharedPref.getString(Constants.FIRST_NAME, ""),
                        sharedPref.getString(Constants.BUSINESS_EMAIL, ""),
                        sharedPref.getString(Constants.BUSINESS_NAME, ""),
                        sharedPref.getString(Constants.PHONE_NUMBER, ""),
                        sharedPref.getString(Constants.BUSINESS_CATEGORY, ""),
                        sharedPref.getString(Constants.CURRENCY, ""),
                        mPassword).enqueue(new Callback<MerchantWrapper>() {
            @Override
            public void onResponse(Call<MerchantWrapper> call, Response<MerchantWrapper> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (response.isSuccessful()) {
                    String authToken = response.headers().get("Access-Token");
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

                    SessionManager sessionManager = new SessionManager(mContext);
                    sessionManager.setMerchantSessionData(
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
                    editor.putBoolean(getString(R.string.pref_turn_on_pos_key), merchant.isTurn_on_point_of_sale() != null && merchant.isTurn_on_point_of_sale());
                    editor.apply();

                    SharedPreferences.Editor signUpEditor = sharedPref.edit();
                    signUpEditor.clear();
                    signUpEditor.apply();

                    Bundle bundle = new Bundle();
                    Intent intent = new Intent();

                    bundle.putString(AccountManager.KEY_ACCOUNT_NAME, merchant.getEmail());
                    bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                    bundle.putString(AuthenticatorActivity.PARAM_USER_PASS, mPassword);
                    intent.putExtras(bundle);

                    setResult(RESULT_OK, intent);
                    finish();


                } else if (response.code() == 422){
                    ObjectMapper mapper = ApiUtils.getObjectMapper(false);
                    try {
                        JsonNode responseObject = mapper.readTree(response.errorBody().charStream());
                        JSONObject errorObject = new JSONObject(responseObject.toString());
                        JSONObject errors = errorObject.getJSONObject("errors");
                        JSONArray fullMessagesArray = errors.getJSONArray("full_messages");
                        Snackbar.make(mLayout, fullMessagesArray.join(", "), Snackbar.LENGTH_LONG).show();
                    } catch (IOException | JSONException e) {
                        showSnackbar(R.string.unknown_error);
                        e.printStackTrace();
                    }
                } else {
                    showSnackbar(R.string.unknown_error);
                }
            }

            @Override
            public void onFailure(Call<MerchantWrapper> call, Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                showSnackbar(R.string.error_internet_connection_timed_out);
            }
        });
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}