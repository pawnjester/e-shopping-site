package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.reactivestreams.Subscriber;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;

import co.loystar.loystarbusiness.models.databinders.DownloadInvoice;
import co.loystar.loystarbusiness.models.databinders.Invoice;
import co.loystar.loystarbusiness.models.databinders.InvoicePaymentHistoriesItem;
import co.loystar.loystarbusiness.models.databinders.ItemsItem;
import co.loystar.loystarbusiness.models.databinders.Product;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceHistoryEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceTransactionEntity;
import co.loystar.loystarbusiness.models.entities.ItemsItemEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.Merchant;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.BackgroundNotificationService;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.buttons.GreenButton;
import co.loystar.loystarbusiness.utils.ui.dialogs.CustomerAutoCompleteDialog;
import co.loystar.loystarbusiness.utils.ui.dialogs.SendOptionsDialog;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.query.Tuple;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class InvoicePayActivity extends BaseActivity implements
        CustomerAutoCompleteDialog.SelectedCustomerListener,
        SendOptionsDialog.SendOptionsDialogClickListener{

    private String payment_option;
    private String due_date;
    private DatabaseManager databaseManager;
    private Toolbar toolbar;
    private SendOptionsDialog sendOptionsDialog;

    private RadioGroup paymentRadioGroup;
    private EditText amountText;
    private EditText due_date_picker;
    Button completePaymentButton;
    private ReactiveEntityStore<Persistable> mDataStore;
    private CustomerEntity mSelectedCustomer;
    private MutableLiveData<CustomerEntity> customerLive  = new MutableLiveData<>();
    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    int customerId;
    private ArrayList<Integer> mSelectedProducts;
    private double totalCost;
    private TextView totalAmount;
    private int invoiceId;
    private ApiClient mApiClient;
    private DatabaseManager mDatabaseManager;
    private CustomerAutoCompleteDialog customerAutoCompleteDialog;
    private String paidAmount;
    private TextView amount_due_value;
    private String invoice_payment_method;
    private String invoiceStatus;
    private TextView paymentMessage;
    public static final String PROGRESS_UPDATE = "progress_update";
    private TextView selectedCustomer;
    private String invoiceNumber;
    private TextView recordPaymentText;
    private TextView payment_text;
    private View mLayout;
    private TextView amount_due_header;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    File invoiceFile;
    String strippedFile;


    HashMap<Integer, Integer> mSelectedProductHash = new HashMap<>();


    @Override
    protected void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_pay);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.pay_with_invoice));
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setDisplayShowHomeEnabled(true);
        mLayout = findViewById(R.id.invoice_wrapper);
        amount_due_header = findViewById(R.id.amount_due_text);
        mDataStore = DatabaseManager.getDataStore(this);
        recordPaymentText = findViewById(R.id.recored_payment_text);
        payment_text = findViewById(R.id.payment_text);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class,
                mSessionManager.getMerchantId()).blockingGet();
        customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        selectedCustomer = findViewById(R.id.selected_customer);
        totalCost = getIntent().getDoubleExtra(Constants.CHARGE, 0);
        invoice_payment_method = getIntent().getStringExtra(Constants.PAYMENT_METHOD);
        mSelectedProducts = getIntent().getExtras().getIntegerArrayList(Constants.SELECTED_PRODUCTS);
        mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, customerId).blockingGet();
        invoiceId = getIntent().getIntExtra(Constants.INVOICE_ID, 0);
        invoiceNumber = getIntent().getStringExtra(Constants.INVOICE_NUMBER);
        paymentRadioGroup = findViewById(R.id.payment_radio_group);
        completePaymentButton = findViewById(R.id.completePayment);
        amountText = findViewById(R.id.record_payment);
        databaseManager = DatabaseManager.getInstance(this);
        due_date_picker = findViewById(R.id.due_date_picker);
        paymentMessage = findViewById(R.id.payment_message);
        totalAmount = findViewById(R.id.total_amount);
        totalAmount.setText(String.valueOf(totalCost));
        mApiClient = new ApiClient(this);
        mDatabaseManager = DatabaseManager.getInstance(this);
        amount_due_value = findViewById(R.id.amount_due_value);
        mSelectedProductHash = (HashMap<Integer, Integer>) getIntent().getSerializableExtra(Constants.HASH_MAP);

        invoiceStatus = getIntent().getStringExtra(Constants.STATUS);
        sendOptionsDialog = SendOptionsDialog.newInstance();
        sendOptionsDialog.setListener(this);



        customerAutoCompleteDialog = CustomerAutoCompleteDialog.newInstance(getString(R.string.invoice_owner));
        customerAutoCompleteDialog.setSelectedCustomerListener(this);
        paidAmount = getIntent().getStringExtra(Constants.PAID_AMOUNT);

        paymentRadioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            RadioButton rb = radioGroup.findViewById(i);
            payment_option = rb.getText().toString();
        });
        completePaymentButton.setOnClickListener(view -> {
            if (mSelectedCustomer != null) {
                createInvoice();
            } else {
                Snackbar.make(mLayout, "Please select a customer", Snackbar.LENGTH_SHORT).show();
                selectedCustomer.setBackgroundColor(getResources().getColor(R.color.orange));
            }
        });
        SimpleDateFormat dateFormat =  new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String time = dateFormat.format(cal.getTime());
        due_date_picker.setText(time);
        due_date_picker.setOnClickListener(view -> getDateDialog());

        customerLive.observeForever(customerEntity -> {
            selectedCustomer.setText(customerEntity.getFirstName());
        });


        if (mSelectedCustomer == null) {
            customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
            selectedCustomer.setText("Click to select a customer");
        } else {
            selectedCustomer.setText(mSelectedCustomer.getFirstName());
        }

        if (paidAmount != null ) {
            Log.e(">>>>", paidAmount );
            amountText.setText(paidAmount);
        }
