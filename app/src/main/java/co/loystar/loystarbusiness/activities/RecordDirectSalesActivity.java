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
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.squareup.otto.Subscribe;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.events.AddPointsOnFinishEvent;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.RecordDirectSalesActivityFragmentEvent;
import co.loystar.loystarbusiness.fragments.AddPointsFragment;
import co.loystar.loystarbusiness.fragments.RecordDirectSalesActivityFragment;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * Created by ordgen on 7/4/17.
 */

/*Intent Extras*/
/**
 * String mLoyaltyProgramType => loyalty program type
 * Long mLoyaltyProgramId => loyalty program ID
 * Long mCustomerId => customer ID for direct points transaction (required for startPointsTransaction)
 * boolean startPointsTransaction => boolean flag to start AddPointsFragment fragment;
 * */

@RuntimePermissions
public class RecordDirectSalesActivity extends AppCompatActivity {

    /*static fields*/
    public static final String LOYALTY_PROGRAM_TYPE = "loyaltyProgramType";
    public static final String LOYALTY_PROGRAM_ID = "loyaltyProgramId";
    public static final String RECORD_DIRECT_SALES_ACTIVITY_FRAGMENT = "recordDirectSalesActivityFragment";
    public static final String CUSTOMER_PHONE_NUMBER = "customerPhoneNumber";
    public static final String CUSTOMER_AMOUNT_SPENT = "customerAmountSpent";
    public static final int ADD_NEW_CUSTOMER_REQUEST = 100;
    public static final String TOTAL_CUSTOMER_POINTS = "totalCustomerPoints";
    public static final String CUSTOMER_ID = "customerId";
    public static final String ADD_POINTS_FRAGMENT = "addPointsFragment";
    public static final int REQUEST_PERMISSION_SETTING = 103;
    public static final String CUSTOMER_NAME = "customerName";
    public static final String CUSTOMER_PROGRAM_WORTH = "customerProgramWorth";
    public static final String START_ADD_POINTS_FRAGMENT = "startAddPointsFragment";

