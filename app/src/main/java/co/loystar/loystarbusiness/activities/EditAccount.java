package co.loystar.loystarbusiness.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.JsonUtils;
import co.loystar.loystarbusiness.api.pojos.MerchantUpdateSuccessResponse;
import co.loystar.loystarbusiness.models.BusinessType;
import co.loystar.loystarbusiness.models.BusinessTypesFetcher;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput.CurrencySearchableSpinner;
import co.loystar.loystarbusiness.utils.ui.IntlPhoneInput.IntlPhoneInput;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialog;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogEntity;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogOnItemSelectedListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditAccount extends AppCompatActivity implements SingleChoiceSpinnerDialogOnItemSelectedListener {

    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private EditText fNameView;
    private EditText lNameView;
    private EditText emailView;
    private EditText businessName;
    private CurrencySearchableSpinner currencyView;
    private IntlPhoneInput phoneInput;
    private View mLayout;
    private String businessTypeSelectedItem;
    private ProgressDialog progressDialog;
    private ApiClient mApiClient = LoystarApplication.getInstance().getApiClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);
        mLayout = findViewById(R.id.editAccountLayout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        DBMerchant merchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());

        fNameView = (EditText) findViewById(R.id.firstName);
        lNameView = (EditText) findViewById(R.id.lastName);
        emailView = (EditText) findViewById(R.id.email);
        businessName = (EditText) findViewById(R.id.businessName);
        currencyView = (CurrencySearchableSpinner) findViewById(R.id.currency_spinner);
        phoneInput = (IntlPhoneInput) findViewById(R.id.phone_number);

        fNameView.setText(merchant.getFirst_name());
        lNameView.setText(merchant.getLast_name());
        emailView.setText(merchant.getEmail());
        businessName.setText(merchant.getBusiness_name());
        businessTypeSelectedItem = merchant.getBusiness_type();

        SingleChoiceSpinnerDialog businessTypeSpinner = (SingleChoiceSpinnerDialog) findViewById(R.id.business_type_spinner);
        ArrayList<BusinessType> businessTypes = BusinessTypesFetcher.getBusinessTypes(this);
        ArrayList<SingleChoiceSpinnerDialogEntity> spinnerItems = new ArrayList<>();
        for (BusinessType businessType: businessTypes) {
            SingleChoiceSpinnerDialogEntity entity = new SingleChoiceSpinnerDialogEntity(businessType.getTitle(), (long) businessType.getId());
            spinnerItems.add(entity);
        }

        Long selectedBizCategoryId = null;
        BusinessType selectedBusinessType = BusinessTypesFetcher.getBusinessTypes(this).getBusinessTypeByTitle(businessTypeSelectedItem);
        if (selectedBusinessType != null) {
            selectedBizCategoryId = (long) BusinessTypesFetcher.getBusinessTypes(this).getBusinessTypeByTitle(businessTypeSelectedItem).getId();
        }

        businessTypeSpinner.setItems(spinnerItems, "Select One", this, "Select Category", selectedBizCategoryId, null, "", "");

        currencyView.setCurrency(merchant.getCurrency());
        phoneInput.setNumber(merchant.getContact_number());


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
                Snackbar.make(mLayout, R.string.error_phone_required, Snackbar.LENGTH_LONG).show();

            }
            else {
                Snackbar.make(mLayout, R.string.error_phone_invalid, Snackbar.LENGTH_LONG).show();
            }
        }

        updateMerchant();
    }

    private void updateMerchant() {

        progressDialog = new ProgressDialog(EditAccount.this);
        progressDialog.setMessage(getString(R.string.a_moment));
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        mApiClient.getLoystarApi().updateMerchant(
                fNameView.getText().toString(),
                lNameView.getText().toString(),
                emailView.getText().toString(),
                businessName.getText().toString(),
                phoneInput.getNumber(),
                businessTypeSelectedItem,
                currencyView.getCurrencyCode(), null).enqueue(new Callback<MerchantUpdateSuccessResponse>() {

            @Override
            public void onResponse(Call<MerchantUpdateSuccessResponse> call, Response<MerchantUpdateSuccessResponse> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (response.isSuccessful()) {
                    MerchantUpdateSuccessResponse merchantUpdateSuccessResponse = response.body();
                    DBMerchant merchant = merchantUpdateSuccessResponse.getData();
                    databaseHelper.updateMerchant(merchant);

                    sessionManager.setMerchantSessionData(
                            merchant.getBusiness_name(),
                            merchant.getEmail(),
                            merchant.getId().toString(),
                            merchant.getCurrency(),
                            merchant.getFirst_name(),
                            merchant.getLast_name(),
                            merchant.getBusiness_type(),
                            merchant.getContact_number(),
                            sessionManager.getAccessToken(),
                            sessionManager.getClientKey(),
                            sessionManager.getKeyTokenExpiry()
                    );

                    Toast.makeText(EditAccount.this, getString(R.string.merchant_update_success), Toast.LENGTH_LONG).show();
                    finish();
                }
                else {
                    if (response.code() == 422) {
                        ObjectMapper mapper = JsonUtils.objectMapper;
                        try {
                            JsonNode responseObject = mapper.readTree(response.errorBody().charStream());
                            JSONObject errorObject = new JSONObject(responseObject.toString());
                            JSONObject errors = errorObject.getJSONObject("errors");
                            JSONArray fullMessagesArray = errors.getJSONArray("full_messages");
                            Snackbar.make(mLayout, fullMessagesArray.join(", "), Snackbar.LENGTH_LONG).show();
                        } catch (IOException | JSONException e) {
                            //Crashlytics.logException(e);
                            e.printStackTrace();
                        }
                    }
                    else {
                        Snackbar.make(mLayout, getString(R.string.error_merchant_update), Snackbar.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<MerchantUpdateSuccessResponse> call, Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Snackbar.make(mLayout, getString(R.string.error_internet_connection), Snackbar.LENGTH_LONG).show();
            }
        });
    }



    private boolean isValidEmail() {
        String email = emailView.getText().toString().trim();
        if (email.isEmpty()) {
            return false;
        }
        else if (!TextUtilsHelper.isValidEmail(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            emailView.requestFocus();
            return false;
        }
        return true;
    }

    @Override
    public void itemSelectedId(Long Id) {
        BusinessType businessType = BusinessTypesFetcher.getBusinessTypes(EditAccount.this).getBusinessTypeById(Integer.parseInt(String.valueOf(Id)));
        if (businessType != null) {
            businessTypeSelectedItem = businessType.getTitle();
        }
    }
}