//        else {
//            paidAmount = "0.0";
//        }

        if (invoiceId > 0) {
            completePaymentButton.setText("Update");
            selectedCustomer.setEnabled(false);
            if (invoiceStatus != null && invoiceStatus.equals("paid")) {
                amount_due_value.setText(getResources().getString(R.string.paid_text));
                amountText.setEnabled(false);
                due_date_picker.setEnabled(false);
                paymentMessage.setEnabled(false);
                selectedCustomer.setEnabled(false);
                completePaymentButton.setEnabled(false);
                completePaymentButton.setBackgroundColor(getResources().getColor(R.color.light_grey));
            } else if (invoiceStatus != null && invoiceStatus.equals("unpaid") || invoiceStatus.equals("partial")) {
                Double difference;
                difference = totalCost - Double.valueOf(paidAmount == null ? "0.0" : paidAmount);
//                        Double.valueOf(
//                                amountText.getText().toString().length() == 0 ? "0.0" : amountText.getText().toString());
                amount_due_value.setText(difference.toString());
            }
        } else {
            amountText.setVisibility(View.GONE);
            recordPaymentText.setVisibility(View.GONE);
            payment_text.setVisibility(View.GONE);
            paymentRadioGroup.setVisibility(View.GONE);
            amount_due_header.setVisibility(View.GONE);
            amount_due_value.setVisibility(View.GONE);
        }

        registerReceiver();
        selectedCustomer.setOnClickListener(view -> {
            customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
        });
        if(isOnline(this)) {
            getInvoiceMessage();
        }
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.layout_loading_dialog);
        dialog = builder.create();
    }

    private void registerReceiver() {
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PROGRESS_UPDATE);
        bManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void startInvoiceDownload(int invoiceId) {
        Intent intent = new Intent(this, BackgroundNotificationService.class);
        intent.putExtra(Constants.INVOICE_ID, invoiceId);
        intent.putExtra(Constants.INVOICE_NUMBER, invoiceNumber);
        startService(intent);
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PROGRESS_UPDATE)) {
                boolean downloadComplete = intent.getBooleanExtra("downloadComplete", false);
                if (downloadComplete ) {
                    Toast.makeText(getApplicationContext(), "File download completed", Toast.LENGTH_SHORT).show();
                    Intent merchantIntent = new Intent(getApplicationContext(), InvoiceListActivity.class);
                    startActivity(merchantIntent);
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(createInvoiceStarted, new IntentFilter(Constants.CREATE_INVOICE_STARTED));
        registerReceiver(createInvoiceEnded, new IntentFilter(Constants.CREATE_INVOICE_ENDED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            unregisterReceiver(createInvoiceStarted);
            unregisterReceiver(createInvoiceEnded);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver createInvoiceStarted = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dialog.show();
        }
    };
    private BroadcastReceiver createInvoiceEnded = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dialog.dismiss();
            String ress = intent.getStringExtra("responseNumber");
            if (invoiceNumber == null) {
                invoiceNumber = ress;
            }
            if (intent.getStringExtra("errorbody") != null) {
                String error = intent.getStringExtra("errorbody");
                Snackbar.make(mLayout, error , Snackbar.LENGTH_LONG)
                        .setAction("Upgrade", view -> {
                    Intent merchantIntent = new Intent(context,
                            PaySubscriptionActivity.class);
                    startActivity(merchantIntent);
                }).show();
            } else if (intent.getIntExtra("responseCode", 0) != 0) {
                Snackbar.make(
                        mLayout,
                        "Invoice could not be created",
                        Snackbar.LENGTH_LONG).show();
            }
            else {
                showChoiceDialog();
            }
        }
    };

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_share_invoice:
                if (isOnline(this)) {
                    sendInvoiceToCustomer(invoiceId);
                } else {
                    Snackbar.make(mLayout,
                            "Please connect to the internet", Snackbar.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_delete:
                deleteInvoice(invoiceId);
                return true;
            case R.id.action_share_to_whatsapp:
                if (isOnline(this)) {
                    downloadPdf(invoiceId);
                } else {
                    Snackbar.make(mLayout,
                            "Please connect to the internet", Snackbar.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_download_pdf:
                if (isOnline(this)) {
                    if (isStoragePermissionGranted()) {
                        completePaymentButton.setEnabled(false);
                        startInvoiceDownload(invoiceId);
                    }
                } else {
                    Snackbar.make(mLayout,
                            "Please connect to the internet", Snackbar.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        MenuItem shareItem = menu.findItem(R.id.action_share_invoice);
        MenuItem shareItemWhatsapp = menu.findItem(R.id.action_share_to_whatsapp);
        MenuItem downloadPdfmenu = menu.findItem(R.id.action_download_pdf);
        if (invoiceId <= 0) {
            deleteItem.setEnabled(false);
            shareItem.setEnabled(false);
            shareItemWhatsapp.setEnabled(false);
            downloadPdfmenu.setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void sendInvoiceToCustomer(int invoiceId) {
        dialog.setMessage("Sending Invoice to Customer Email");
        dialog.show();
        mApiClient.getLoystarApi(false)
                .sendInvoiceToCustomer(invoiceId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.code() == 200) {
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(),
                                    "Invoice sent to the email",
                                    Toast.LENGTH_SHORT).show();
                            Intent merchantIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                            startActivity(merchantIntent);
                        }else {
                            dialog.dismiss();
                            try {
                                JSONObject jObjError = new JSONObject(response.errorBody().string());
                                String error = jObjError.getString("message");
                                Snackbar.make(mLayout, error , Snackbar.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Timber.e(e.getLocalizedMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        dialog.dismiss();
                    }
                });
    }

    @SuppressLint("SimpleDateFormat")
    private void getDateDialog() {
        final Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (datePicker, year, monthOfYear, dayOfMonth) -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, monthOfYear);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String formattedDate = format.format(calendar.getTime());
                    due_date = formattedDate;
                    due_date_picker.setText(due_date);
                }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();

    }

    private void deleteInvoice(int id) {
        dialog.setTitle("Deleting invoice");
        dialog.show();
        InvoiceEntity invoiceEntity = databaseManager.getInvoiceById(id);
        mApiClient
                .getLoystarApi(false)
                .deleteInvoice(id)
                .enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    mDataStore.delete(invoiceEntity).subscribe(/* no-op */);
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(),
                            "Invoice Deleted", Toast.LENGTH_SHORT).show();
                    Intent merchantIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                    startActivity(merchantIntent);
                    SyncAdapter.performSync(getApplicationContext(), mSessionManager.getEmail());
                }else {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(),
                            "Invoice could not be deleted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(getApplicationContext(),
                        "Invoice could not be deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateInvoice(int id) {
        dialog.setTitle("Updating invoice payment");
        dialog.show();
        if (amountText.getText().length() < 1) {
            amountText.setText("0.0");
        } else if (paymentMessage.getText().length() < 1) {
            paymentMessage.setText("");
        } else if (payment_option == null) {
            payment_option = "";
        }
        InvoiceEntity invoiceEntity = databaseManager.getInvoiceById(id);
        invoiceEntity.setUpdatedAt(new Timestamp(new DateTime().getMillis()));
        invoiceEntity.setPaymentMethod(payment_option);
        invoiceEntity.setPaidAmount(amountText.getText().toString());
        invoiceEntity.setSynced(true);
        invoiceEntity.setOwner(merchantEntity);
        invoiceEntity.setCustomer(mSelectedCustomer);
        invoiceEntity.setDueDate(due_date);
        invoiceEntity.setPaymentMessage(paymentMessage.getText().toString());

        mDataStore.update(invoiceEntity).subscribe(updatedInvoiceEntity -> {
            try{

                JSONObject jsonObjectData = new JSONObject();
                if (paidAmount == null) {
                    paidAmount ="0.0";
                }
                double total  = Double.valueOf(
                        updatedInvoiceEntity.getPaidAmount()) + Double.valueOf(paidAmount);
                if (total
                        >= Double.valueOf(updatedInvoiceEntity.getAmount())) {
                    jsonObjectData.put("paid_amount", updatedInvoiceEntity.getPaidAmount());
                    jsonObjectData.put("status", "paid");
                }
                else if ( total
                        < Double.valueOf(updatedInvoiceEntity.getAmount())) {
                    jsonObjectData.put("paid_amount", updatedInvoiceEntity.getPaidAmount());
                    jsonObjectData.put("status", "partial");
                }else {
                    jsonObjectData.put("paid_amount", updatedInvoiceEntity.getPaidAmount());
                    jsonObjectData.put("status", "unpaid");
                }

                jsonObjectData.put("payment_method", updatedInvoiceEntity.getPaymentMethod());
                jsonObjectData.put("payment_message", updatedInvoiceEntity.getPaymentMessage());

                JSONArray jsonArray = new JSONArray();
                for (InvoiceTransactionEntity transactionEntity: updatedInvoiceEntity.getTransactions()) {
                    LoyaltyProgramEntity programEntity =
                            mDatabaseManager.getLoyaltyProgramById(
                                    transactionEntity.getMerchantLoyaltyProgramId());
                    if (programEntity != null) {
                        JSONObject jsonObject = new JSONObject();

                        if (transactionEntity.getUserId() > 0) {
                            jsonObject.put("user_id", transactionEntity.getUserId());
                        }
                        jsonObject.put("merchant_id", merchantEntity.getId());
                        jsonObject.put("amount", transactionEntity.getAmount());

                        if (transactionEntity.getProductId() > 0) {
                            jsonObject.put("id", transactionEntity.getProductId());
                        }
                        jsonObject.put("merchant_loyalty_program_id", transactionEntity.getMerchantLoyaltyProgramId());
                        jsonObject.put("program_type", transactionEntity.getProgramType());

                        if (programEntity.getProgramType().equals(getString(R.string.simple_points))) {
                            jsonObject.put("points", transactionEntity.getPoints());
                        }
                        else if (programEntity.getProgramType().equals(getString(R.string.stamps_program))) {
                            jsonObject.put("stamps", transactionEntity.getStamps());
                        }
                        jsonArray.put(jsonObject);
                    }
                }

                jsonObjectData.put("items", jsonArray);
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);
                Log.e("reques", requestData + "");

                RequestBody requestBody = RequestBody
                        .create(MediaType.parse(
                                "application/json; charset=utf-8"), requestData.toString());
                mApiClient.getLoystarApi(false)
                        .updateInvoice(
                        updatedInvoiceEntity.getId(), requestBody).enqueue(new Callback<Invoice>() {
                    @Override
                    public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(),
                                    "Invoice has been updated successfully", Toast.LENGTH_SHORT).show();
                            Invoice invoice = response.body();
                            updatedInvoiceEntity.setUpdatedAt(new Timestamp(new DateTime().getMillis()));
                            updatedInvoiceEntity.setPaymentMethod(payment_option);
                            updatedInvoiceEntity.setPaidAmount(invoice.getPaidAmount());
                            updatedInvoiceEntity.setSynced(true);
                            updatedInvoiceEntity.setStatus(invoice.getStatus());
                            updatedInvoiceEntity.setOwner(merchantEntity);
                            updatedInvoiceEntity.setCustomer(mSelectedCustomer);
                            updatedInvoiceEntity.setPaymentMessage(invoice.getPaymentMessage());
                            mDataStore.upsert(updatedInvoiceEntity).subscribe( up -> {
                                List<InvoicePaymentHistoriesItem> item = invoice.getInvoicePaymentHistories();
                                    for (InvoicePaymentHistoriesItem entity: item) {
                                        InvoiceHistoryEntity historyEntity = new InvoiceHistoryEntity();
                                        historyEntity.setId(entity.getId());
                                        historyEntity.setInvoice(updatedInvoiceEntity);
                                        historyEntity.setPaidAmount(entity.getPaidAmount());
                                        historyEntity.setPaidAt(entity.getPaidAt());
                                        mDataStore.upsert(historyEntity).subscribe();
                                    }
                                    }
                            );
                            SyncAdapter.performSync(getApplicationContext(), mSessionManager.getEmail());
                            Intent nextIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                            startActivity(nextIntent);

                        }else {
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(),
                                    "Invoice could not be updated", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Invoice> call, Throwable t) {

                    }
                });
            } catch (JSONException e) {
                Timber.e(e);
            }
        });
    }

    private void getInvoiceMessage() {
        mApiClient.getLoystarApi(false)
                .getPaymentMessage()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    paymentMessage.setText(response.getMessage());
                });
    }


    private void createInvoice() {
        dialog.show();
        if (invoiceId > 0) {
            updateInvoice(invoiceId);
        }
        else {
        Integer lastInvoiceId = databaseManager.getLastInvoiceRecordId();

        InvoiceEntity newInvoiceEntity = new InvoiceEntity();
        if (lastInvoiceId == null) {
            newInvoiceEntity.setId(1);
        } else {
            newInvoiceEntity.setId(lastInvoiceId + 1);
        }

            newInvoiceEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
            newInvoiceEntity.setSynced(false);
            newInvoiceEntity.setCustomer(mSelectedCustomer);
            newInvoiceEntity.setOwner(merchantEntity);
            newInvoiceEntity.setDueDate(due_date_picker.getText().toString());
            newInvoiceEntity.setAmount(String.valueOf(totalCost));
            newInvoiceEntity.setPaymentMessage(paymentMessage.getText().toString());
            mDataStore.upsert(newInvoiceEntity).subscribe(invoiceEntity -> {
                Result<ProductEntity> result = mDataStore.select(ProductEntity.class)
                        .where(ProductEntity.ID.in(mSelectedProducts))
                        .orderBy(ProductEntity.UPDATED_AT.desc())
                        .get();
                List<ProductEntity> productEntities = result.toList();
                Integer lastTransactionId = databaseManager.getLastInvoiceTransactionRecordId();
                ArrayList<Integer> newTransactionIds = new ArrayList<>();
                for (int x = 0; x < productEntities.size(); x++) {
                    if (lastTransactionId == null) {
                        newTransactionIds.add(x, x + 1);
                    } else {
                        newTransactionIds.add(x, (lastTransactionId + x + 1));
                    }
                }


                for (int i = 0; i < productEntities.size(); i++) {
                    ProductEntity product = productEntities.get(i);
                    LoyaltyProgramEntity loyaltyProgram = product.getLoyaltyProgram();


                    if (loyaltyProgram != null) {
                        InvoiceTransactionEntity transactionEntity = new InvoiceTransactionEntity();
                        transactionEntity.setId(newTransactionIds.get(i));
                        transactionEntity.setMerchantLoyaltyProgramId(loyaltyProgram.getId());

                        String template = "%.2f";
                        double tc = product.getPrice() * mSelectedProductHash.get(product.getId());
                        double totalCosts = Double.valueOf(String.format(Locale.UK, template, tc));
                        transactionEntity.setAmount(totalCosts);

                        if (loyaltyProgram.getProgramType().equals(getString(R.string.simple_points))) {
                            transactionEntity
                                    .setPoints(Double.valueOf(totalCost).intValue());
                            transactionEntity.setProgramType(getString(R.string.simple_points));
                        } else if (loyaltyProgram.getProgramType().equals(getString(R.string.stamps_program))) {
                            int stampsEarned = mSelectedProducts.get(product.getId());
                            transactionEntity.setStamps(stampsEarned);
                            transactionEntity.setProgramType(getString(R.string.stamps_program));
                        }
                        transactionEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
                        transactionEntity.setProductId(product.getId());
                        if (mSelectedCustomer != null) {
                            transactionEntity.setUserId(mSelectedCustomer.getUserId());
                            transactionEntity.setCustomer(mSelectedCustomer)
                            ;
                        }
                        transactionEntity.setSynced(false);
                        transactionEntity.setMerchant(merchantEntity);
                        transactionEntity.setInvoice(invoiceEntity);
                        mDataStore.upsert(transactionEntity).subscribe(/*no-op*/);

                        if (i + 1 == productEntities.size()) {
                            Completable.complete()
                                    .delay(1, TimeUnit.SECONDS)
                                    .compose(bindToLifecycle())
                                    .doOnComplete(() -> {
                                        if (isOnline(this)) {
                                            uploadNewinvoices();
                                            SyncAdapter.performSync(this, mSessionManager.getEmail());
                                        } else {
                                            dialog.dismiss();
                                            SyncAdapter.performSync(this, mSessionManager.getEmail());
                                            Intent merchantIntent = new Intent(this, MerchantBackOfficeActivity.class);
                                            startActivity(merchantIntent);
                                        }
                                    }).subscribe();
                        }
                    }
                }

            });

    }
    }

    public boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }

    private void uploadNewinvoices() {
        for(InvoiceEntity invoiceEntity: mDatabaseManager.getUnsyncedInvoiceEntities(merchantEntity)) {
            try {
                JSONObject jsonObjectData = new JSONObject();
                if (invoiceEntity.getCustomer() != null) {
                    jsonObjectData.put("user_id", invoiceEntity.getCustomer().getUserId());

                }
                if (invoiceEntity.getStatus() != null) {
                    jsonObjectData.put("status", invoiceEntity.getStatus());
                }
                if (invoiceEntity.getPaymentMethod() != null) {
                    jsonObjectData.put("payment_method", invoiceEntity.getPaymentMethod());
                }
                if (invoiceEntity.getPaymentMessage() != null) {
                    jsonObjectData.put("payment_message", invoiceEntity.getPaymentMessage());
                }
                jsonObjectData.put("due_date", invoiceEntity.getDueDate());
                if (invoiceEntity.getPaidAmount() != null) {
                    if (Double.valueOf(invoiceEntity.getPaidAmount()) >= Double.valueOf(invoiceEntity.getAmount())) {
                        jsonObjectData.put("paid_amount", invoiceEntity.getPaidAmount());
                        jsonObjectData.put("status", "paid");
                    } else if ( Double.valueOf(invoiceEntity.getPaidAmount()) < Double.valueOf(invoiceEntity.getAmount())) {
                        jsonObjectData.put("paid_amount", invoiceEntity.getPaidAmount());
                        jsonObjectData.put("status", "partial");
                    }else {
                        jsonObjectData.put("paid_amount", invoiceEntity.getPaidAmount());
                        jsonObjectData.put("status", "unpaid");
                    }
                }

                JSONArray jsonArray = new JSONArray();
                for (InvoiceTransactionEntity transactionEntity: invoiceEntity.getTransactions()) {
                    LoyaltyProgramEntity programEntity =
                            mDatabaseManager.getLoyaltyProgramById(
                                    transactionEntity.getMerchantLoyaltyProgramId());
                    if (programEntity != null) {
                        JSONObject jsonObject = new JSONObject();

                        if (transactionEntity.getUserId() > 0) {
                            jsonObject.put("user_id", transactionEntity.getUserId());
                        }
                        jsonObject.put("merchant_id", merchantEntity.getId());
                        jsonObject.put("amount", transactionEntity.getAmount());

                        if (transactionEntity.getProductId() > 0) {
                            jsonObject.put("product_id", transactionEntity.getProductId());
                        }
                        jsonObject.put("merchant_loyalty_program_id",
                                transactionEntity.getMerchantLoyaltyProgramId());
                        jsonObject.put("program_type", transactionEntity.getProgramType());

                        if (programEntity.getProgramType().equals(getString(R.string.simple_points))) {
                            jsonObject.put("points", transactionEntity.getPoints());
                        }
                        else if (programEntity.getProgramType().equals(getString(R.string.stamps_program))) {
                            jsonObject.put("stamps", transactionEntity.getStamps());
                        }

                        jsonArray.put(jsonObject);
                    }
                }
                jsonObjectData.put("transactions", jsonArray);
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody
                        .create(MediaType.parse(
                                "application/json; charset=utf-8"), requestData.toString());

                mApiClient
                        .getLoystarApi(false)
                        .createInvoice(requestBody).enqueue(new Callback<Invoice>() {
                    @Override
                    public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                        if (response.isSuccessful()) {
                            Invoice invoice = response.body();
                            invoiceNumber = invoice.getNumber();
                            dialog.dismiss();
                            showChoiceDialog();
                            if (invoice != null) {
                                for (int i=0; i < invoiceEntity.getTransactions().size(); i++) {
                                    mDataStore.delete(invoiceEntity.getTransactions().get(i)).subscribe();
                                    if (i +1 == invoiceEntity.getTransactions().size()) {
                                        String query = "DELETE FROM Invoice WHERE ROWID=" + invoiceEntity.getId();
                                        ReactiveResult<Tuple> result = mDataStore.raw(query);
                                        if (result != null && result.first() != null) {
                                            try {
                                                Integer deletedEntity = result.first().get(0);
                                                Timber.e("DELETED INVOICE: %s", deletedEntity);
                                            } catch (ClassCastException e) {
                                                try {
                                                    Long deletedEntity = result.first().get(0);
                                                    Timber.e("DELETED INVOICE: %s", deletedEntity);
                                                } catch (ClassCastException e1) {
                                                    e1.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                }
                                CustomerEntity customerEntity = mDatabaseManager
                                        .getCustomerByUserId(invoice.getCustomer().getUser_id());
                                InvoiceEntity newInvoiceEntity =  new InvoiceEntity();
                                newInvoiceEntity.setId(invoice.getId());
                                newInvoiceEntity.setAmount(invoice.getSubtotal());
                                newInvoiceEntity.setSubTotal(invoice.getSubtotal());
                                newInvoiceEntity.setCreatedAt(new Timestamp(invoice.getCreatedAt().getMillis()));
                                newInvoiceEntity.setUpdatedAt(new Timestamp(invoice.getUpdatedAt().getMillis()));
                                newInvoiceEntity.setCustomer(customerEntity);
                                newInvoiceEntity.setSynced(true);
                                newInvoiceEntity.setNumber(invoice.getNumber());
                                newInvoiceEntity.setStatus(invoice.getStatus());
                                newInvoiceEntity.setOwner(merchantEntity);
                                newInvoiceEntity.setDueDate(invoice.getDueDate());
                                newInvoiceEntity.setPaymentMessage(invoice.getPaymentMessage());

                                mDataStore.upsert(newInvoiceEntity).subscribe(invoiceEntity -> {
                                    for (ItemsItem transaction: invoice.getItems()) {
                                        InvoiceTransactionEntity transactionEntity = new InvoiceTransactionEntity();
                                        transactionEntity.setId(invoice.getId());
                                        transactionEntity.setUserId(invoice.getCustomer().getUser_id());
                                        transactionEntity.setMerchantLoyaltyProgramId(
                                                transaction.getProduct().getMerchant_loyalty_program_id());
                                        transactionEntity.setAmount(Double.valueOf(invoice.getSubtotal()).intValue());
                                        transactionEntity.setMerchant(merchantEntity);
                                        transactionEntity.setProductId(transaction.getProduct().getId());
                                        transactionEntity.setCustomer(customerEntity);
                                        transactionEntity.setInvoice(invoiceEntity);
                                        mDataStore.upsert(transactionEntity).subscribe(/*no-op*/);
                                    }
                                });
                            }
                        } else {
                            try {
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            } catch (Exception exception) {
                                Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Invoice> call, Throwable t) {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        Timber.e(t);
                    }
                });
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

    }

    private void downloadPdf(int invoiceId) {
                    dialog.show();
        mApiClient.getLoystarApi(false)
                .getInvoiceDownloadLink(invoiceId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    Log.e("ddd", response.getMessage());
                    shareToWhatsApp(response.getMessage());
//                    Intent invoiceIntent = new Intent(this, InvoiceListActivity.class);
//                    startActivity(invoiceIntent);
                });
    }

    private void shareToWhatsApp(String url) {
        if (url != null) {
            try{
//                dialog.setTitle("Sending Invoice Payment to whatsapp");
//                dialog.show();
                String toNumber = mSelectedCustomer.getPhoneNumber().substring(1);
                String encodedString = URLEncoder.encode( "Hello " +
                        mSelectedCustomer.getLastName()
                        + " " + mSelectedCustomer.getFirstName() + ", please download the invoice "
                        + invoiceNumber + " from the SHACK: ", "UTF-8");
                String message = "http://api.whatsapp.com/send?phone=" + toNumber + "&text=" +
                        encodedString + url;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(message));
                Log.e("MESSAGE", message );
                startActivity(intent);
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.share_invoice_to_customer, menu);
        return true;
    }


    @Override
    public void onCustomerSelected(@NonNull CustomerEntity customerEntity) {
        mSelectedCustomer = customerEntity;
        customerLive.setValue(customerEntity);
    }

    public void showChoiceDialog() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);
        builder.setTitle("Do you want to send the invoice? ");
        builder.setPositiveButton("Yes", null);
        builder.setNegativeButton("No", ((dialog, i) -> {
            dialog.dismiss();
            Intent intent = new Intent(this, MerchantBackOfficeActivity.class);
            startActivity(intent);
        }));

        AlertDialog alertDialog  =  builder.create();
        alertDialog.show();

        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(view -> {
            alertDialog.dismiss();
            sendOptionsDialog.show(getSupportFragmentManager(), SendOptionsDialog.TAG);
        });

    }

    @Override
    public void onSendWithEmail() {
        Integer lastInvoiceId = databaseManager.getLastInvoiceRecordId();
        sendInvoiceToCustomer(lastInvoiceId);
    }

//    @Override
//    public void onSendWithWhatsapp() {
//        Integer lastInvoiceId = databaseManager.getLastInvoiceRecordId();
//        Log.e("last", lastInvoiceId + "");
//        downloadPdf(lastInvoiceId);
//    }

    @Override
    public void onDownloadPdf() {
        if (isStoragePermissionGranted()) {
            Integer lastInvoiceId = databaseManager.getLastInvoiceRecordId();
            completePaymentButton.setEnabled(false);
            startInvoiceDownload(lastInvoiceId);
        }

    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    &&  checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            startInvoiceDownload(invoiceId);
        }
    }
}