    /*views*/
    private View mLayout;

    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private String mLoyaltyProgramType;
    private Long mLoyaltyProgramId;
    private Fragment mFragment = null;
    private FragmentManager mFragmentManager = getSupportFragmentManager();
    private Context mContext;
    private SessionManager sessionManager;
    private boolean isSimplePoints = false;
    private boolean isStamps = false;
    private String mCustomerAmountSpent;
    private DBCustomer mCustomer;
    private MixpanelAPI mixPanel;
    private BottomBar bottomNavigationBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_direct_sales);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mLayout = findViewById(R.id.activity_record_direct_sales_coordinator_layout);
        mContext = this;
        sessionManager = new SessionManager(this);
        mixPanel = MixpanelAPI.getInstance(mContext, BuildConfig.MIXPANEL_TOKEN);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        mLoyaltyProgramType = extras.getString(LOYALTY_PROGRAM_TYPE, "");
        mLoyaltyProgramId = extras.getLong(LOYALTY_PROGRAM_ID, 0L);
        Long mCustomerId = extras.getLong(CUSTOMER_ID, 0L);
        mCustomer = databaseHelper.getCustomerById(mCustomerId);
        boolean startAddPointsDirectly = extras.getBoolean(START_ADD_POINTS_FRAGMENT, false);

        if (mLoyaltyProgramType.equals(getString(R.string.simple_points))) {
            isSimplePoints = true;
        }
        else if (mLoyaltyProgramType.equals(getString(R.string.stamps_program))) {
            isStamps = true;
        }

        if (savedInstanceState == null) {
            if (startAddPointsDirectly) {
                int totalUserPoints = databaseHelper.getTotalUserPointsForProgram(mCustomer.getUser_id(), mLoyaltyProgramId, sessionManager.getMerchantId());
                Bundle data = new Bundle();
                data.putLong(LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
                data.putLong(CUSTOMER_ID, mCustomerId);
                data.putString(CUSTOMER_AMOUNT_SPENT, "0");
                data.putString(TOTAL_CUSTOMER_POINTS, String.valueOf(totalUserPoints));

                mFragment = new AddPointsFragment();
                mFragment.setArguments(data);
                mFragmentManager.beginTransaction().add(R.id.activity_record_direct_sales_container, mFragment, ADD_POINTS_FRAGMENT).commit();
            }
            else {
                Bundle bundle = new Bundle();
                bundle.putString(LOYALTY_PROGRAM_TYPE, mLoyaltyProgramType);
                mFragment = new RecordDirectSalesActivityFragment();
                mFragment.setArguments(bundle);
                mFragmentManager.beginTransaction().add(R.id.activity_record_direct_sales_container, mFragment, RECORD_DIRECT_SALES_ACTIVITY_FRAGMENT).commit();
            }
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationBar = (BottomBar) findViewById(R.id.activity_record_direct_sales_bottom_navigation_bar);
        bottomNavigationBar.selectTabWithId(R.id.record_sale);
        bottomNavigationBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.home:
                        Intent homeIntent = new Intent(mContext, MerchantBackOffice.class);
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(homeIntent);
                        break;
                    case R.id.record_sale:
                        break;
                    case R.id.customers:
                        Intent customerIntent = new Intent(mContext, CustomerListActivity.class);
                        customerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(customerIntent);
                        break;
                    case R.id.campaigns:
                        Intent loyaltyIntent = new Intent(mContext, LoyaltyProgramsListActivity.class);
                        loyaltyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(loyaltyIntent);
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);

        RecordDirectSalesActivityFragment directSalesActivityFragment = (RecordDirectSalesActivityFragment) mFragmentManager.findFragmentByTag(RECORD_DIRECT_SALES_ACTIVITY_FRAGMENT);
        AddPointsFragment addPointsFragment = (AddPointsFragment) mFragmentManager.findFragmentByTag(ADD_POINTS_FRAGMENT);

        if ((directSalesActivityFragment != null && directSalesActivityFragment.isVisible())
                || (addPointsFragment != null && addPointsFragment.isVisible()) && bottomNavigationBar != null) {
            bottomNavigationBar.selectTabWithId(R.id.record_sale);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Subscribe
    public void OnRecordDirectSalesActivityFragmentEvent(RecordDirectSalesActivityFragmentEvent.OnFinish onFinish) {
        Bundle extras = onFinish.getExtras();
        mCustomerAmountSpent = extras.getString(CUSTOMER_AMOUNT_SPENT, "0");
        Long mCustomerId = extras.getLong(CUSTOMER_ID, 0L);

        DBCustomer customer = databaseHelper.getCustomerById(mCustomerId);
        mCustomer = customer;
        addPointsOrStamps(customer);
    }

    @Subscribe
    public void OnAddPointsEvent(AddPointsOnFinishEvent.OnFinish onFinish) {

        if (mCustomer == null) {
            return;
        }
        RecordDirectSalesActivityPermissionsDispatcher.syncDataWithCheck(RecordDirectSalesActivity.this);
        Bundle extras = onFinish.getExtras();
        String mTotalCustomerPoints = extras.getString(TOTAL_CUSTOMER_POINTS, "");

        Bundle data = new Bundle();
        data.putString(LOYALTY_PROGRAM_TYPE, mLoyaltyProgramType);
        data.putString(CUSTOMER_PROGRAM_WORTH, mTotalCustomerPoints);
        data.putString(CUSTOMER_NAME, mCustomer.getFirst_name());
        data.putBoolean(TransactionsConfirmation.SHOW_CONTINUE_BUTTON, true);
        data.putLong(LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
        data.putLong(CUSTOMER_ID, mCustomer.getId());


        try {
            JSONObject transactionProps = new JSONObject();
            transactionProps.put("TransactionType", mLoyaltyProgramType);
            mixPanel.track("New transaction", transactionProps);
        } catch (JSONException e) {
            e.printStackTrace();
            //Crashlytics.logException(e);
        }

        Intent intent = new Intent(mContext, TransactionsConfirmation.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtras(data);
        startActivity(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                DBCustomer customer = databaseHelper.getCustomerById(extras.getLong(CUSTOMER_ID, 0L));
                mCustomer = customer;

                getSupportFragmentManager().beginTransaction().
                        remove(getSupportFragmentManager().findFragmentByTag(RECORD_DIRECT_SALES_ACTIVITY_FRAGMENT)).commit();

                Bundle bundle = new Bundle();
                bundle.putString(LOYALTY_PROGRAM_TYPE, mLoyaltyProgramType);
                bundle.putLong(CUSTOMER_ID, customer.getId());
                mFragment = new RecordDirectSalesActivityFragment();
                mFragment.setArguments(bundle);
                mFragmentManager.beginTransaction().add(R.id.activity_record_direct_sales_container, mFragment, RECORD_DIRECT_SALES_ACTIVITY_FRAGMENT).commit();
            }
        }
        else if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (resultCode == RESULT_OK) {
                RecordDirectSalesActivityPermissionsDispatcher.syncDataWithCheck(RecordDirectSalesActivity.this);
            }
        }
    }

    protected void addPointsOrStamps(DBCustomer customer) {
        if (customer == null) {
            return;
        }
        if (isSimplePoints) {
            int totalUserPoints = databaseHelper.getTotalUserPointsForProgram(customer.getUser_id(), mLoyaltyProgramId, sessionManager.getMerchantId());
            Bundle data = new Bundle();
            data.putLong(LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
            data.putLong(CUSTOMER_ID, customer.getId());
            data.putString(CUSTOMER_AMOUNT_SPENT, mCustomerAmountSpent);
            data.putString(TOTAL_CUSTOMER_POINTS, String.valueOf(totalUserPoints));

            mFragment = new AddPointsFragment();
            mFragment.setArguments(data);
            mFragmentManager.beginTransaction().replace(R.id.activity_record_direct_sales_container, mFragment, ADD_POINTS_FRAGMENT).addToBackStack(null).commit();
        }
        else if (isStamps) {
            Intent addStampsIntent = new Intent(mContext, AddStampsActivity.class);
            addStampsIntent.putExtra(LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
            addStampsIntent.putExtra(CUSTOMER_ID, customer.getId());
            addStampsIntent.putExtra(CUSTOMER_AMOUNT_SPENT, Integer.parseInt(mCustomerAmountSpent));
            startActivity(addStampsIntent);
        }
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
    public void syncData() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(RecordDirectSalesActivity.this);
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
        RecordDirectSalesActivityPermissionsDispatcher.onRequestPermissionsResult(RecordDirectSalesActivity.this, requestCode, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        else if (id == R.id.add_customer_from_menu) {
            Intent addCustomerIntent = new Intent(mContext, AddNewCustomerActivity.class);
            startActivityForResult(addCustomerIntent, ADD_NEW_CUSTOMER_REQUEST);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add_customer, menu);
        return true;
    }
}
