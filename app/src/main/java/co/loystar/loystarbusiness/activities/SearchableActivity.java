package co.loystar.loystarbusiness.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.CustomerListRecyclerViewAdapter;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerViewOnItemClickListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SimpleDividerItemDecoration;

/**
 * Created by ordgen on 7/4/17.
 */

public class SearchableActivity extends AppCompatActivity implements RecyclerViewOnItemClickListener {
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private Context mContext;
    private EmptyRecyclerView mRecyclerView;
    private CustomerListRecyclerViewAdapter recyclerViewAdapter;
    private ArrayList<DBCustomer> customerArrayList;
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private View emptyView;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mContext = this;
        sessionManager = new SessionManager(mContext);
        mRecyclerView = (EmptyRecyclerView) findViewById(R.id.searchable_activity_recycler_view);
        emptyView = findViewById(R.id.searchable_activity_rv_empty_list);
        mLayoutManager = new LinearLayoutManager(this);

        /*Get the intent, verify the action and get the query*/
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            customerArrayList = databaseHelper.searchCustomersByNameOrNumber(query, sessionManager.getMerchantId());
            recyclerViewAdapter = new CustomerListRecyclerViewAdapter(mContext, customerArrayList, this);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(recyclerViewAdapter);
            mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mContext));
            mRecyclerView.setEmptyView(emptyView);
        }
        else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
           /* Handle a suggestions click (because the suggestions all use ACTION_VIEW)*/
            Uri data = intent.getData();
            String customerId = data.getLastPathSegment();
            DBCustomer user = databaseHelper.getCustomerById(Long.valueOf(customerId));

            /*setup adapter to handle on back pressed*/
            customerArrayList = new ArrayList<>();
            customerArrayList.add(user);
            recyclerViewAdapter = new CustomerListRecyclerViewAdapter(mContext, customerArrayList, this);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(recyclerViewAdapter);
            mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mContext));
            mRecyclerView.setEmptyView(emptyView);

            Intent customerDetailIntent = new Intent(mContext, CustomerListActivity.class);
            customerDetailIntent.putExtra(MerchantBackOffice.CUSTOMER_ID, Long.valueOf(customerId));
            startActivity(customerDetailIntent);
        }
    }

    public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query =
                    intent.getStringExtra(SearchManager.QUERY);
            customerArrayList = databaseHelper.searchCustomersByNameOrNumber(query, sessionManager.getMerchantId());
            recyclerViewAdapter = new CustomerListRecyclerViewAdapter(mContext, customerArrayList, this);
            if (mRecyclerView != null) {
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(recyclerViewAdapter);
                mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mContext));
                mRecyclerView.setEmptyView(emptyView);
            }
        }
        else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            String customerId = data.getLastPathSegment();
            Intent customerDetailIntent = new Intent(mContext, CustomerListActivity.class);
            customerDetailIntent.putExtra(MerchantBackOffice.CUSTOMER_ID, Long.valueOf(customerId));
            startActivity(customerDetailIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_searchable_activity, menu);

        /*Get the SearchView and set the searchable configuration*/
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(SearchableActivity.this, MerchantBackOffice.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onItemClicked(int position) {
        DBCustomer customer = customerArrayList.get(position);
        if (customer != null) {
            Intent customerDetailIntent = new Intent(mContext, CustomerListActivity.class);
            customerDetailIntent.putExtra(MerchantBackOffice.CUSTOMER_ID, customer.getId());
            startActivity(customerDetailIntent);
        }
    }
}
