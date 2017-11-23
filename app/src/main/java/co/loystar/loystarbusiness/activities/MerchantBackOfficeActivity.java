package co.loystar.loystarbusiness.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.fragments.ChartFragment;
import co.loystar.loystarbusiness.fragments.HomeEmptyStateFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;


public class MerchantBackOfficeActivity extends AppCompatActivity {
    private static final String TAG = MerchantBackOfficeActivity.class.getCanonicalName();
    private SessionManager mSessionManager;
    private Context mContext;
    private View mLayout;
    private ReactiveEntityStore<Persistable> mDataStore;
    private BottomBar bottomNavigationBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_back_office);
        Toolbar toolbar = findViewById(R.id.activity_merchant_back_office_toolbar);
        setSupportActionBar(toolbar);

        mLayout = findViewById(R.id.merchant_back_office_wrapper);

        mContext = this;
        mSessionManager = new SessionManager(this);
        mDataStore = DatabaseManager.getDataStore(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mSessionManager.getBusinessName().substring(0, 1).toUpperCase() + mSessionManager.getBusinessName().substring(1));
        }



        if (savedInstanceState == null) {
            int totalLoyaltyPrograms = mDataStore.count(LoyaltyProgramEntity.class).get().value();
            int totalSales =  mDataStore.count(SalesTransactionEntity.class).get().value();
            if (totalLoyaltyPrograms == 0 || totalSales == 0) {
                HomeEmptyStateFragment emptyStateFragment = new HomeEmptyStateFragment();
                Bundle bundle = new Bundle();
                if (totalLoyaltyPrograms == 0) {
                    bundle.putString(Constants.STATE_TYPE, Constants.NO_LOYALTY_PROGRAM);
                } else  {
                    bundle.putString(Constants.STATE_TYPE, Constants.NO_SALES_TRANSACTIONS);
                }
                emptyStateFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction().replace(R.id.merchant_back_office_container, emptyStateFragment).commit();
            } else {
                ChartFragment chartFragment = new ChartFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.merchant_back_office_container, chartFragment).commit();
            }
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationBar = findViewById(R.id.bottom_navigation_bar);
        bottomNavigationBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.record_sale:
                        /*boolean isPosTurnedOn = merchant.getTurn_on_point_of_sale() != null
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
                        }*/
                        break;
                    case R.id.customers:
                        Intent intent = new Intent(mContext, CustomerListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.campaigns:
                        Intent loyaltyIntent = new Intent(mContext, LoyaltyProgramListActivity.class);
                        loyaltyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(loyaltyIntent);
                        break;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.merchant_back_office, menu);
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
                SyncAdapter.performSync(mContext, mSessionManager.getEmail());
                return true;
            case R.id.action_settings:
                Intent settings_intent = new Intent(mContext, SettingsActivity.class);
                startActivity(settings_intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(syncFinishedReceiver);
            unregisterReceiver(syncStartedReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(syncFinishedReceiver, new IntentFilter(Constants.SYNC_FINISHED));
        registerReceiver(syncStartedReceiver, new IntentFilter(Constants.SYNC_STARTED));

        if (bottomNavigationBar != null){
            bottomNavigationBar.selectTabWithId(R.id.home);
        }
    }

    private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            showSnackbar(R.string.records_updated_notice);
        }
    };

    private BroadcastReceiver syncStartedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            showSnackbar(R.string.records_to_be_updated_notice);
        }
    };
}
