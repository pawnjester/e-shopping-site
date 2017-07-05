package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.StampsImageViewAdapter;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TimeUtils;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.CUSTOMER_NAME;
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.CUSTOMER_PROGRAM_WORTH;
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE;


/*Intent Extras*/
/**
 * Long mProgramId => loyalty program id
 * Long mCustomerId => customer id
 * Long mProductId => selected product Id (optional)
 * int amountSpent => amount spent by customer (optional)
 * */

@RuntimePermissions
public class AddStampsActivity extends AppCompatActivity {

    /*constants*/
    public static final int REQUEST_PERMISSION_SETTING = 103;
    private static String template = "TOTAL STAMPS: %s";
    private static final String TAG = AddStampsActivity.class.getCanonicalName();

    private int currentCustomerStamps;
    private int initialCustomerStamps;
    int amountSpent;
    private Long mProgramId;
    private Long mProductId;
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private Context mContext;

    /*views*/
    private TextView totalStampsTextView;
    private View mLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stamps_transactions);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mLayout = findViewById(R.id.stamps_transactions_container);
        mContext = this;
        final Bundle bundle = getIntent().getExtras();
        mProgramId = bundle.getLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID);
        mProductId = bundle.getLong(RecordStampsSalesWithPosActivity.PRODUCT_ID, 0L);
        amountSpent = bundle.getInt(RecordDirectSalesActivity.CUSTOMER_AMOUNT_SPENT, 0);
        final Long mCustomerId = bundle.getLong(RecordDirectSalesActivity.CUSTOMER_ID, 0L);

        final GridView gridview = (GridView) findViewById(R.id.gridview);
        totalStampsTextView = (TextView) findViewById(R.id.total_stamps);
        Button addStampsBtn = (Button) findViewById(R.id.add_stamps);

        DBMerchantLoyaltyProgram loyaltyProgram = databaseHelper.getProgramById(mProgramId, sessionManager.getMerchantId());
        int stampsThreshold = loyaltyProgram.getThreshold();


        final DBCustomer mCustomer = databaseHelper.getCustomerById(mCustomerId);
        currentCustomerStamps = databaseHelper.getTotalUserStampsForProgram(mCustomer.getUser_id(), mProgramId, sessionManager.getMerchantId());
        initialCustomerStamps = currentCustomerStamps;


        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                mCustomer.getFirst_name().replace("\"", "").substring(0, 1).toUpperCase() +
                    mCustomer.getFirst_name().replace("\"", "").substring(1)
            );
        }

        String[] USER_STAMPS = new String[currentCustomerStamps];

        for (int i = 0; i< currentCustomerStamps; i++) {
            USER_STAMPS[i] = "CHECKED";
        }

        final String[] COUNTS = new String[stampsThreshold];

        for (int i = 0; i< stampsThreshold; i++) {
            if (i < USER_STAMPS.length) {
                COUNTS[i] = USER_STAMPS[i];
            } else {
                COUNTS[i] = "UNCHECKED";
            }
        }

        addStampsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            int userStampsForThisTransaction = currentCustomerStamps - initialCustomerStamps;
                if (mProductId > 0) {
                    DBProduct product = databaseHelper.getProductById(mProductId);
                    if (product != null) {
                        amountSpent = (int) (userStampsForThisTransaction * product.getPrice());
                    }
                }
            DBTransaction dbTransaction = new DBTransaction();
            dbTransaction.setPoints(0);
            dbTransaction.setAmount(amountSpent);
            dbTransaction.setProgram_type(getString(R.string.stamps_program));
            dbTransaction.setStamps(userStampsForThisTransaction);
            dbTransaction.setLocal_db_created_at(TimeUtils.getCurrentDateAndTime());
            dbTransaction.setUser_id(mCustomer.getUser_id());
            if (databaseHelper.getProductById(mProductId) != null) {
                dbTransaction.setProduct_id(mProductId);
            }
            dbTransaction.setMerchant_id(sessionManager.getMerchantId());
            dbTransaction.setSynced(false);
            dbTransaction.setMerchant_loyalty_program_id(mProgramId);

            databaseHelper.insertTransaction(dbTransaction);
            int totalStamps = databaseHelper.getTotalUserStampsForProgram(mCustomer.getUser_id(), mProgramId, sessionManager.getMerchantId());

            AddStampsActivityPermissionsDispatcher.syncDataWithCheck(AddStampsActivity.this);

            Bundle bundle = new Bundle();
            bundle.putString(LOYALTY_PROGRAM_TYPE, getString(R.string.stamps_program));
            bundle.putString(CUSTOMER_NAME, mCustomer.getFirst_name());
            bundle.putString(CUSTOMER_PROGRAM_WORTH, String.valueOf(totalStamps));
            bundle.putBoolean(TransactionsConfirmation.SHOW_CONTINUE_BUTTON, true);
            bundle.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mProgramId);
            bundle.putLong(RecordDirectSalesActivity.CUSTOMER_ID, mCustomerId);

            Intent intent = new Intent(mContext, TransactionsConfirmation.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtras(bundle);
            startActivity(intent);
            }
        });

        final List<String> total_stamps = new LinkedList<String>(Arrays.asList(COUNTS));

        String ts = String.format(template, currentCustomerStamps);
        totalStampsTextView.setText(ts);

        gridview.setAdapter(new StampsImageViewAdapter(mContext, total_stamps));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                final StampsImageViewAdapter associatedAdapter = (StampsImageViewAdapter) parent.getAdapter();
                String associatedItem = (String) associatedAdapter.getItem(position);
                if (associatedItem.equals("CHECKED")) {
                    currentCustomerStamps -= 1;
                    COUNTS[position] = "UNCHECKED";
                    List<String> ts = new LinkedList<String>(Arrays.asList(COUNTS));
                    gridview.setAdapter(new StampsImageViewAdapter(mContext, ts));
                    associatedAdapter.notifyDataSetChanged();
                    String tss = String.format(template, currentCustomerStamps);
                    totalStampsTextView.setText(tss);
                }
                if (associatedItem.equals("UNCHECKED")) {
                    currentCustomerStamps += 1;
                    COUNTS[position] = "CHECKED";
                    List<String> tsv = new LinkedList<String>(Arrays.asList(COUNTS));
                    gridview.setAdapter(new StampsImageViewAdapter(mContext, tsv));
                    associatedAdapter.notifyDataSetChanged();
                    String tss = String.format(template, currentCustomerStamps);
                    totalStampsTextView.setText(tss);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
    public void syncData() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(AddStampsActivity.this);
        Account[] accounts = accountManager.getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
        String merchantEmail = sessionManager.getMerchantEmail().replace("\"", "");
        for (Account acc: accounts) {
            if (acc.name.equals(merchantEmail)) {
                account = acc;
            }
        }
        if (account != null) {
            SyncAdapter.syncImmediately(account);
        }
    }

    @OnShowRationale(Manifest.permission.GET_ACCOUNTS)
    void showRationaleForGetAccounts(final PermissionRequest request) {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.permission_get_accounts_rationale)
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
    void showDeniedForGetAccounts() {
        Snackbar.make(mLayout, R.string.permission_accounts_denied, Snackbar.LENGTH_LONG).show();
    }

    @OnNeverAskAgain(Manifest.permission.GET_ACCOUNTS)
    void showNeverAskForGetAccounts() {
        Snackbar.make(mLayout, R.string.permission_accounts_neverask,
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AddStampsActivityPermissionsDispatcher.onRequestPermissionsResult(AddStampsActivity.this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (resultCode == RESULT_OK) {
                AddStampsActivityPermissionsDispatcher.syncDataWithCheck(AddStampsActivity.this);
            }
        }
    }
}
