package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.TimeUtils;
import co.loystar.loystarbusiness.utils.ui.Buttons.AddCustomerButton;
import co.loystar.loystarbusiness.utils.ui.Buttons.SpinnerButton;
import co.loystar.loystarbusiness.utils.ui.IntlPhoneInput.IntlPhoneInput;
import co.loystar.loystarbusiness.utils.ui.MyAlertDialog.MyAlertDialog;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.R.attr.versionCode;
import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

/**
 * Created by ordgen on 7/4/17.
 */

@RuntimePermissions
public class AddNewCustomerActivity extends AppCompatActivity implements DialogInterface.OnClickListener {
    /*Views*/
    private View mLayout;
    private AddCustomerButton addFromContactsBtn;
    private EditText customerLnameView;
    private EditText customerFnameView;
    private IntlPhoneInput customerPhoneView;
    private EditText customerEmailView;
    private SpinnerButton datePicker;
    private SpinnerButton datePickerFromContacts;
    private View addFromContactsBloc;
    private View addCustomerManuallyBloc;
    private EditText customerFNameViewFromContacts;
    private EditText customerLNameViewFromContacts;
    private IntlPhoneInput customerPhoneViewFromContacts;
    private DBCustomer userFromContacts;
    private View dividerBloc;

    /*Constants*/
    private static final int REQUEST_PICK_CONTACT = 10;
    public static final int REQUEST_PERMISSION_SETTING = 102;
    public static final String TAG = AddNewCustomerActivity.class.getSimpleName();
    public static final String CUSTOMER_ID = "customerId";
    public static final String CUSTOMER_NAME = "customerName";

