package co.loystar.loystarbusiness.activities;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.JsonUtils;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.Buttons.SpinnerButton;
import co.loystar.loystarbusiness.utils.ui.IntlPhoneInput.IntlPhoneInput;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 7/4/17.
 */

public class EditCustomerDetailsActivity extends AppCompatActivity {
    /*constants*/
    public static final int REQUEST_PERMISSION_SETTING = 103;

    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private EditText userFnameField;
    private EditText userLnameField;
    private EditText userEmailField;
    private IntlPhoneInput userPhoneField;
    private DBCustomer customer;
    private String genderSelected;
    private View mLayout;
    private SpinnerButton datePicker;
    private Date dateOfBirth;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_customer_details);
        mLayout = findViewById(R.id.edit_customer_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        Long customerId = getIntent().getLongExtra(CustomerListActivity.CUSTOMER_ID, 0L);
        customer = databaseHelper.getCustomerById(customerId);
        userFnameField = (EditText) findViewById(R.id.fname);
        userLnameField = (EditText) findViewById(R.id.lname);
        userEmailField = (EditText) findViewById(R.id.user_email);
        userPhoneField = (IntlPhoneInput) findViewById(R.id.customer_phone);
        datePicker = (SpinnerButton) findViewById(R.id.activity_edit_customer_details_select_date);

        RadioGroup gender_select = (RadioGroup) findViewById(R.id.gender_select);
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


        final Calendar c = Calendar.getInstance();
        datePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(mContext,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                String dob = dayOfMonth + "/"
                                        + (monthOfYear + 1) + "/" + year;
                                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
                                if (!dob.isEmpty()) {
                                    try {
                                        dateOfBirth = formatter.parse(dob);
                                    } catch (ParseException e) {
                                        //Crashlytics.logException(e);
                                    }
                                }
                                datePicker.setText(dob);

                            }
                        }, mYear, mMonth, mDay);
                assert datePickerDialog.getWindow() != null;
                datePickerDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                datePickerDialog.show();
            }
        });

        if (customer != null) {
            userFnameField.setText(customer.getFirst_name());
            userLnameField.setText(customer.getLast_name());
            userPhoneField.setNumber(customer.getPhone_number());
            userEmailField.setText(customer.getEmail());
            if (customer.getDate_of_birth() != null) {
                datePicker.setText(SyncAdapter.simpleDateFormat.format(customer.getDate_of_birth()));
            }
            if (customer.getSex() != null) {
                if (!customer.getSex().isEmpty()) {
                    String sex = customer.getSex();
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
    }

    private void submitForm() {
        String p_number = userPhoneField.getNumber();
        String fname = userFnameField.getText().toString();
        String lname = userLnameField.getText().toString();
        String email = userEmailField.getText().toString();

        if (TextUtils.isEmpty(fname)) {
            userFnameField.setError(getString(R.string.error_first_name_required));
            userFnameField.requestFocus();
            return;
        }
        if (!userPhoneField.isValid()) {
            if (p_number == null) {
                Snackbar.make(mLayout, R.string.error_phone_required, Snackbar.LENGTH_LONG).show();

            }
            else {
                Snackbar.make(mLayout, R.string.error_phone_invalid, Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        if (!validateEmail()) {
            return;
        }

        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        try {
            JSONObject jsonObjectData = new JSONObject();
            JSONObject requestData = new JSONObject();

            jsonObjectData.put("email", email);
            jsonObjectData.put("first_name", fname);
            jsonObjectData.put("last_name", lname);

            jsonObjectData.put("phone_number", p_number);
            if (dateOfBirth != null) {
                jsonObjectData.put("date_of_birth", dateOfBirth);
            }
            jsonObjectData.put("sex", genderSelected);
            requestData.put("data", jsonObjectData);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

            final ProgressDialog progressDialog = new ProgressDialog(mContext);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.a_moment));
            progressDialog.show();

            ApiClient apiClient = LoystarApplication.getInstance().getApiClient();
            apiClient.getLoystarApi().updateCustomer(customer.getId().toString(), requestBody).enqueue(new Callback<DBCustomer>() {
                @Override
                public void onResponse(Call<DBCustomer> call, Response<DBCustomer> response) {
                    if (response.isSuccessful()) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        databaseHelper.updateCustomer(response.body());

                        Intent intent = new Intent(EditCustomerDetailsActivity.this, CustomerListActivity.class);
                        intent.putExtra(CustomerListActivity.CUSTOMER_UPDATE_SUCCESS, true);
                        intent.putExtra(MerchantBackOffice.CUSTOMER_ID, customer.getId());
                        startActivity(intent);
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
                            Snackbar.make(mLayout, getString(R.string.error_customer_update), Snackbar.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<DBCustomer> call, Throwable t) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Snackbar.make(mLayout, getString(R.string.error_internet_connection), Snackbar.LENGTH_LONG).show();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
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

    private boolean validateEmail() {
        String email = userEmailField.getText().toString().trim();
        if (email.isEmpty()) {
            return true;
        }
        else if (!TextUtilsHelper.isValidEmail(email)) {
            userEmailField.setError(getString(R.string.error_invalid_email));
            userEmailField.requestFocus();
            return false;
        }
        return true;
    }
}
