package co.loystar.loystarbusiness.activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;

import co.loystar.loystarbusiness.models.databinders.DownloadInvoice;
import co.loystar.loystarbusiness.models.databinders.Invoice;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceTransactionEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.BackgroundNotificationService;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.buttons.GreenButton;
import co.loystar.loystarbusiness.utils.ui.dialogs.CustomerAutoCompleteDialog;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoicePayActivity extends AppCompatActivity
        implements
        CustomerAutoCompleteDialog.SelectedCustomerListener{

    private String payment_option;
    private String due_date;
    private DatabaseManager databaseManager;
    private Toolbar toolbar;

    private RadioGroup paymentRadioGroup;
    private EditText amountText;
    private EditText due_date_picker;
    Button completePaymentButton;
    Button downloadPdf;
    private ReactiveEntityStore<Persistable> mDataStore;
    private CustomerEntity mSelectedCustomer;
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
    private ProgressBar createProgressbar;

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
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class,
                mSessionManager.getMerchantId()).blockingGet();
        customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        totalCost = getIntent().getDoubleExtra(Constants.CHARGE, 0);
        invoice_payment_method = getIntent().getStringExtra(Constants.PAYMENT_METHOD);
        mSelectedProducts = getIntent().getExtras().getIntegerArrayList(Constants.SELECTED_PRODUCTS);
        mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, customerId).blockingGet();
        invoiceId = getIntent().getIntExtra(Constants.INVOICE_ID, 0);
        paymentRadioGroup = findViewById(R.id.payment_radio_group);
        completePaymentButton = findViewById(R.id.completePayment);
        amountText = findViewById(R.id.record_payment);
        databaseManager = DatabaseManager.getInstance(this);
        due_date_picker = findViewById(R.id.due_date_picker);
        paymentMessage = findViewById(R.id.payment_message);
        totalAmount = findViewById(R.id.total_amount);
        totalAmount.setText(String.valueOf(totalCost));
        createProgressbar = findViewById(R.id.create_invoice_loader);
        mApiClient = new ApiClient(this);
        mDatabaseManager = DatabaseManager.getInstance(this);
        downloadPdf = findViewById(R.id.download_pdf);
        amount_due_value = findViewById(R.id.amount_due_value);
        mSelectedProductHash = (HashMap<Integer, Integer>) getIntent().getSerializableExtra(Constants.HASH_MAP);

        invoiceStatus = getIntent().getStringExtra(Constants.STATUS);

        customerAutoCompleteDialog = CustomerAutoCompleteDialog.newInstance(getString(R.string.reciept_owner));
        customerAutoCompleteDialog.setSelectedCustomerListener(this);
        paidAmount = getIntent().getStringExtra(Constants.PAID_AMOUNT);

        paymentRadioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            RadioButton rb = radioGroup.findViewById(i);
            payment_option = rb.getText().toString();
        });
        completePaymentButton.setOnClickListener(view -> createInvoice());
        SimpleDateFormat dateFormat =  new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String time = dateFormat.format(cal.getTime());
        due_date_picker.setText(time);
        due_date_picker.setOnClickListener(view -> getDateDialog());



        if (mSelectedCustomer == null) {
            customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
        }

        if (paidAmount != null ) {
            amountText.setText(paidAmount);
        }

        if (invoiceId > 0) {
            downloadPdf.setVisibility(View.VISIBLE);
            downloadPdf.setOnClickListener(view -> startInvoiceDownload());


            if (invoiceStatus != null && invoiceStatus.equals("paid")) {
                amount_due_value.setText(getResources().getString(R.string.paid_text));
                amountText.setEnabled(false);
                due_date_picker.setEnabled(false);
                paymentMessage.setEnabled(false);
                completePaymentButton.setEnabled(false);
                completePaymentButton.setBackgroundColor(getResources().getColor(R.color.light_grey));
            } else if (invoiceStatus != null && invoiceStatus.equals("unpaid") || invoiceStatus.equals("partial")) {
                Double difference;
                difference = totalCost -
                        Double.valueOf(
                                amountText.getText().toString().length() == 0 ? "0.0" : amountText.getText().toString());
                amount_due_value.setText(difference.toString());
            }
        }

        amountText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Double difference;
                difference = totalCost -
                        Double.valueOf(
                                amountText.getText().toString().length() == 0 ? "0.0" : amountText.getText().toString());

                if (difference == 0.0) {
                    amount_due_value.setText("0.0");
                } else {
                    amount_due_value.setText(difference.toString());
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        registerReceiver();
    }

    private void registerReceiver() {

        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PROGRESS_UPDATE);
        bManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void startInvoiceDownload() {
        Intent intent = new Intent(this, BackgroundNotificationService.class);
        intent.putExtra(Constants.INVOICE_ID, invoiceId);
        startService(intent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PROGRESS_UPDATE)) {
                boolean downloadComplete = intent.getBooleanExtra("downloadComplete", false);
                if (downloadComplete ) {
                    Toast.makeText(getApplicationContext(), "File download completed", Toast.LENGTH_SHORT).show();
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
            createProgressbar.setVisibility(View.VISIBLE);
        }
    };
    private BroadcastReceiver createInvoiceEnded = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            createProgressbar.setVisibility(View.GONE);
            intent = new Intent(context, MerchantBackOfficeActivity.class);
            startActivity(intent);
        }
    };

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_share_invoice:
                sendInvoiceToCustomer();
                return true;
            case R.id.action_delete:
                deleteInvoice(invoiceId);
                return true;
            case R.id.action_share_to_whatsapp:
                downloadPdf(invoiceId);
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
        if (invoiceId <= 0) {
            deleteItem.setEnabled(false);
            shareItem.setEnabled(false);
            shareItemWhatsapp.setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void sendInvoiceToCustomer() {
        mApiClient.getLoystarApi(false)
                .sendInvoiceToCustomer(invoiceId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.code() == 200) {
                            Toast.makeText(getApplicationContext(),
                                    "Invoice sent to the email",
                                    Toast.LENGTH_SHORT).show();
                            SyncAdapter.performSync(getApplicationContext(), mSessionManager.getEmail());
                            Intent merchantIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                            startActivity(merchantIntent);
                        }else {
                            Toast.makeText(getApplicationContext(),
                                    "Invoice could not be sent", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

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
        InvoiceEntity invoiceEntity = databaseManager.getInvoiceById(id);
        mApiClient.getLoystarApi(false).deleteInvoice(id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 204) {
                    mDataStore.delete(invoiceEntity).subscribe(/* no-op */);
                    Toast.makeText(getApplicationContext(),
                            "Invoice Deleted", Toast.LENGTH_SHORT).show();
                    Intent merchantIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                    startActivity(merchantIntent);
                    SyncAdapter.performSync(getApplicationContext(), mSessionManager.getEmail());
                }else {
                    Toast.makeText(getApplicationContext(),
                            "Invoice could not be deleted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    private void updateInvoice(int id) {
        InvoiceEntity invoiceEntity = databaseManager.getInvoiceById(id);
        invoiceEntity.setUpdatedAt(new Timestamp(new DateTime().getMillis()));
        invoiceEntity.setPaymentMethod(payment_option);
        invoiceEntity.setPaidAmount(amountText.getText().toString());
        invoiceEntity.setSynced(false);
        invoiceEntity.setOwner(merchantEntity);
        invoiceEntity.setCustomer(mSelectedCustomer);
        invoiceEntity.setDueDate(due_date);
        mDataStore.update(invoiceEntity).subscribe(updatedInvoiceEntity -> {
            try{

                JSONObject jsonObjectData = new JSONObject();
                if (Double.valueOf(updatedInvoiceEntity.getPaidAmount()) >= Double.valueOf(updatedInvoiceEntity.getAmount())) {
                    jsonObjectData.put("paid_amount", updatedInvoiceEntity.getPaidAmount());
                    jsonObjectData.put("status", "paid");
                } else if ( Double.valueOf(updatedInvoiceEntity.getPaidAmount()) < Double.valueOf(updatedInvoiceEntity.getAmount())) {
                    jsonObjectData.put("paid_amount", updatedInvoiceEntity.getPaidAmount());
                    jsonObjectData.put("status", "partial");
                }else {
                    jsonObjectData.put("paid_amount", updatedInvoiceEntity.getPaidAmount());
                    jsonObjectData.put("status", "unpaid");
                }

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

                RequestBody requestBody = RequestBody
                        .create(MediaType.parse(
                                "application/json; charset=utf-8"), requestData.toString());
                mApiClient.getLoystarApi(false).updateInvoice(
                        updatedInvoiceEntity.getId(), requestBody).enqueue(new Callback<Invoice>() {
                    @Override
                    public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                        if (response.isSuccessful()) {
                            Invoice invoice = response.body();
                            updatedInvoiceEntity.setUpdatedAt(new Timestamp(new DateTime().getMillis()));
                            updatedInvoiceEntity.setPaymentMethod(payment_option);
                            updatedInvoiceEntity.setPaidAmount(amountText.getText().toString());
                            updatedInvoiceEntity.setSynced(false);
                            updatedInvoiceEntity.setOwner(merchantEntity);
                            updatedInvoiceEntity.setCustomer(mSelectedCustomer);
                            mDataStore.update(updatedInvoiceEntity).subscribe(/*no-op*/);
                            SyncAdapter.performSync(getApplicationContext(), mSessionManager.getEmail());

                        }else {
                            Toast.makeText(getApplicationContext(),
                                    "Invoice could not be updated", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Invoice> call, Throwable t) {

                    }
                });
            } catch (JSONException e) {

            }
        });
    }


    private void createInvoice() {
        Intent intent = new Intent(Constants.CREATE_INVOICE_STARTED);
        sendBroadcast(intent);
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

        if(amountText.getText().length() > 0) {
            newInvoiceEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
            newInvoiceEntity.setSynced(false);
            newInvoiceEntity.setPaidAmount(amountText.getText().toString());
            newInvoiceEntity.setPaymentMethod(payment_option);
            newInvoiceEntity.setCustomer(mSelectedCustomer);
            newInvoiceEntity.setOwner(merchantEntity);
            newInvoiceEntity.setDueDate(due_date_picker.getText().toString());
            newInvoiceEntity.setAmount(String.valueOf(totalCost));
            newInvoiceEntity.setPaymentMessage(paymentMessage.toString());
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
                            SyncAdapter.performSync(this, mSessionManager.getEmail());
                        }
                    }
                }

            });
        } else  {
            Toast.makeText(this, "Please record an amount", Toast.LENGTH_SHORT).show();
        }

    }
    }

    private void downloadPdf(int invoiceId) {
        mApiClient.getLoystarApi(false)
                .getInvoiceDownloadLink(invoiceId)
                .flatMap((Function<DownloadInvoice, ObservableSource<Response<ResponseBody>>>) downloadInvoice ->
                        mApiClient.getLoystarApi(false)
                                .downloadInvoice(downloadInvoice.getMessage().substring(33)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doOnError(error -> Toast.makeText(getApplicationContext(),
                        "Invoice could not be downloaded", Toast.LENGTH_SHORT).show())
                .subscribe(response -> {
                    shareToWhatsApp(response.raw().request().url().toString());
                });
    }

    private void shareToWhatsApp(String url) {
        if (url != null) {
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(whatsappIntent,
                    "Send Invoice Using: "));
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
    }
}