    private Date dateOfBirth;
    private Context mContext;
    private SessionManager sessionManager;
    private String genderSelected = "";
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private MyAlertDialog myAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_customer);
        mLayout = findViewById(R.id.add_new_customer_container_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(AppCompatDrawableManager.get().getDrawable(this, R.drawable.ic_close_white_24px));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        sessionManager = new SessionManager(mContext);
        myAlertDialog = new MyAlertDialog();

        /*Initialize views*/
        addFromContactsBtn = (AddCustomerButton) findViewById(R.id.add_customer_from_contacts);
        addFromContactsBloc = findViewById(R.id.add_from_contacts_bloc);
        customerFnameView = (EditText) findViewById(R.id.fname);
        customerFNameViewFromContacts = (EditText) findViewById(R.id.fname_from_contacts);
        customerLNameViewFromContacts = (EditText) findViewById(R.id.lname_from_contacts);
        customerPhoneViewFromContacts = (IntlPhoneInput) findViewById(R.id.customer_phone_from_contacts);
        customerLnameView = (EditText) findViewById(R.id.lname);
        customerPhoneView = (IntlPhoneInput) findViewById(R.id.customer_phone);
        customerEmailView = (EditText) findViewById(R.id.user_email);
        datePicker = (SpinnerButton) findViewById(R.id.select_date);
        datePickerFromContacts = (SpinnerButton) findViewById(R.id.select_date_1);
        RadioGroup genderSelect = (RadioGroup) findViewById(R.id.gender_select);
        RadioGroup getGenderSelectFromContacts = (RadioGroup) findViewById(R.id.gender_select_1);
        addCustomerManuallyBloc = findViewById(R.id.add_customer_manually_bloc);
        dividerBloc = findViewById(R.id.divider_bloc);

        if (getIntent().hasExtra(RecordDirectSalesActivity.CUSTOMER_PHONE_NUMBER)) {
            customerPhoneView.setNumber(getIntent().getStringExtra(RecordDirectSalesActivity.CUSTOMER_PHONE_NUMBER));
        }

        if (getIntent().hasExtra(CUSTOMER_NAME)) {
            customerFnameView.setText(getIntent().getStringExtra(CUSTOMER_NAME));
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
                                        Log.e(TAG, "DateParseException: " + e.getMessage());
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

        datePickerFromContacts.setOnClickListener(new View.OnClickListener() {
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
                                        Log.e(TAG, "DateParseException: " + e.getMessage());
                                    }
                                }
                                datePickerFromContacts.setText(dob);

                            }
                        }, mYear, mMonth, mDay);
                assert datePickerDialog.getWindow() != null;
                datePickerDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                datePickerDialog.show();
            }
        });

        addFromContactsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddNewCustomerActivityPermissionsDispatcher.pickContactsWithCheck(AddNewCustomerActivity.this);
            }
        });

        genderSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
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

        getGenderSelectFromContacts.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.male_1:
                        genderSelected = "M";
                        break;
                    case R.id.female_1:
                        genderSelected = "F";
                        break;
                }
            }
        });
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
                if (formIsDirty()) {
                    closeKeyBoard();
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(R.string.discard_changes);
                    builder.setMessage(R.string.discard_changes_explain)
                            .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                    builder.show();
                }
                else {
                    finish();
                }
                return true;
            case R.id.action_save:
                if (formIsDirty()) {
                    closeKeyBoard();
                    if (userFromContacts == null) {
                        registerCustomer();
                    }
                    else {
                        registerUserFromContacts();
                    }
                    return true;
                }
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean formIsDirty() {
        String fname = customerFnameView.getText().toString();
        String lname = customerLnameView.getText().toString();
        String email = customerEmailView.getText().toString();
        String phone = customerPhoneView.getNumber();

        return !fname.isEmpty() || !lname.isEmpty() || !email.isEmpty() || !phone.isEmpty() || !genderSelected.isEmpty() || addFromContactsBloc.getVisibility() == View.VISIBLE;
    }

    private void closeKeyBoard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_save).setTitle(getString(R.string.add_caps));
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.READ_CONTACTS)
    protected void pickContacts() {
        Intent contactPickerIntent = new Intent(
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI

        );
        startActivityForResult(contactPickerIntent, REQUEST_PICK_CONTACT);
    }

    @OnShowRationale(Manifest.permission.GET_ACCOUNTS)
    void showRationaleForGetContacts(final PermissionRequest request) {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.permission_contacts_rationale)
                .setPositiveButton(R.string.buttonc_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @OnPermissionDenied(Manifest.permission.GET_ACCOUNTS)
    void showDeniedForGetContacts() {
        Snackbar.make(mLayout, R.string.permission_read_contacts_denied, Snackbar.LENGTH_LONG).show();
    }

    @OnNeverAskAgain(Manifest.permission.GET_ACCOUNTS)
    void showNeverAskForGetContacts() {
        Snackbar.make(mLayout, R.string.permission_read_contacts_neverask,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.buttonc_allow, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_CONTACT) {
            if (resultCode == RESULT_OK) {
                contactPicked(data);
            }
        }
        else if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (resultCode == RESULT_OK) {
                AddNewCustomerActivityPermissionsDispatcher.pickContactsWithCheck(AddNewCustomerActivity.this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AddNewCustomerActivityPermissionsDispatcher.onRequestPermissionsResult(AddNewCustomerActivity.this, requestCode, grantResults);
    }

    private void contactPicked(Intent data) {

        Uri contactData = data.getData();
        String fname = "";
        String lname = "";
        String email = "";
        String phone = "";

        // Cursor loader to query contact name
        CursorLoader clName = new CursorLoader(mContext);
        clName.setProjection(new String[] { ContactsContract.Contacts.DISPLAY_NAME });
        clName.setUri(ContactsContract.Contacts.CONTENT_URI);
        clName.setSelection(ContactsContract.Contacts._ID + " = ?");
        clName.setSelectionArgs(new String[] { contactData.getLastPathSegment() });

        //Cursor loader to query contact phone number
        CursorLoader clPhone = new CursorLoader(mContext);
        clPhone.setProjection(new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER });
        clPhone.setUri(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        clPhone.setSelection(ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?");
        clPhone.setSelectionArgs(new String[] { contactData.getLastPathSegment() });

        // Cursor loader to query optional contact email
        CursorLoader clEmail = new CursorLoader(mContext);
        clEmail.setProjection(new String[] { ContactsContract.CommonDataKinds.Email.DATA });
        clEmail.setUri(ContactsContract.CommonDataKinds.Email.CONTENT_URI);
        clEmail.setSelection(ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?");
        clEmail.setSelectionArgs(new String[] { contactData.getLastPathSegment() });

        // Execute queries and get results from cursors
        Cursor cName = clName.loadInBackground();
        Cursor cPhone = clPhone.loadInBackground();
        Cursor cEmail = clEmail.loadInBackground();

        final int nameIndex = cName.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        final int phoneIndex = cPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        final int emailIndex = cEmail.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);

        if (cName.moveToFirst()) {
            String names = cName.getString(nameIndex);
            if (!names.trim().isEmpty()) {
                String[] namesArray = names.split("\\s+");
                fname = namesArray[0];
                if (namesArray.length > 1) {
                    for (int i=1; i<namesArray.length; i++) {
                        lname += " " + namesArray[i];
                    }
                }
            }
        }

        if (cPhone.moveToFirst()) {
            phone = cPhone.getString(phoneIndex);
        }

        // This only could be true if selected contact has at least one email
        if (cEmail.moveToFirst()) {
            email = cEmail.getString(emailIndex);
        }

        cName.close();
        cPhone.close();
        cEmail.close();

        DBCustomer dbCustomer = new DBCustomer();
        dbCustomer.setEmail(email);
        userFromContacts = dbCustomer;

        addFromContactsBtn.setVisibility(View.GONE);
        addFromContactsBloc.setVisibility(View.VISIBLE);
        addCustomerManuallyBloc.setVisibility(View.GONE);
        dividerBloc.setVisibility(View.GONE);
        dividerBloc.setVisibility(View.GONE);

        customerFNameViewFromContacts.setText(fname);
        customerLNameViewFromContacts.setText(lname);
        customerPhoneViewFromContacts.setNumber(phone);
    }

    private void registerUserFromContacts() {
        if (TextUtils.isEmpty(customerFNameViewFromContacts.getText().toString())) {
            customerFNameViewFromContacts.setError(getString(R.string.error_first_name_required));
            customerFNameViewFromContacts.requestFocus();
            return;
        }
        if (!customerPhoneViewFromContacts.isValid()) {
            if (customerPhoneViewFromContacts.getNumber() == null) {
                Snackbar.make(mLayout, R.string.error_phone_required, Snackbar.LENGTH_LONG).show();
            }
            else {
                Snackbar.make(mLayout, R.string.error_phone_invalid, Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        if (genderSelected.isEmpty()) {
            Snackbar.make(mLayout, R.string.error_gender_required, Snackbar.LENGTH_LONG).show();
            return;
        }

        userFromContacts.setFirst_name(customerFNameViewFromContacts.getText().toString());
        userFromContacts.setLast_name(customerLNameViewFromContacts.getText().toString());
        userFromContacts.setPhone_number(customerPhoneViewFromContacts.getNumber());
        userFromContacts.setSex(genderSelected);

        completeNewCustomerRegistration(userFromContacts);
    }

    private void registerCustomer() {
        String fname = customerFnameView.getText().toString();
        String lname = customerLnameView.getText().toString();
        String email = customerEmailView.getText().toString();
        String phone = customerPhoneView.getNumber();

        if (TextUtils.isEmpty(fname)) {
            customerFnameView.setError(getString(R.string.error_first_name_required));
            customerFnameView.requestFocus();
            return;
        }
        if (!customerPhoneView.isValid()) {
            if (phone == null) {
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
        if (genderSelected.isEmpty()) {
            Snackbar.make(mLayout, R.string.error_gender_required, Snackbar.LENGTH_LONG).show();
            return;
        }

        DBCustomer dbCustomer = new DBCustomer();
        dbCustomer.setFirst_name(fname);
        dbCustomer.setLast_name(lname);
        dbCustomer.setPhone_number(phone);
        dbCustomer.setEmail(email);
        dbCustomer.setSex(genderSelected);

        completeNewCustomerRegistration(dbCustomer);
    }

    private boolean validateEmail() {
        String email = customerEmailView.getText().toString().trim();
        if (!email.isEmpty() && !TextUtilsHelper.isValidEmail(email)) {
            customerEmailView.setError(getString(R.string.error_invalid_email));
            customerEmailView.requestFocus();
            return false;
        }
        return true;
    }

    private void completeNewCustomerRegistration(final DBCustomer customer) {
        DBCustomer oldCustomer = databaseHelper.getCustomerByPhone(
                TextUtilsHelper.trimQuotes(customer.getPhone_number()),
                sessionManager.getMerchantId()
        );

        if (oldCustomer == null) {
            final ProgressDialog progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage(getString(R.string.a_moment));
            progressDialog.setIndeterminate(true);
            progressDialog.show();

            ApiClient mAPiClient = new ApiClient(mContext);

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("email", TextUtilsHelper.trimQuotes(customer.getEmail()));
                jsonObject.put("first_name", TextUtilsHelper.trimQuotes(customer.getFirst_name()));
                jsonObject.put("last_name", TextUtilsHelper.trimQuotes(customer.getLast_name()));
                jsonObject.put("phone_number", TextUtilsHelper.trimQuotes(customer.getPhone_number()));
                jsonObject.put("sex", genderSelected);
                jsonObject.put("merchant_id", String.valueOf(sessionManager.getMerchantId()));
                jsonObject.put("android_app_version_code", versionCode);
                jsonObject.put("local_db_created_at", TimeUtils.getCurrentDateAndTime());
                jsonObject.put("date_of_birth", dateOfBirth);

                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObject);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                mAPiClient.getLoystarApi().addUserDirect(requestBody).enqueue(new Callback<DBCustomer>() {
                    @Override
                    public void onResponse(Call<DBCustomer> call, Response<DBCustomer> response) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        if (response.isSuccessful()) {
                            DBCustomer user = response.body();
                            databaseHelper.insertCustomer(user);

                            Intent intent = new Intent();
                            intent.putExtra(CUSTOMER_ID, user.getId());
                            setResult(RESULT_OK, intent);
                            finish();

                        }
                        else {
                            Snackbar.make(mLayout, getString(R.string.error_add_user), Snackbar.LENGTH_LONG).show();
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
        else {
            myAlertDialog.setTitle("Customer Already Exists");
            myAlertDialog.setMessage("A customer with the name: " + oldCustomer.getFirst_name() + " and phone number: " + customer.getPhone_number() + " already exists. Click 'Ok' to register a different customer.");
            myAlertDialog.setPositiveButton(getString(android.R.string.yes), AddNewCustomerActivity.this);
            myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);

        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case BUTTON_NEGATIVE:
                finish();
                break;
            case BUTTON_POSITIVE:
                myAlertDialog.dismiss();
                break;
        }
    }
}
