package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.OrderBelongsToFragment;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.TimeUtils;

import static co.loystar.loystarbusiness.activities.MerchantBackOffice.CUSTOMER_ID;
import static co.loystar.loystarbusiness.activities.MerchantBackOffice.CUSTOMER_PHONE_NUMBER;
import static co.loystar.loystarbusiness.activities.MerchantBackOffice.LOYALTY_PROGRAM_ID;
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.ADD_NEW_CUSTOMER_REQUEST;

/** Intent Extras
 * Long mProgramId => preselected loyalty program ID
 * Long mSelectedCustomerId => preselected customer ID (optional)
 * */
public class OrderBelongsToActivity extends AppCompatActivity
        implements OrderBelongsToFragment.OnOrderBelongsToFragmentInteractionListener{

    private static final String ORDER_BELONGS_TO_FRAGMENT = "orderBelongsToFragment";
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private Context mContext;
    private android.support.v4.app.FragmentManager mFragmentManager = getSupportFragmentManager();
    private BottomBar bottomNavigationBar;
    private Long mProgramId;
    private boolean isSimplePoints = false;
    private boolean isStamps = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_belongs_to);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SessionManager sessionManager = new SessionManager(this);
        mContext = this;
        mProgramId = getIntent().getLongExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, 0L);
        Long mSelectedCustomerId = getIntent().getLongExtra(RecordDirectSalesActivity.CUSTOMER_ID, 0L);
        DBMerchantLoyaltyProgram program = databaseHelper.getProgramById(mProgramId, sessionManager.getMerchantId());

        if (program != null) {
            if (program.getProgram_type().equals(getString(R.string.simple_points))) {
                isSimplePoints = true;
            }
            else if (program.getProgram_type().equals(getString(R.string.stamps_program))) {
                isStamps = true;
            }
        }


        if (savedInstanceState == null) {
            Bundle bundle = new Bundle();
            bundle.putLong(RecordDirectSalesActivity.CUSTOMER_ID, mSelectedCustomerId);
            Fragment fragment = new OrderBelongsToFragment();
            fragment.setArguments(bundle);
            mFragmentManager.beginTransaction().add(R.id.activity_order_belongs_to_container, fragment, ORDER_BELONGS_TO_FRAGMENT).commit();
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationBar = (BottomBar) findViewById(R.id.activity_order_belongs_to_bottom_navigation_bar);
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

        OrderBelongsToFragment orderBelongsToFragment = (OrderBelongsToFragment) mFragmentManager.findFragmentByTag(ORDER_BELONGS_TO_FRAGMENT);

        if ((orderBelongsToFragment != null && orderBelongsToFragment.isVisible())
                 && bottomNavigationBar != null) {
            bottomNavigationBar.selectTabWithId(R.id.record_sale);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {

                Bundle extras = new Bundle();
                extras.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mProgramId);
                extras.putLong(CUSTOMER_ID, data.getLongExtra(AddNewCustomerActivity.CUSTOMER_ID, 0L));


                if (isSimplePoints) {
                    Intent recordSalesIntent = new Intent(mContext, RecordPointsSalesWithPosActivity.class);
                    recordSalesIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    recordSalesIntent.putExtras(extras);
                    startActivity(recordSalesIntent);
                }

                else if (isStamps) {
                    Intent recordSalesIntent = new Intent(mContext, RecordStampsSalesWithPosActivity.class);
                    recordSalesIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    recordSalesIntent.putExtras(extras);
                    startActivity(recordSalesIntent);
                }
            }
        }
    }

    @Override
    public void onOrderBelongsFragmentInteraction(Long customerId) {
        DBCustomer customer = databaseHelper.getCustomerById(customerId);
        if (customer == null) {
            Intent addCustomerIntent = new Intent(mContext, AddNewCustomerActivity.class);
            startActivityForResult(addCustomerIntent, ADD_NEW_CUSTOMER_REQUEST);
        }
        else {
            Bundle data = new Bundle();
            data.putLong(LOYALTY_PROGRAM_ID, mProgramId);
            data.putLong(CUSTOMER_ID, customer.getId());


            if (isSimplePoints) {
                Intent recordSalesIntent = new Intent(mContext, RecordPointsSalesWithPosActivity.class);
                recordSalesIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                recordSalesIntent.putExtras(data);
                startActivity(recordSalesIntent);
            }

            else if (isStamps) {
                Intent recordSalesIntent = new Intent(mContext, RecordStampsSalesWithPosActivity.class);
                recordSalesIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                recordSalesIntent.putExtras(data);
                startActivity(recordSalesIntent);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
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
