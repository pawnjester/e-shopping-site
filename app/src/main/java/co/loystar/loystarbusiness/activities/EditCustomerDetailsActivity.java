package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.api.ApiUtils;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Customer;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput.InternationalPhoneInput;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditCustomerDetailsActivity extends AppCompatActivity {
    private DatabaseManager mDatabaseManager;
    private EditText userFnameField;
    private EditText userLnameField;
    private EditText userEmailField;
    private String genderSelected;
    private InternationalPhoneInput userPhoneField;
    private View mLayout;
    private CustomerEntity mCustomer;
    private SpinnerButton datePicker;
    private Context mContext;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_customer_details);
        Toolbar toolbar = findViewById(R.id.edit_customer_detail_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mDatabaseManager = DatabaseManager.getInstance(this);
        int customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        mCustomer = mDatabaseManager.getCustomerById(customerId);

        mLayout = findViewById(R.id.edit_customer_detail_wrapper);
        userFnameField = findViewById(R.id.fname);
        userLnameField = findViewById(R.id.lname);
        userEmailField = findViewById(R.id.user_email);
        userPhoneField = findViewById(R.id.edit_customer_phone);

        datePicker = findViewById(R.id.edit_customer_date_of_birth_spinner);
        datePicker.setCalendarView();

        RadioGroup gender_select = findViewById(R.id.gender_select);
        if (gender_select != null) {
            gender_select.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.male:
                            genderSelected = "M";
                            break;
                        case R.id.female:
                            genderSelected = "F";
                            break;
                    }
                }
            });
        }

        if (mCustomer != null) {
            userFnameField.setText(mCustomer.getFirstName());
            userLnameField.setText(mCustomer.getLastName());
            userPhoneField.setNumber(mCustomer.getPhoneNumber());
            userEmailField.setText(mCustomer.getEmail());
            if (mCustomer.getDateOfBirth() != null) {
                datePicker.setDateSelection(mCustomer.getDateOfBirth());
            }
            if (TextUtils.isEmpty(mCustomer.getSex())) {
                String sex = mCustomer.getSex();
                switch (sex) {
                    case "M":
                        if (gender_select != null) {
                            gender_select.check(R.id.male);
                            genderSelected = "M";
                        }
                        break;
                    case "F":
                        if (gender_select != null) {
                            gender_select.check(R.id.female);
                            genderSelected = "F";
                        }
                }
            }

        }
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
        String phoneNumber = userPhoneField.getNumber();
        String fname = userFnameField.getText().toString();
        String lname = userLnameField.getText().toString();
        String email = userEmailField.getText().toString();

        if (TextUtils.isEmpty(fname)) {
            userFnameField.setError(getString(R.string.error_first_name_required));
            userFnameField.requestFocus();
            return;
        }
        if (!userPhoneField.isValid()) {
            if (TextUtils.isEmpty(phoneNumber)) {
                showSnackbar(R.string.error_phone_required);

            }
            else {
                showSnackbar(R.string.error_phone_invalid);
            }
            return;
        }
        if (!validateEmail()) {
            return;
        }

        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        try {
            JSONObject jsonObjectData = new JSONObject();
            JSONObject requestData = new JSONObject();

            jsonObjectData.put("email", email);
            jsonObjectData.put("first_name", fname);
            jsonObjectData.put("last_name", lname);

            jsonObjectData.put("phone_number", phoneNumber);
            if (datePicker.getDateSelection() != null) {
                jsonObjectData.put("date_of_birth", datePicker.getDateSelection());
            }
            jsonObjectData.put("sex", genderSelected);
            requestData.put("data", jsonObjectData);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

            progressDialog = new ProgressDialog(mContext);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.a_moment));
            progressDialog.show();

            ApiClient apiClient = new ApiClient(mContext);
            apiClient.getLoystarApi(false).updateCustomer(mCustomer.getId(), requestBody).enqueue(new Callback<Customer>() {
                @Override
                public void onResponse(Call<Customer> call, Response<Customer> response) {
                    if (response.isSuccessful()) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Customer customer = response.body();
                        mCustomer.setPhoneNumber(customer.getPhone_number());
                        mCustomer.setFirstName(customer.getFirst_name());
                        mCustomer.setLastName(customer.getLast_name());
                        mCustomer.setDateOfBirth(customer.getDate_of_birth());
                        mCustomer.setUpdatedAt(new Timestamp(customer.getUpdated_at().getMillis()));
                        mCustomer.setSex(customer.getSex());
                        mCustomer.setEmail(customer.getEmail());
                        mDatabaseManager.updateCustomer(mCustomer);

                        Intent intent = new Intent(EditCustomerDetailsActivity.this, CustomerListActivity.class);
                        intent.putExtra(Constants.CUSTOMER_UPDATE_SUCCESS, true);
                        intent.putExtra(Constants.CUSTOMER_ID, mCustomer.getId());
                        startActivity(intent);
                    } else if (response.code() == 422) {
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
                    }
                    else {
                        if (response.code() == 401) {
                            SessionManager sessionManager = new SessionManager(mContext);
                            AccountManager accountManager = AccountManager.get(mContext);
                            accountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, sessionManager.getAccessToken());
                        }
                        showSnackbar(R.string.unknown_error);
                    }
                }

                @Override
                public void onFailure(Call<Customer> call, Throwable t) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    showSnackbar(R.string.error_internet_connection_timed_out);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean validateEmail() {
        String email = userEmailField.getText().toString().trim();
        if (email.isEmpty()) {
            return true;
        }
        else if (!TextUtilsHelper.isValidEmailAddress(email)) {
            userEmailField.setError(getString(R.string.error_invalid_email));
            userEmailField.requestFocus();
            return false;
        }
        return true;
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
