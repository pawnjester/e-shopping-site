package co.loystar.loystarbusiness.activities;


import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.onesignal.OneSignal;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;
import com.uxcam.UXCam;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.GraphCoordinates;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput.CurrenciesFetcher;
import io.smooch.core.Smooch;
import io.smooch.core.User;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE;

/**
 * Created by ordgen on 7/4/17.
 */

@RuntimePermissions
public class MerchantBackOffice extends AppCompatActivity implements OnChartValueSelectedListener {
    /*constants*/
    public static final int RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM = 111;
    public static final int RECORD_SALES_WITH_POS_CHOOSE_PROGRAM = 130;
    public static final String CUSTOMER_PHONE_NUMBER = "customerPhoneNumber";
    public static final String CUSTOMER_NAME = "customerName";
    public static final String CUSTOMER_ID = "customerId";
    public static final int REQUEST_PERMISSION_SETTING = 103;
    public static final String NEW_LOGIN = "newLogin";
    public static final String LOYALTY_PROGRAM_ID = "loyaltyProgramId";
    private static final String TAG = MerchantBackOffice.class.getCanonicalName();
    public static final String RECORD_SALE_WITHOUT_POS = "recordSaleWithoutPos";

    private Context mContext;
    private ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms;
    private boolean isNewLogin = false;
    private DBMerchant merchant;

