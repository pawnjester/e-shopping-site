package co.loystar.loystarbusiness.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.api.ApiUtils;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.databinders.MerchantWrapper;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.pojos.BusinessType;
import co.loystar.loystarbusiness.models.BusinessTypesFetcher;
import co.loystar.loystarbusiness.utils.ui.Currency.Currency;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrencyPicker;
import co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput.InternationalPhoneInput;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyAccountProfileActivity extends AppCompatActivity
        implements CurrencyPicker.OnCurrencySelectedListener {

    private static final String TAG = MyAccountProfileActivity.class.getSimpleName();
    private SessionManager sessionManager;
    private DatabaseManager mDatabaseManager;
    private EditText fNameView;
    private EditText lNameView;
    private EditText emailView;
    private EditText businessName;
    private InternationalPhoneInput phoneInput;
    private View mLayout;
    private String selectedBusinessType;
    private String selectedCurrency;
    private ProgressDialog progressDialog;
    private ApiClient mApiClient;
    private MerchantEntity merchantEntity;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account_profile);
        Toolbar toolbar = findViewById(R.id.activity_my_account_profile_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mLayout = findViewById(R.id.activity_my_account_profile_wrapper);
        mContext = this;
        sessionManager = new SessionManager(this);
        mDatabaseManager = DatabaseManager.getInstance(this);
        merchantEntity = mDatabaseManager.getMerchant(sessionManager.getMerchantId());
        mApiClient = new ApiClient(this);

        ArrayList<BusinessType> getBusinessTypes = BusinessTypesFetcher.getBusinessTypes(this);
        final CharSequence[] businessTypeEntries = new CharSequence[getBusinessTypes.size()];
        for (int i = 0; i < getBusinessTypes.size(); i++) {
            businessTypeEntries[i] = getBusinessTypes.get(i).getTitle();
        }
        SpinnerButton businessTypeSpinner = findViewById(R.id.business_type_spinner);
        businessTypeSpinner.setEntries(businessTypeEntries);
        SpinnerButton.OnItemSelectedListener businessTypeSelectedListener = position -> selectedBusinessType = (String) businessTypeEntries[position];
        businessTypeSpinner.setListener(businessTypeSelectedListener);

        fNameView = findViewById(R.id.firstName);
        lNameView = findViewById(R.id.lastName);
        emailView = findViewById(R.id.email);
        businessName = findViewById(R.id.businessName);
        CurrencyPicker currencyPicker = findViewById(R.id.currency_spinner);
        phoneInput = findViewById(R.id.phone_number);

        if (merchantEntity != null) {
            businessTypeSpinner.setSelection(getBusinessTypes.indexOf(
                    BusinessTypesFetcher.getBusinessTypes(this)
                            .getBusinessTypeByTitle(sessionManager.getBusinessType()
                            )));
            fNameView.setText(merchantEntity.getFirstName());
            lNameView.setText(merchantEntity.getLastName());
            emailView.setText(merchantEntity.getEmail());
            businessName.setText(merchantEntity.getBusinessName());
            selectedBusinessType = merchantEntity.getBusinessType();
            selectedCurrency = merchantEntity.getCurrency();
            phoneInput.setNumber(merchantEntity.getContactNumber());
        }

        currencyPicker.setListener(this);
    }

    @Override
    public void onCurrencySelected(Currency currency) {
        selectedCurrency = currency.getCode();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_action_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_save:
                submitForm();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void submitForm() {

        if (TextUtils.isEmpty(fNameView.getText().toString())) {
            fNameView.setError(getString(R.string.error_first_name_required));
            fNameView.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(businessName.getText().toString())) {
            businessName.setError(getString(R.string.error_business_name_required));
            businessName.requestFocus();
            return;
        }
        if (!isValidEmail()) {
            emailView.setError(getString(R.string.error_invalid_email));
            emailView.requestFocus();
            return;
        }
        if (!phoneInput.isValid()) {
            if (phoneInput.getText() == null) {
                showSnackbar(R.string.error_phone_required);
                Snackbar.make(mLayout, R.string.error_phone_required, Snackbar.LENGTH_LONG).show();

            }
            else {
                showSnackbar(R.string.error_phone_invalid);
            }
        }

        updateMerchant();
    }

    private void updateMerchant() {

        progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage(getString(R.string.a_moment));
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        mApiClient.getLoystarApi(false).updateMerchant(
                fNameView.getText().toString(),
                lNameView.getText().toString(),
                emailView.getText().toString(),
                businessName.getText().toString(),
                phoneInput.getNumber(),
                selectedBusinessType,
                selectedCurrency,
                null,
            null,
            null
        ).enqueue(new Callback<MerchantWrapper>() {

            @Override
            public void onResponse(@NonNull Call<MerchantWrapper> call, @NonNull Response<MerchantWrapper> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (response.isSuccessful()) {
                    MerchantWrapper merchantWrapper =  response.body();
                    if (merchantWrapper == null) {
                        showSnackbar(R.string.unknown_error);
                    } else {
                        Merchant merchant = merchantWrapper.getMerchant();
                        merchantEntity.setBusinessName(merchant.getBusiness_name());
                        merchantEntity.setEmail(merchant.getEmail());
                        merchantEntity.setFirstName(merchant.getFirst_name());
                        merchantEntity.setLastName(merchant.getLast_name());
                        merchantEntity.setBusinessType(merchant.getBusiness_type());
                        merchantEntity.setContactNumber(merchant.getContact_number());
                        merchantEntity.setCurrency(merchant.getCurrency());
                        merchantEntity.setSyncFrequency(merchant.getSync_frequency());
                        merchantEntity.setBluetoothPrintEnabled(merchant.getEnable_bluetooth_printing());
                        mDatabaseManager.updateMerchant(merchantEntity);

                        sessionManager.setMerchantSessionData(
                                merchant.getId(),
                                merchant.getEmail(),
                                merchant.getFirst_name(),
                                merchant.getLast_name(),
                                merchant.getContact_number(),
                                merchant.getBusiness_name(),
                                merchant.getBusiness_type(),
                                merchant.getCurrency(),
                                sessionManager.getAccessToken(),
                                sessionManager.getClientKey()
                        );
                        showSnackbar(R.string.profile_update_success);
                        finish();
                    }
                } else if (response.code() == 422) {
                    ObjectMapper mapper = ApiUtils.getObjectMapper(false);
                    try {
                        ResponseBody responseBody = response.errorBody();
                        if (responseBody == null) {
                            showSnackbar(R.string.unknown_error);
                        } else {
                            JsonNode responseObject = mapper.readTree(responseBody.charStream());
                            JSONObject errorObject = new JSONObject(responseObject.toString());
                            JSONObject errors = errorObject.getJSONObject("errors");
                            JSONArray fullMessagesArray = errors.getJSONArray("full_messages");
                            Snackbar.make(mLayout, fullMessagesArray.join(", "), Snackbar.LENGTH_LONG).show();
                        }
                    } catch (IOException | JSONException e) {
                        showSnackbar(R.string.unknown_error);
                        e.printStackTrace();
                    }
                }
                else {
                    showSnackbar(R.string.unknown_error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MerchantWrapper> call, @NonNull Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                showSnackbar(R.string.error_internet_connection_timed_out);
            }
        });
    }

    private boolean isValidEmail() {
        String email = emailView.getText().toString().trim();
        if (email.isEmpty()) {
            return false;
        }
        else if (!TextUtilsHelper.isValidEmailAddress(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            emailView.requestFocus();
            return false;
        }
        return true;
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}