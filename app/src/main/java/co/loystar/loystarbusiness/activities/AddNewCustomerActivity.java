package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Date;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Customer;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput.InternationalPhoneInput;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.AddCustomerButton;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;
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


@RuntimePermissions
public class AddNewCustomerActivity extends AppCompatActivity {
    /*Views*/
    private View mLayout;
    private AddCustomerButton addFromContactsBtn;
    private EditText customerLnameView;
    private EditText customerFnameView;
    private InternationalPhoneInput customerPhoneView;
    private EditText customerEmailView;
    private View addFromContactsBloc;
    private View addCustomerManuallyBloc;
    private EditText customerFNameViewFromContacts;
    private EditText customerLNameViewFromContacts;
    private InternationalPhoneInput customerPhoneViewFromContacts;
    private CustomerEntity customerFromContacts;
    private View dividerBloc;

    /*Constants*/
    private static final int REQUEST_PICK_CONTACT = 10;
    public static final int REQUEST_PERMISSION_SETTING = 102;

    private Date dateOfBirth;
    private Context mContext;
    private SessionManager mSessionManager;
    private String genderSelected = "";
    private DatabaseManager mDatabaseManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_customer);
        Toolbar toolbar = findViewById(R.id.add_new_customer_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(AppCompatResources.getDrawable(this, R.drawable.ic_close_white_24px));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mLayout = findViewById(R.id.add_new_customer_wrapper);
        mSessionManager = new SessionManager(this);
        mDatabaseManager = DatabaseManager.getInstance(this);

        /*Initialize views*/
        addFromContactsBtn = findViewById(R.id.add_customer_from_contacts);
        addFromContactsBloc = findViewById(R.id.add_from_contacts_bloc);
        customerFnameView = findViewById(R.id.fname);
        customerFNameViewFromContacts = findViewById(R.id.fname_from_contacts);
        customerLNameViewFromContacts = findViewById(R.id.lname_from_contacts);
        customerPhoneViewFromContacts = findViewById(R.id.customer_phone_from_contacts);
        customerLnameView = findViewById(R.id.lname);
        customerPhoneView = findViewById(R.id.customer_phone);
        customerEmailView = findViewById(R.id.user_email);
        RadioGroup genderSelect = findViewById(R.id.gender_select);
        RadioGroup getGenderSelectFromContacts = findViewById(R.id.gender_select_1);
        addCustomerManuallyBloc = findViewById(R.id.add_customer_manually_bloc);
        dividerBloc = findViewById(R.id.divider_bloc);

        SpinnerButton datePicker = findViewById(R.id.date_of_birth_spinner_2);
        datePicker.setCalendarView();
        SpinnerButton datePickerFromContacts = findViewById(R.id.date_of_birth_spinner);
        datePickerFromContacts.setCalendarView();

        SpinnerButton.OnDatePickedListener onDatePickedListener = new SpinnerButton.OnDatePickedListener() {
            @Override
            public void onDatePicked(Date date) {
                dateOfBirth = date;
            }
        };
        datePicker.setDatePickedListener(onDatePickedListener);

        SpinnerButton.OnDatePickedListener onDatePickedListener1 = new SpinnerButton.OnDatePickedListener() {
            @Override
            public void onDatePicked(Date date) {
                dateOfBirth = date;
            }
        };
        datePickerFromContacts.setDatePickedListener(onDatePickedListener1);

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
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
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
                .setAction(R.string.button_allow, new View.OnClickListener() {
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
                    if (customerFromContacts == null) {
                        registerCustomer();
                    }
                    else {
                        registerCustomerFromContacts();
                    }
                    return true;
                }
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void registerCustomerFromContacts() {
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

        customerFromContacts.setFirstName(customerFNameViewFromContacts.getText().toString());
        customerFromContacts.setLastName(customerLNameViewFromContacts.getText().toString());
        customerFromContacts.setPhoneNumber(customerPhoneViewFromContacts.getNumber());
        customerFromContacts.setSex(genderSelected);

        completeNewCustomerRegistration(customerFromContacts);
    }

    private void completeNewCustomerRegistration(CustomerEntity customerEntity) {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage(getString(R.string.a_moment));
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        ApiClient mAPiClient = new ApiClient(mContext);

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("email", customerEntity.getEmail());
            jsonObject.put("first_name", customerEntity.getFirstName());
            jsonObject.put("last_name", customerEntity.getLastName());
            jsonObject.put("phone_number", customerEntity.getPhoneNumber());
            jsonObject.put("sex", genderSelected);
            jsonObject.put("merchant_id", mSessionManager.getMerchantId());
            jsonObject.put("android_app_version_code", BuildConfig.VERSION_CODE);
            jsonObject.put("date_of_birth", dateOfBirth);

            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObject);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
            mAPiClient.getLoystarApi(false).addUserDirect(requestBody).enqueue(new Callback<Customer>() {
                @Override
                public void onResponse(Call<Customer> call, Response<Customer> response) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (response.isSuccessful()) {
                        Customer customer = response.body();
                        CustomerEntity customerEntity = new CustomerEntity();
                        customerEntity.setId(customer.getId());
                        customerEntity.setEmail(customer.getEmail());
                        customerEntity.setFirstName(customer.getFirst_name());
                        customerEntity.setDeleted(false);
                        customerEntity.setLastName(customer.getLast_name());
                        customerEntity.setSex(customer.getSex());
                        customerEntity.setDateOfBirth(customer.getDate_of_birth());
                        customerEntity.setPhoneNumber(customer.getPhone_number());
                        customerEntity.setUserId(customer.getUser_id());
                        customerEntity.setCreatedAt(new Timestamp(customer.getCreated_at().getMillis()));
                        customerEntity.setUpdatedAt(new Timestamp(customer.getUpdated_at().getMillis()));
                        customerEntity.setOwner(mDatabaseManager.getMerchant(mSessionManager.getMerchantId()));

                        mDatabaseManager.insertNewCustomer(customerEntity);

                        Intent intent = new Intent();
                        intent.putExtra(Constants.CUSTOMER_ID, customer.getId());
                        setResult(RESULT_OK, intent);
                        finish();

                    }
                    else {
                        if (response.code() == 401) {
                            AccountManager accountManager = AccountManager.get(mContext);
                            accountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mSessionManager.getAccessToken());
                        }
                        Snackbar.make(mLayout, getString(R.string.error_add_user), Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Customer> call, @NonNull Throwable t) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Snackbar.make(mLayout, getString(R.string.error_internet_connection_timed_out), Snackbar.LENGTH_LONG).show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
            if (TextUtils.isEmpty(phone)) {
                customerPhoneView.setErrorText(getString(R.string.error_phone_required));
            }
            else {
                customerPhoneView.setErrorText(getString(R.string.error_phone_invalid));
            }
            return;
        }
        if (!customerPhoneView.isUnique()) {
            customerPhoneView.setErrorText(getString(R.string.error_phone_not_unique));
            return;
        }
        if (!validateEmail()) {
            return;
        }
        if (genderSelected.isEmpty()) {
            Snackbar.make(mLayout, R.string.error_gender_required, Snackbar.LENGTH_LONG).show();
            return;
        }

        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setFirstName(fname);
        customerEntity.setLastName(lname);
        customerEntity.setPhoneNumber(phone);
        customerEntity.setEmail(email);
        customerEntity.setSex(genderSelected);

        completeNewCustomerRegistration(customerEntity);
    }

    private boolean validateEmail() {
        String email = customerEmailView.getText().toString().trim();
        if (!email.isEmpty() && !TextUtilsHelper.isValidEmailAddress(email)) {
            customerEmailView.setError(getString(R.string.error_invalid_email));
            customerEmailView.requestFocus();
            return false;
        }
        return true;
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
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_save).setTitle(getString(R.string.add_caps));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AddNewCustomerActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    private void contactPicked(Intent data) {

        Uri contactData = data.getData();
        String fname = "";
        String lname = "";
        String email = "";
        String phone = "";

        // Cursor loader to query contact name
        CursorLoader clName = new CursorLoader(mContext);
        clName.setProjection(new String[]{ContactsContract.Contacts.DISPLAY_NAME});
        clName.setUri(ContactsContract.Contacts.CONTENT_URI);
        clName.setSelection(ContactsContract.Contacts._ID + " = ?");
        assert contactData != null;
        clName.setSelectionArgs(new String[]{contactData.getLastPathSegment()});

        //Cursor loader to query contact phone number
        CursorLoader clPhone = new CursorLoader(mContext);
        clPhone.setProjection(new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER});
        clPhone.setUri(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        clPhone.setSelection(ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?");
        clPhone.setSelectionArgs(new String[]{contactData.getLastPathSegment()});

        // Cursor loader to query optional contact email
        CursorLoader clEmail = new CursorLoader(mContext);
        clEmail.setProjection(new String[]{ContactsContract.CommonDataKinds.Email.DATA});
        clEmail.setUri(ContactsContract.CommonDataKinds.Email.CONTENT_URI);
        clEmail.setSelection(ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?");
        clEmail.setSelectionArgs(new String[]{contactData.getLastPathSegment()});

        // Execute queries and get results from cursors
        Cursor cName = clName.loadInBackground();
        Cursor cPhone = clPhone.loadInBackground();
        Cursor cEmail = clEmail.loadInBackground();

        final int nameIndex = cName.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        final int phoneIndex = cPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        final int emailIndex = cEmail.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);

        if (cName.moveToFirst()) {
            StringBuilder stringBuilder = new StringBuilder();
            String names = cName.getString(nameIndex);
            if (!names.trim().isEmpty()) {
                String[] namesArray = names.split("\\s+");
                fname = namesArray[0];
                if (namesArray.length > 1) {
                    for (int i = 1; i < namesArray.length; i++) {
                        stringBuilder.append(" ").append(namesArray[i]);
                    }
                    lname = stringBuilder.toString();
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

        CustomerEntity dbCustomer = new CustomerEntity();
        dbCustomer.setEmail(email);
        customerFromContacts = dbCustomer;

        addFromContactsBtn.setVisibility(View.GONE);
        addFromContactsBloc.setVisibility(View.VISIBLE);
        addCustomerManuallyBloc.setVisibility(View.GONE);
        dividerBloc.setVisibility(View.GONE);
        dividerBloc.setVisibility(View.GONE);

        customerFNameViewFromContacts.setText(fname);
        customerLNameViewFromContacts.setText(lname);
        customerPhoneViewFromContacts.setNumber(phone);
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
}