    private ProgressDialog progressDialog;
    private View mLayout;
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private MixpanelAPI mixPanel;
    private BarChart barChart;
    private String merchantCurrencySymbol;
    private BottomBar bottomNavigationBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_merchant_back_office);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLayout = findViewById(R.id.mb_coordinatorLayout);
        merchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());
        loyaltyPrograms = databaseHelper.listMerchantPrograms(sessionManager.getMerchantId());
        merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(this).getCurrency(sessionManager.getMerchantCurrency()).getSymbol();
        mContext = this;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(sessionManager.getBusinessName().substring(0, 1).toUpperCase() + sessionManager.getBusinessName().substring(1));
        }

        Log.e(TAG, "MERCHANT_ID: " + sessionManager.getMerchantId());

        if (merchant == null) {
            merchant = new DBMerchant();
            merchant.setId(sessionManager.getMerchantId());
            merchant.setFirst_name(sessionManager.getMerchantFirstName());
            merchant.setLast_name(sessionManager.getMerchantLastName());
            merchant.setEmail(sessionManager.getMerchantEmail());
            merchant.setBusiness_name(sessionManager.getBusinessName());
            merchant.setBusiness_type(sessionManager.getMerchantBusinessType());
            merchant.setContact_number(sessionManager.getMerchantPhone());
            merchant.setCurrency(sessionManager.getMerchantCurrency());

            databaseHelper.insertMerchant(merchant);
        }

        ArrayList<DBCustomer> customers = databaseHelper.listMerchantCustomers(sessionManager.getMerchantId());
        ArrayList<DBTransaction> transactions = databaseHelper.listMerchantTransactions(sessionManager.getMerchantId());

        if (loyaltyPrograms.isEmpty() || customers.isEmpty() || transactions.isEmpty()) {
            MerchantBackOfficePermissionsDispatcher.syncDataWithCheck(this);
        }

        if (getSupportActionBar() != null) {
            if (sessionManager.getBusinessName() != null && sessionManager.getBusinessName().length() > 0)
                getSupportActionBar().setTitle(sessionManager.getBusinessName().substring(0, 1).toUpperCase() + sessionManager.getBusinessName().substring(1));
        }

        setupGraph();
        setupBottomNavigation();
        setUserInfoForPlugings();
        setupReceivedIntentActions();
    }

    private void setupGraph() {

        barChart = findViewById(R.id.chart);
        barChart.setDrawValueAboveBar(true);
        barChart.setDescription(null);
        barChart.setNoDataText("No Sales Recorded");

        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);
        barChart.setOnChartValueSelectedListener(this);


        Paint p = barChart.getPaint(Chart.PAINT_INFO);
        int emptyStateTextSize = getResources().getDimensionPixelSize(R.dimen.empty_state_text_title);
        int chartPadding = getResources().getDimensionPixelSize(R.dimen.chart_padding);
        p.setTextSize(emptyStateTextSize);
        p.setTypeface(LoystarApplication.getInstance().getTypeface());
        p.setColor(ContextCompat.getColor(mContext, R.color.black_overlay));

        barChart.setExtraTopOffset(chartPadding);
        barChart.setExtraBottomOffset(chartPadding);
        addGraphDataset();
    }

    private void addGraphDataset() {
        ArrayList<GraphCoordinates> graphCoordinates = databaseHelper.getMerchantSalesHistory(
                sessionManager.getMerchantId(), "daily");

        if (!graphCoordinates.isEmpty()) {
            String[] xVals = new String[graphCoordinates.size()];
            ArrayList<BarEntry> yVals = new ArrayList<>();
            DateFormat outFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
            Calendar todayCalendar = Calendar.getInstance();
            Calendar cal = Calendar.getInstance();
            try {
                String todayDate = TextUtilsHelper.getFormattedDateString(todayCalendar);
                Date todayDateWithoutTimeStamp = outFormatter.parse(todayDate);
                for (int i = 0; i < graphCoordinates.size(); i++) {
                    GraphCoordinates gc = graphCoordinates.get(i);
                    cal.setTime(gc.getX());
                    String dateString = TextUtilsHelper.getFormattedDateString(cal);
                    Date salesCreatedAt = outFormatter.parse(dateString);
                    if (salesCreatedAt.equals(todayDateWithoutTimeStamp)) {
                        dateString = "today";
                    }
                    xVals[i] = dateString;
                    yVals.add(new BarEntry(i, gc.getY()));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            XAxis xAxis = barChart.getXAxis();
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(xVals));

            YAxis leftAxis = barChart.getAxisLeft();
            leftAxis.setValueFormatter(new MyAxisValueFormatter());
            leftAxis.setTypeface(LoystarApplication.getInstance().getTypeface());

            YAxis rightAxis = barChart.getAxisRight();
            rightAxis.setEnabled(false);

            BarDataSet barDataSet = new BarDataSet(yVals, "Total Sales");
            barDataSet.setValueTypeface(LoystarApplication.getInstance().getTypeface());
            barDataSet.setValueTextSize(14);
            barDataSet.setValueFormatter(new MyAxisValueFormatter());
            barDataSet.setColor(ContextCompat.getColor(mContext, R.color.colorAccentLight));

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(barDataSet);

            BarData data = new BarData(dataSets);
            data.setValueTextSize(10f);
            data.setValueTypeface(LoystarApplication.getInstance().getTypeface());
            data.setBarWidth(0.9f);
            data.notifyDataChanged();

            barChart.setData(data);
            barChart.notifyDataSetChanged();
            barChart.invalidate();
        }
    }

    private void setupReceivedIntentActions() {
        if (getIntent().getBooleanExtra(MerchantSignUpActivity.KEY_SIGN_UP, false)) {
            //its a new signUp => on-board
            new AlertDialog.Builder(mContext)
                    .setTitle(getString(R.string.welcome) + " " + sessionManager.getMerchantFirstName() + "!")
                    .setMessage(getString(R.string.welcome_text))
                    .setPositiveButton(getString(R.string.get_started), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(mContext, CreateLoyaltyProgramListActivity.class);
                            startActivity(intent);
                        }
                    }).show().setCancelable(false);

        }

        else if (getIntent().getBooleanExtra(NEW_LOGIN, false)) {
            isNewLogin = true;
        }
        else if (getIntent().getBooleanExtra(RECORD_SALE_WITHOUT_POS, false)) {
            loyaltyPrograms = databaseHelper.listMerchantPrograms(sessionManager.getMerchantId());
            if (loyaltyPrograms.isEmpty()) {
                Intent intent = new Intent(mContext, CreateLoyaltyProgramListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else {
                if (loyaltyPrograms.size() == 1) {
                    DBMerchantLoyaltyProgram loyaltyProgram = loyaltyPrograms.get(0);
                    Bundle data = new Bundle();
                    data.putString(LOYALTY_PROGRAM_TYPE, loyaltyProgram.getProgram_type());
                    data.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());

                    Intent intent = new Intent(mContext, RecordDirectSalesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtras(data);
                    startActivity(intent);
                }
                else {
                    Intent chooseProgram = new Intent(MerchantBackOffice.this, SelectLoyaltyProgramForSales.class);
                    startActivityForResult(chooseProgram, RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM);
                }
            }
        }
    }

    private void setUserInfoForPlugings() {
        if (!BuildConfig.DEBUG) {
            if (!sessionManager.getMerchantEmail().equals("loystarapp@gmail.com") &&
                    !sessionManager.getMerchantEmail().equals("niinyarko1@gmail.com") &&
                    !sessionManager.getMerchantEmail().equals("boxxy@gmail.com")) {

                UXCam.startWithKey(BuildConfig.UXCAM_TOKEN);
                UXCam.tagUsersName(sessionManager.getMerchantEmail());

                mixPanel = MixpanelAPI.getInstance(mContext, BuildConfig.MIXPANEL_TOKEN);
                mixPanel.identify(sessionManager.getMerchantEmail());
                mixPanel.getPeople().identify(mixPanel.getDistinctId());
                mixPanel.getPeople().identify(sessionManager.getMerchantEmail());
                mixPanel.getPeople().set("BusinessType", sessionManager.getMerchantBusinessType());
                mixPanel.getPeople().set("phone", sessionManager.getMerchantPhone());
                mixPanel.getPeople().set("name", sessionManager.getBusinessName());
                mixPanel.getPeople().showNotificationIfAvailable(this);

                Smooch.login(sessionManager.getMerchantEmail(), null);
                User.getCurrentUser().setEmail(sessionManager.getMerchantEmail());
                User.getCurrentUser().setFirstName(sessionManager.getFullMerchantName());
                final Map<String, Object> customProperties = new HashMap<>();
                customProperties.put("businessName", sessionManager.getBusinessName());
                customProperties.put("merchantPhone", sessionManager.getMerchantPhone());
                customProperties.put("businessType", sessionManager.getMerchantBusinessType());

                if (isLoyaltyProgramCreated()) {
                    customProperties.put("createdLoyaltyProgram", true);
                    Smooch.track("Loyalty Program Created");
                } else {
                    customProperties.put("createdLoyaltyProgram", false);
                    Smooch.track("No Loyalty Program");
                }

                User.getCurrentUser().addProperties(customProperties);
                OneSignal.sendTag("user", sessionManager.getMerchantEmail());
                OneSignal.syncHashedEmail(sessionManager.getMerchantEmail());
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationBar = findViewById(R.id.bottom_navigation_bar);
        bottomNavigationBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.record_sale:
                        boolean isPosTurnedOn = merchant.getTurn_on_point_of_sale() != null
                                && merchant.getTurn_on_point_of_sale();
                        loyaltyPrograms = databaseHelper.listMerchantPrograms(sessionManager.getMerchantId());

                        if (loyaltyPrograms.isEmpty()) {
                            Intent intent = new Intent(mContext, CreateLoyaltyProgramListActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                        else {
                            if (loyaltyPrograms.size() == 1) {
                                DBMerchantLoyaltyProgram loyaltyProgram = loyaltyPrograms.get(0);
                                if (isPosTurnedOn) {
                                    if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                                        Intent intent = new Intent(mContext, RecordPointsSalesWithPosActivity.class);
                                        intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                        startActivity(intent);
                                    }
                                    else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                                        Intent intent = new Intent(mContext, RecordStampsSalesWithPosActivity.class);
                                        intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                        startActivity(intent);
                                    }
                                }
                                else {
                                    Bundle data = new Bundle();
                                    data.putString(RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE, loyaltyProgram.getProgram_type());
                                    data.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                    Intent intent = new Intent(mContext, RecordDirectSalesActivity.class);
                                    intent.putExtras(data);
                                    startActivity(intent);
                                }
                            }
                            else {
                                if (isPosTurnedOn) {
                                    Intent chooseProgram = new Intent(MerchantBackOffice.this, SelectLoyaltyProgramForSales.class);
                                    startActivityForResult(chooseProgram, RECORD_SALES_WITH_POS_CHOOSE_PROGRAM);
                                }
                                else {
                                    Intent chooseProgram = new Intent(MerchantBackOffice.this, SelectLoyaltyProgramForSales.class);
                                    startActivityForResult(chooseProgram, RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM);
                                }
                            }
                        }
                        break;
                    case R.id.customers:
                        Intent intent = new Intent(mContext, CustomerListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.campaigns:
                        Intent loyaltyIntent = new Intent(mContext, LoyaltyProgramsListActivity.class);
                        loyaltyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(loyaltyIntent);
                        break;
                }
            }
        });

        bottomNavigationBar.setOnTabReselectListener(new OnTabReselectListener() {
            @Override
            public void onTabReSelected(@IdRes int tabId) {
                if (tabId == R.id.home) {
                    if (barChart != null) {
                        barChart.highlightValues(null);
                        barChart.fitScreen();
                    }
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.merchant_back_office, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        ComponentName componentName = new ComponentName(mContext, SearchableActivity.class);
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));
            searchView.setIconifiedByDefault(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.sync_now_item:
                MerchantBackOfficePermissionsDispatcher.syncDataWithCheck(MerchantBackOffice.this);
                return true;
            case R.id.action_settings:
                Intent settings_intent = new Intent(mContext, SettingsActivity.class);
                startActivity(settings_intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
        try {
            unregisterReceiver(syncFinishedReceiver);
            unregisterReceiver(syncStartedReceiver);
            unregisterReceiver(syncAuthenticationFailureReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNavigationBar != null){
            bottomNavigationBar.selectTabWithId(R.id.home);

        }

        registerReceiver(syncFinishedReceiver, new IntentFilter(SyncAdapter.SYNC_FINISHED));
        registerReceiver(syncStartedReceiver, new IntentFilter(SyncAdapter.SYNC_STARTED));
        registerReceiver(syncAuthenticationFailureReceiver, new IntentFilter(SyncAdapter.AUTH_FAILURE));

        BusProvider.getInstance().register(this);
    }

    private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if (isNewLogin) {
                isNewLogin = false;
            }

            Snackbar.make(mLayout, R.string.records_updated_notice,
                    Snackbar.LENGTH_LONG)
                    .show();

            addGraphDataset();
        }
    };

    private BroadcastReceiver syncAuthenticationFailureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new AlertDialog.Builder(context)
                    .setTitle("Security Check!")
                    .setMessage(getString(R.string.auth_failure_text))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            sessionManager.logoutMerchant();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(true)
                    .show();
        }
    };

    private BroadcastReceiver syncStartedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNewLogin && !getIntent().getBooleanExtra(MerchantSignUpActivity.KEY_SIGN_UP, false)) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setIndeterminate(true);
                progressDialog.setTitle("Please wait...");
                progressDialog.setMessage("Your records are being updated");
                progressDialog.show();
            } else {
                Snackbar.make(mLayout, R.string.records_to_be_updated_notice,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    };

    private void recordNewSalesWithoutPos(Bundle extras) {
        DBMerchantLoyaltyProgram loyaltyProgram = databaseHelper.getProgramById(extras.getLong(MerchantBackOffice.LOYALTY_PROGRAM_ID), sessionManager.getMerchantId());
        Bundle data = new Bundle();
        data.putString(LOYALTY_PROGRAM_TYPE, loyaltyProgram.getProgram_type());
        data.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());

        Intent intent = new Intent(mContext, RecordDirectSalesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtras(data);
        startActivity(intent);
    }

    private boolean isLoyaltyProgramCreated() {
        return loyaltyPrograms.size() > 0;
    }

    @Override
    protected void onDestroy() {
        if (mixPanel != null) {
            mixPanel.flush();
        }
        super.onDestroy();
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
    public void syncData() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(MerchantBackOffice.this);
        Account[] accounts = accountManager.getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
        String merchantEmail = sessionManager.getMerchantEmail().replace("\"", "");
        for (Account acc: accounts) {
            if (acc.name.equals(merchantEmail)) {
                account = acc;
            }
        }
        if (account != null) {
            if (!SyncAdapter.isSyncActive(account, AccountGeneral.AUTHORITY)) {
                SyncAdapter.syncImmediately(account);
            }
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
                        //take user to app settings
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
        if (requestCode == RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM && resultCode == RESULT_OK) {
            recordNewSalesWithoutPos(data.getExtras());
        }
        else if (requestCode == RECORD_SALES_WITH_POS_CHOOSE_PROGRAM && resultCode == RESULT_OK) {
            Long programId = data.getLongExtra(LOYALTY_PROGRAM_ID, 0L);
            DBMerchantLoyaltyProgram loyaltyProgram = databaseHelper.getProgramById(programId, sessionManager.getMerchantId());

            if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                Intent intent = new Intent(mContext, RecordPointsSalesWithPosActivity.class);
                intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                startActivity(intent);
            }
            else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                Intent intent = new Intent(mContext, RecordStampsSalesWithPosActivity.class);
                intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                startActivity(intent);
            }
        }
        else if (requestCode == REQUEST_PERMISSION_SETTING && resultCode == RESULT_OK) {
            MerchantBackOfficePermissionsDispatcher.syncDataWithCheck(MerchantBackOffice.this);
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MerchantBackOfficePermissionsDispatcher.onRequestPermissionsResult(MerchantBackOffice.this, requestCode, grantResults);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    private class MyAxisValueFormatter implements IAxisValueFormatter, IValueFormatter {

        private String mFormat;

        private MyAxisValueFormatter() {
            mFormat = "%s %s";
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return String.format(mFormat, merchantCurrencySymbol, value);
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return String.format(mFormat, merchantCurrencySymbol, Math.round(value));
        }
    }
}
