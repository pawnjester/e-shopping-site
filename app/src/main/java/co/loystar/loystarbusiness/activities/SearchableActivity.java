package co.loystar.loystarbusiness.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.DividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;

public class SearchableActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String TAG = SearchableActivity.class.getSimpleName();

    private Context mContext;
    private SearchResultAdapter mAdapter;
    private ArrayList<CustomerEntity> mEntities = new ArrayList<>();
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;

    private SearchView mSearchView;
    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);
        Toolbar toolbar = findViewById(R.id.activity_searchable_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mAdapter = new SearchResultAdapter(mEntities);
        mDatabaseManager = DatabaseManager.getInstance(this);
        mSessionManager = new SessionManager(this);

        EmptyRecyclerView recyclerView = findViewById(R.id.searchable_activity_rv);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);

        onNewIntent(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_SEARCH)) {
                mQuery = intent.getStringExtra(SearchManager.QUERY);
                doSearch(mQuery);
            } else if (action.equals(Intent.ACTION_VIEW)) {
                Uri data = intent.getData();
                if (data != null) {
                    String customerId = data.getLastPathSegment();
                    CustomerEntity customerEntity = mDatabaseManager.getCustomerById(Integer.parseInt(customerId));

                    if (customerEntity != null) {
                        Intent customerDetailIntent = new Intent(mContext, CustomerListActivity.class);
                        customerDetailIntent.putExtra(Constants.CUSTOMER_ID, customerEntity.getId());
                        startActivity(customerDetailIntent);
                    }
                }
            }
        }
    }

    private void doSearch(String mQuery) {
        List<CustomerEntity> customerEntityList = mDatabaseManager.searchCustomersByNameOrNumber(mQuery, mSessionManager.getMerchantId());
        mEntities.clear();
        mEntities.addAll(customerEntityList);
        mAdapter.notifyDataSetChanged();
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.searchable_activity_rv_empty_list);

        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(mContext, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        recyclerView.setEmptyView(emptyView);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                CustomerEntity customerEntity = mAdapter.mCustomerEntities.get(position);
                Intent customerDetailIntent = new Intent(mContext, CustomerListActivity.class);
                customerDetailIntent.putExtra(Constants.CUSTOMER_ID, customerEntity.getId());
                startActivity(customerDetailIntent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

        private ArrayList<CustomerEntity> mCustomerEntities;

        SearchResultAdapter(ArrayList<CustomerEntity> customerEntities) {
            mCustomerEntities = customerEntities;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView mCustomerName;
            private TextView mCustomerNumber;

            ViewHolder(View itemView) {
                super(itemView);
                mCustomerName = itemView.findViewById(R.id.customer_name);
                mCustomerNumber = itemView.findViewById(R.id.customer_phone_number);
            }
        }

        @Override
        public SearchResultAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.searchable_customer_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SearchResultAdapter.ViewHolder holder, int position) {
            CustomerEntity customerEntity = mCustomerEntities.get(position);
            String lastName;
            if (customerEntity.getLastName() == null) {
                lastName = "";
            } else {
                lastName = customerEntity.getLastName();
            }
            String name = customerEntity.getFirstName() + " " + lastName;

            holder.mCustomerName.setText(name);
            holder.mCustomerNumber.setText(customerEntity.getPhoneNumber());
        }

        @Override
        public int getItemCount() {
            return mCustomerEntities.size();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.searchview_in_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
        setupSearchView(searchItem);

        if (mQuery != null) {
            mSearchView.setQuery(mQuery, false);
        }

        return true;
    }

    private void setupSearchView(MenuItem searchItem) {

        mSearchView.setIconifiedByDefault(false);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        mSearchView.setOnQueryTextListener(this);
        mSearchView.setFocusable(false);
        mSearchView.setFocusableInTouchMode(false);
        mSearchView.setIconifiedByDefault(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(SearchableActivity.this, MerchantBackOfficeActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
