package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.roughike.bottombar.BottomBar;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.AddPointsFragment;
import co.loystar.loystarbusiness.fragments.SaleWithoutPosFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.utils.Constants;

public class SaleWithoutPosActivity extends AppCompatActivity implements
        AddPointsFragment.OnAddPointsFragmentInteractionListener,
        SaleWithoutPosFragment.OnSaleWithoutPosFragmentInteractionListener {
    private static final int ADD_NEW_CUSTOMER_REQUEST = 122;
    private static final String SALE_WITHOUT_POS_FRAGMENT = "SALE_WITHOUT_POS_FRAGMENT";
    private int mLoyaltyProgramId;
    private int mCustomerId;
    private Fragment mFragment = null;
    private FragmentManager mFragmentManager = getSupportFragmentManager();
    private Context mContext;
    private LoyaltyProgramEntity mLoyaltyProgram;
    private BottomBar bottomNavigationBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_without_pos);
        Toolbar toolbar = findViewById(R.id.activity_sale_without_pos_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext  = this;
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(this);

        mLoyaltyProgramId = getIntent().getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
        mLoyaltyProgram = mDatabaseManager.getLoyaltyProgramById(mLoyaltyProgramId);
        mCustomerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra(Constants.ADD_POINTS, false)) {
                Bundle data = new Bundle();
                data.putInt(Constants.LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
                data.putInt(Constants.CUSTOMER_ID, mCustomerId);
                data.putInt(Constants.AMOUNT_SPENT, 0 );

                mFragment = new AddPointsFragment();
                mFragment.setArguments(data);
                mFragmentManager.beginTransaction().replace(R.id.activity_sale_without_pos_container, mFragment).commit();
            } else {
                mFragment = new SaleWithoutPosFragment();
                mFragmentManager.beginTransaction().replace(R.id.activity_sale_without_pos_container, mFragment, SALE_WITHOUT_POS_FRAGMENT).commit();
            }
        }
        setupBottomNavigation();
    }

    @Override
    public void onSaleWithoutPosFragmentInteraction(Bundle data) {
        if (mLoyaltyProgram != null) {
            if (mLoyaltyProgram.getProgramType().equals(getString(R.string.simple_points))) {
                addPoints(data);
            } else if (mLoyaltyProgram.getProgramType().equals(getString(R.string.stamps_program))) {
                addStamps(data);
            }
        }
    }

    private void addStamps(Bundle data) {
        Intent addStampsIntent = new Intent(mContext, AddStampsActivity.class);
        addStampsIntent.putExtra(Constants.AMOUNT_SPENT, Integer.parseInt(data.getString(Constants.AMOUNT_SPENT, "0")));
        addStampsIntent.putExtra(Constants.LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
        addStampsIntent.putExtra(Constants.CUSTOMER_ID, data.getInt(Constants.CUSTOMER_ID, 0));
        startActivity(addStampsIntent);
    }

    private void addPoints(Bundle data) {
        mCustomerId = data.getInt(Constants.CUSTOMER_ID, 0);
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constants.PRINT_RECEIPT, true);
        bundle.putInt(Constants.LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
        bundle.putInt(Constants.CUSTOMER_ID, mCustomerId);
        bundle.putInt(
                Constants.AMOUNT_SPENT,
                Integer.parseInt(data.getString(Constants.AMOUNT_SPENT, "0"))
        );

        mFragment = new AddPointsFragment();
        mFragment.setArguments(bundle);
        mFragmentManager.beginTransaction().replace(R.id.activity_sale_without_pos_container, mFragment).addToBackStack(null).commit();
    }

    @Override
    public void onAddPointsFragmentInteraction(Bundle data) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constants.SHOW_CONTINUE_BUTTON, true);
        bundle.putInt(Constants.LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
        bundle.putInt(Constants.CUSTOMER_ID, mCustomerId);

        Intent intent = new Intent(mContext, TransactionsConfirmation.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        bottomNavigationBar = findViewById(R.id.activity_sale_without_pos_bottom_bar);
        bottomNavigationBar.selectTabWithId(R.id.record_sale);
        bottomNavigationBar.setOnTabSelectListener(tabId -> {
            switch (tabId) {
                case R.id.home:
                    Intent homeIntent = new Intent(mContext, MerchantBackOfficeActivity.class);
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
                    Intent loyaltyIntent = new Intent(mContext, LoyaltyProgramListActivity.class);
                    loyaltyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loyaltyIntent);
                    break;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationBar != null) {
            bottomNavigationBar.selectTabWithId(R.id.record_sale);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add_customer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.add_customer_from_menu) {
            Intent addCustomerIntent = new Intent(mContext, AddNewCustomerActivity.class);
            startActivityForResult(addCustomerIntent, ADD_NEW_CUSTOMER_REQUEST);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                getSupportFragmentManager().beginTransaction().
                        remove(getSupportFragmentManager().findFragmentByTag(SALE_WITHOUT_POS_FRAGMENT)).commit();

                Bundle bundle = new Bundle();
                bundle.putInt(Constants.CUSTOMER_ID, data.getIntExtra(Constants.CUSTOMER_ID, 0));
                mFragment = new SaleWithoutPosFragment();
                mFragment.setArguments(bundle);
                mFragmentManager.beginTransaction().add(R.id.activity_sale_without_pos_container, mFragment, SALE_WITHOUT_POS_FRAGMENT).commit();
            }
        }
    }
}
