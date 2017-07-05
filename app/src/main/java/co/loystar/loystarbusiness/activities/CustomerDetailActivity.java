package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.CustomerDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.LoystarApplication;

import static co.loystar.loystarbusiness.activities.CustomerListActivity.CUSTOMER_ID;

/**
 * Created by ordgen on 7/4/17.
 */

public class CustomerDetailActivity extends AppCompatActivity implements CustomerDetailFragment.OnCustomerDetailInteractionListener {
    private DBCustomer mItem;
    private Long customerId;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private CustomerDetailFragment mCustomerDetailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

        }

        customerId = getIntent().getLongExtra(CustomerDetailFragment.ARG_ITEM_ID, 0L);
        mItem = databaseHelper.getCustomerById(customerId);

        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        if (appBarLayout != null && mItem != null) {
            String fullCustomerName = mItem.getFirst_name() + " " + mItem.getLast_name();
            appBarLayout.setTitle(fullCustomerName);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_create_white_48px));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mItem != null) {
                        Intent intent = new Intent(CustomerDetailActivity.this, EditCustomerDetailsActivity.class);
                        intent.putExtra(CustomerListActivity.CUSTOMER_ID, customerId);
                        startActivity(intent);
                    }
                }
            });
        }

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putLong(CustomerDetailFragment.ARG_ITEM_ID, customerId);
            mCustomerDetailFragment = new CustomerDetailFragment();
            mCustomerDetailFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.customer_detail_container, mCustomerDetailFragment)
                    .commit();
        }


        //Track Screen views
        /*Answers.getInstance().logContentView(new ContentViewEvent().putContentName("CustomerDetailScreen").putContentType("Activity")
                .putContentId("CustomerDetailScreen")
                .putCustomAttribute("Time of the Day ", "Not set")
                .putCustomAttribute("Screen Orientation", "Not set"));*/
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

    @Override
    public void onCustomerDetailInteraction(Long customerId) {
        Intent intent = new Intent(CustomerDetailActivity.this, EditCustomerDetailsActivity.class);
        intent.putExtra(CUSTOMER_ID, customerId);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CustomerDetailFragment.RECORD_SALES_WITH_POS_CHOOSE_PROGRAM) {
            if (resultCode == RESULT_OK) {
                mCustomerDetailFragment.onActivityResult(requestCode, resultCode, data);
            }
        }
        else if (requestCode == CustomerDetailFragment.RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM) {
            if (resultCode == RESULT_OK) {
                mCustomerDetailFragment.onActivityResult(requestCode, resultCode, data);
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
