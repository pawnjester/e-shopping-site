package co.loystar.loystarbusiness.activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.joda.time.DateTime;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;

import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.Invoice;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.ui.buttons.GreenButton;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;

public class InvoicePayActivity extends AppCompatActivity{

    private String payment_option;
    private String due_date;
    private DatabaseManager databaseManager;
    private Toolbar toolbar;

    private RadioGroup paymentRadioGroup;
    private EditText amountText;
    private EditText due_date_picker;
//
    GreenButton completePaymentButton;
    private ReactiveEntityStore<Persistable> mDataStore;
    private CustomerEntity mSelectedCustomer;
    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    int customerId;

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
//        customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
//        mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, customerId).blockingGet();
        paymentRadioGroup = findViewById(R.id.payment_radio_group);
        completePaymentButton = findViewById(R.id.completePayment);
        amountText = findViewById(R.id.record_payment);
        databaseManager = DatabaseManager.getInstance(this);
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class,
                mSessionManager.getMerchantId()).blockingGet();
        due_date_picker = findViewById(R.id.due_date_picker);

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


    }

    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SimpleDateFormat")
    private void getDateDialog() {
//        new DatePickerDialog(this,
//                (DatePickerDialog.OnDateSetListener) (datePicker, year, month, dayOfMonth) -> {
//            Calendar calendar = Calendar.getInstance();
//            calendar.set(Calendar.YEAR, year);
//            calendar.set(Calendar.MONTH, month);
//            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
//
//            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            String formattedDate = format.format(calendar.getTime());
//            due_date = formattedDate;
//            due_date_picker.setText(due_date);
//        })
//        val dateListener =
//                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
//                    val calendar = Calendar.getInstance()
//                    calendar.set(Calendar.YEAR, year)
//                    calendar.set(Calendar.MONTH, month)
//                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
//                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                    val date = calendar.time
//                    val formattedDate = format.format(date)
//                    due_date = formattedDate
//                    due_date_picker.setText(due_date)
//                }
//        val dateDialog = DatePickerDialog(this, dateListener, 1990, 1, 1)
//        dateDialog.show()

    }

    private void createInvoice() {
        Integer lastInvoiceId = databaseManager.getLastInvoiceRecordId();
        SparseIntArray mSelectedProducts = new SparseIntArray();

        InvoiceEntity newInvoiceEntity = new InvoiceEntity();
        if (lastInvoiceId == null) {
            newInvoiceEntity.setId(1);
        } else {
            newInvoiceEntity.setId(lastInvoiceId + 1);
        }

        newInvoiceEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
        newInvoiceEntity.setSynced(false);
        newInvoiceEntity.setPaidAmount(amountText.getText().toString());
        newInvoiceEntity.setPaymentMethod(payment_option);
        newInvoiceEntity.setCustomer(mSelectedCustomer);

        mDataStore.upsert(newInvoiceEntity).subscribe(invoiceEntity -> {
            ArrayList<Integer> productIds = new ArrayList<>();
            for (int i = 0; i < mSelectedProducts.size(); i++) {
                productIds.add(mSelectedProducts.keyAt(i));
            }
            Result<ProductEntity> result = mDataStore.select(ProductEntity.class)
                    .where(ProductEntity.ID.in(productIds))
                    .orderBy(ProductEntity.UPDATED_AT.desc())
                    .get();
            List<ProductEntity> productEntities = result.toList();
            Integer lastTransactionId = databaseManager.getLastTransactionRecordId();
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
                Log.e("loyalty", loyaltyProgram +"");
                if (loyaltyProgram != null) {
                    SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
                    transactionEntity.setId(newTransactionIds.get(i));

                    String template = "%.2f";
                    double tc = product.getPrice() * mSelectedProducts.get(product.getId());
                    double totalCost = Double.valueOf(String.format(Locale.UK, template, tc));
                    transactionEntity.setAmount(totalCost);
                    transactionEntity.setMerchantLoyaltyProgramId(loyaltyProgram.getId());

                    if (loyaltyProgram.getProgramType().equals(getString(R.string.simple_points))) {
                        transactionEntity.setPoints(Double.valueOf(totalCost).intValue());
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
                        transactionEntity.setCustomer(mSelectedCustomer);
                    }
                    transactionEntity.setSynced(false);
                    transactionEntity.setMerchant(merchantEntity);
                    mDataStore.upsert(transactionEntity).subscribe(/*no-op*/);

                    if (i +1 == productEntities.size()) {
                        SyncAdapter.performSync(this, mSessionManager.getEmail());
                    }
                }
            }

        });


//        newInvoiceEntity.setA
//            val jsonObjectData = JSONObject()
//            jsonObjectData.put("user_id", 2646)
//            jsonObjectData.put("business_branch_id", null)
//            jsonObjectData.put("payment_method", payment_option)
//            jsonObjectData.put("paid_amount", null)
//            jsonObjectData.put("send_notification", true)
//            jsonObjectData.put("payment_message", "Contact us for payment: 09099999999")
//
//            val jsonArray = JSONArray()
//
//            val jsonObject = JSONObject()
//            jsonObject.put("user_id", 2646)
//            jsonObject.put("amount", 20.0)
//            jsonObject.put("merchant_id", 497)
//            jsonObject.put("product_id", 512)
//            jsonObject.put("program_type", "SimplePoints")
//            jsonObject.put("merchant_loyalty_program_id", 489)
//            jsonObject.put("points", 20)
//            jsonObject.put("stamp", 0)
//
//            jsonArray.put(jsonObject)
//            jsonObjectData.put("transactions", jsonArray)
//            val requestData = JSONObject()
//            requestData.put("data", jsonObjectData)
//            val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString())
//            apiClient.getLoystarApi(false)
//                    .createInvoice(requestBody)
//                    .subscribeOn(Schedulers.newThread())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe {
//                        response ->
//                        Log.e("UUUU", response.toString())
//                    }
    }
}
