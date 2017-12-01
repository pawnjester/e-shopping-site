package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.joda.time.DateTime;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.CustomerItemBinding;
import co.loystar.loystarbusiness.fragments.CustomerDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.Customer;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.DownloadCustomerList;
import co.loystar.loystarbusiness.utils.EventBus.CustomerDetailFragmentEventBus;
import co.loystar.loystarbusiness.utils.ui.MyAlertDialog;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.DividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.support.v4.app.NavUtils.navigateUpFromSameTask;

/**
 * An activity representing a list of Customers. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link CustomerDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
@RuntimePermissions
public class CustomerListActivity extends RxAppCompatActivity
        implements DialogInterface.OnClickListener, SearchView.OnQueryTextListener {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 104;
    private static final int REQUEST_CHOOSE_PROGRAM = 110;
    private static final String TAG = CustomerListActivity.class.getSimpleName();
    private final String KEY_RECYCLER_STATE = "recycler_state";
    private Bundle mBundleRecyclerViewState;

    private EmptyRecyclerView mRecyclerView;
    private Context mContext;
    private View mLayout;
    private ExecutorService executor;
    private SessionManager mSessionManager;
    private CustomerListAdapter mAdapter;
    private ReactiveEntityStore<Persistable> mDataStore;
    private MyAlertDialog myAlertDialog;
    private Customer mSelectedCustomer;
    private Toolbar toolbar;
    private String searchFilterText;
    private MerchantEntity merchantEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

        toolbar = findViewById(R.id.customer_list_toolbar);

        searchFilterText = getString(R.string.all_contacts);
        myAlertDialog = new MyAlertDialog();
        mLayout = findViewById(R.id.customer_list_wrapper);
        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        mAdapter = new CustomerListAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        if (findViewById(R.id.customer_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        if (getIntent().hasExtra(Constants.CUSTOMER_ID)){
            int customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
            if (mTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putInt(CustomerDetailFragment.ARG_ITEM_ID, customerId);
                CustomerDetailFragment customerDetailFragment = new CustomerDetailFragment();
                customerDetailFragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.customer_detail_container, customerDetailFragment)
                        .commit();
            } else {
                Intent intent = new Intent(mContext, CustomerDetailActivity.class);
                intent.putExtra(CustomerDetailFragment.ARG_ITEM_ID, customerId);
                startActivity(intent);
            }
        }
        else {
            if (mTwoPane) {
                Result<CustomerEntity> customerEntities = mAdapter.performQuery();
                if (!customerEntities.toList().isEmpty()) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(CustomerDetailFragment.ARG_ITEM_ID, customerEntities.first().getId());
                    CustomerDetailFragment customerDetailFragment = new CustomerDetailFragment();
                    customerDetailFragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.customer_detail_container, customerDetailFragment)
                            .commit();
                }
            }
        }

        FloatingActionButton addCustomer = findViewById(R.id.activity_customer_list_fab_add_customer);
        FloatingActionButton rewardCustomer = findViewById(R.id.activity_customer_list_fab_rewards);
        FloatingActionButton sendAnnouncement = findViewById(R.id.activity_customer_list_fab_send_blast);


        addCustomer.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_person_add_white_24px));
        rewardCustomer.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_loyalty_white_24px));
        sendAnnouncement.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_megaphone));

        addCustomer.setOnClickListener(clickListener);
        rewardCustomer.setOnClickListener(clickListener);
        sendAnnouncement.setOnClickListener(clickListener);

        EmptyRecyclerView recyclerView = findViewById(R.id.customers_rv);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);

        boolean customerUpdated = getIntent().getBooleanExtra(Constants.CUSTOMER_UPDATE_SUCCESS, false);

        if (customerUpdated) {
            showSnackbar(R.string.customer_update_success);
            mAdapter.queryAsync();
        }

        if (toolbar != null) {
            Result<CustomerEntity> result = mAdapter.performQuery();
            toolbar.setTitle(getString(R.string.customers_count, String.valueOf(result.toList().size()))
            );
        }
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.empty_items_container);
        ImageView stateWelcomeImageView = emptyView.findViewById(R.id.stateImage);
        TextView stateWelcomeTextView = emptyView.findViewById(R.id.stateIntroText);
        TextView stateDescriptionTextView = emptyView.findViewById(R.id.stateDescriptionText);
        BrandButtonNormal stateActionBtn = emptyView.findViewById(R.id.stateActionBtn);

        stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_nocustomers));
        stateWelcomeTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateWelcomeTextView.setTextColor(ContextCompat.getColor(this, R.color.wallet_hint_foreground_holo_dark));
        stateDescriptionTextView.setText(getString(R.string.no_customers_found));
        stateDescriptionTextView.setTextColor(ContextCompat.getColor(this, R.color.wallet_hint_foreground_holo_dark));

        stateActionBtn.setText(getString(R.string.start_adding_customers_label));
        stateActionBtn.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, AddNewCustomerActivity.class);
            startActivity(intent);
        });

        mRecyclerView = recyclerView;
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, LinearLayoutManager.VERTICAL));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setEmptyView(emptyView);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                CustomerItemBinding customerItemBinding = (CustomerItemBinding) view.getTag();
                if (customerItemBinding != null) {
                    mSelectedCustomer = customerItemBinding.getCustomer();
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putInt(CustomerDetailFragment.ARG_ITEM_ID, mSelectedCustomer.getId());
                        CustomerDetailFragment customerDetailFragment = new CustomerDetailFragment();
                        customerDetailFragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.customer_detail_container, customerDetailFragment)
                                .commit();
                    } else {
                        Intent intent = new Intent(mContext, CustomerDetailActivity.class);
                        intent.putExtra(CustomerDetailFragment.ARG_ITEM_ID, mSelectedCustomer.getId());
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                CustomerItemBinding customerItemBinding = (CustomerItemBinding) view.getTag();
                if (customerItemBinding != null) {
                    mSelectedCustomer = customerItemBinding.getCustomer();
                    if (mSelectedCustomer != null) {
                        myAlertDialog.setTitle("Are you sure?");
                        myAlertDialog.setMessage("All sales records for " + mSelectedCustomer.getFirstName() + " will be deleted as well.");
                        myAlertDialog.setPositiveButton(getString(R.string.confirm_delete_positive), CustomerListActivity.this);
                        myAlertDialog.setNegativeButtonText(getString(R.string.confirm_delete_negative));
                        myAlertDialog.setDialogIcon(AppCompatResources.getDrawable(mContext, android.R.drawable.ic_dialog_alert));
                        myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);
                    }
                }
            }
        }));
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case BUTTON_NEGATIVE:
                dialogInterface.dismiss();
                break;
            case BUTTON_POSITIVE:
                myAlertDialog.dismiss();
                if (mSelectedCustomer != null) {
                    CustomerEntity customerEntity = mDataStore.findByKey(CustomerEntity.class, mSelectedCustomer.getId()).blockingGet();
                    if (customerEntity != null) {
                        customerEntity.setDeleted(true);
                        mDataStore.update(customerEntity).subscribe(/*no-op*/);
                        mAdapter.queryAsync();
                        SyncAdapter.performSync(mContext, mSessionManager.getEmail());

                        String deleteText =  mSelectedCustomer.getFirstName() + " has been deleted!";
                        Snackbar.make(mLayout, deleteText, Snackbar.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.activity_customer_list_fab_add_customer:
                    Intent addCustomerIntent = new Intent(mContext, AddNewCustomerActivity.class);
                    startActivityForResult(addCustomerIntent, Constants.ADD_NEW_CUSTOMER_REQUEST);
                    break;
                case R.id.activity_customer_list_fab_rewards:
                    Intent rewardCustomerIntent = new Intent(mContext, RewardCustomersActivity.class);
                    startActivity(rewardCustomerIntent);
                    break;
                case R.id.activity_customer_list_fab_send_blast:
                    Intent sendBlastIntent = new Intent(mContext, MessageBroadcastActivity.class);
                    startActivity(sendBlastIntent);
                    break;
            }
        }
    };

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            searchFilterText = getString(R.string.all_contacts);
            ((CustomerListAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
        }
        else {
            ((CustomerListAdapter) mRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        mRecyclerView.scrollToPosition(0);
        return true;
    }

    private class CustomerListAdapter extends
            QueryRecyclerAdapter<CustomerEntity, BindingHolder<CustomerItemBinding>> implements Filterable {

        private Filter filter;

        CustomerListAdapter() {
            super(CustomerEntity.$TYPE);
        }

        @Override
        public Result<CustomerEntity> performQuery() {
            if (merchantEntity == null) {
                return null;
            }

            if (searchFilterText.equals(getString(R.string.all_contacts))) {
                Selection<ReactiveResult<CustomerEntity>> customerSelection = mDataStore.select(CustomerEntity.class);
                customerSelection.where(CustomerEntity.DELETED.notEqual(true));
                customerSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                return customerSelection.orderBy(CustomerEntity.FIRST_NAME.asc()).get();
            } else if (searchFilterText.equals(getString(R.string.last_visit_30_days))) {
                List<Integer> customerIds = new ArrayList<>();
                Selection<ReactiveResult<SalesTransactionEntity>> resultSelection = mDataStore.select(SalesTransactionEntity.class);
                resultSelection.where(SalesTransactionEntity.MERCHANT.eq(merchantEntity));
                resultSelection.where(SalesTransactionEntity.CREATED_AT.greaterThanOrEqual(new Timestamp((new DateTime().minusDays(30)).getMillis())));
                for (SalesTransactionEntity transactionEntity: resultSelection.get().toList()) {
                    customerIds.add(transactionEntity.getCustomer().getId());
                }

                Selection<ReactiveResult<CustomerEntity>> customerSelection = mDataStore.select(CustomerEntity.class);
                customerSelection.where(CustomerEntity.DELETED.notEqual(true));
                customerSelection.where(CustomerEntity.ID.in(customerIds));
                return customerSelection.orderBy(CustomerEntity.FIRST_NAME.asc()).get();
            } else if (searchFilterText.equals(getString(R.string.male_only))) {
                Selection<ReactiveResult<CustomerEntity>> maleSelection = mDataStore.select(CustomerEntity.class);
                maleSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                maleSelection.where(CustomerEntity.DELETED.notEqual(true));
                maleSelection.where(CustomerEntity.SEX.eq("M"));
                return maleSelection.orderBy(CustomerEntity.FIRST_NAME.asc()).get();
            } else if (searchFilterText.equals(getString(R.string.female_only))) {
                Selection<ReactiveResult<CustomerEntity>> femaleSelection = mDataStore.select(CustomerEntity.class);
                femaleSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                femaleSelection.where(CustomerEntity.DELETED.notEqual(true));
                femaleSelection.where(CustomerEntity.SEX.eq("F"));
                return femaleSelection.orderBy(CustomerEntity.FIRST_NAME.asc()).get();
            } else {
                String query = searchFilterText.substring(0, 1).equals("0") ? searchFilterText.substring(1) : searchFilterText;
                String searchQuery = "%" + query.toLowerCase() + "%";
                if (TextUtilsHelper.isInteger(searchFilterText)) {
                    Selection<ReactiveResult<CustomerEntity>> phoneSelection = mDataStore.select(CustomerEntity.class);
                    phoneSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                    phoneSelection.where(CustomerEntity.DELETED.notEqual(true));
                    phoneSelection.where(CustomerEntity.PHONE_NUMBER.like(searchQuery));
                    return phoneSelection.orderBy(CustomerEntity.FIRST_NAME.asc()).get();
                } else {
                    Selection<ReactiveResult<CustomerEntity>> nameSelection = mDataStore.select(CustomerEntity.class);
                    nameSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                    nameSelection.where(CustomerEntity.DELETED.notEqual(true));
                    nameSelection.where(CustomerEntity.FIRST_NAME.like(searchQuery));
                    return nameSelection.orderBy(CustomerEntity.FIRST_NAME.asc()).get();
                }
            }
        }

        @Override
        public void onBindViewHolder(CustomerEntity item, BindingHolder<CustomerItemBinding> holder, int position) {
            holder.binding.setCustomer(item);
        }

        @Override
        public BindingHolder<CustomerItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            CustomerItemBinding binding = CustomerItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            return new BindingHolder<>(binding);
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter =  new CustomerFilter(new ArrayList<>(mAdapter.performQuery().toList()));
            }
            return filter;
        }

        private class CustomerFilter extends Filter {
            private ArrayList<CustomerEntity> mCustomers;

            CustomerFilter(ArrayList<CustomerEntity> customers) {
                mCustomers = new ArrayList<>();
                synchronized (this) {
                    mCustomers.addAll(customers);
                }
            }

            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults result = new FilterResults();
                String searchString = charSequence.toString();
                if (!TextUtils.isEmpty(searchString)) {
                    if (searchString.equals(getString(R.string.all_contacts))) {
                        searchFilterText = getString(R.string.all_contacts);
                    } else if (searchString.equals(getString(R.string.last_visit_30_days))) {
                        searchFilterText = getString(R.string.last_visit_30_days);
                    } else if (searchString.equals(getString(R.string.male_only))) {
                        searchFilterText = getString(R.string.male_only);
                    } else if (searchString.equals(getString(R.string.female_only))) {
                        searchFilterText = getString(R.string.female_only);
                    } else {
                        searchFilterText = searchString;
                    }
                    result.count = 0;
                    result.values = new ArrayList<>();
                }
                else {
                    synchronized (this) {
                        result.count = mCustomers.size();
                        result.values = mCustomers;
                    }
                }
                return result;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mAdapter.queryAsync();
            }
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*save RecyclerView state*/
        mBundleRecyclerViewState = new Bundle();
        Parcelable listState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        mBundleRecyclerViewState.putParcelable(KEY_RECYCLER_STATE, listState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*restore RecyclerView state*/
        if (mBundleRecyclerViewState != null) {
            Parcelable listState = mBundleRecyclerViewState.getParcelable(KEY_RECYCLER_STATE);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(listState);
        }

        mAdapter.queryAsync();
        CustomerDetailFragmentEventBus
                .getInstance()
                .getFragmentEventObservable()
                .compose(bindToLifecycle())
                .subscribe(integer -> {
                    if (integer == CustomerDetailFragmentEventBus.ACTION_START_SALE) {
                        startSale();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mAdapter.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_customer_list_activity, menu);

        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                navigateUpFromSameTask(this);
                return true;
            case R.id.filter_customer_list:
                showFilterMenu(toolbar);
                return true;
            case R.id.download_customer_list:
                CustomerListActivityPermissionsDispatcher.startCustomerListDownloadWithCheck(CustomerListActivity.this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFilterMenu(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_filter_customer, popup.getMenu());
        popup.setOnMenuItemClickListener(new FilterMenuClickListener());
        popup.setGravity(Gravity.END);
        popup.show();
    }

    private class FilterMenuClickListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.all_contacts_filter:
                    ((CustomerListAdapter) mRecyclerView.getAdapter()).getFilter().filter(getString(R.string.all_contacts));
                    return true;
                case R.id.last_visit_filter:
                    ((CustomerListAdapter) mRecyclerView.getAdapter()).getFilter().filter(getString(R.string.last_visit_30_days));
                    return true;
                case R.id.male_only_filter:
                    ((CustomerListAdapter) mRecyclerView.getAdapter()).getFilter().filter(getString(R.string.male_only));
                    return true;
                case R.id.female_only_filter:
                    ((CustomerListAdapter) mRecyclerView.getAdapter()).getFilter().filter(getString(R.string.female_only));
                    return true;
            }
            return false;
        }
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void startCustomerListDownload() {
        DownloadCustomerList downloadCustomerList = new DownloadCustomerList(CustomerListActivity.this);
        downloadCustomerList.execute();
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showRationaleForWriteExternalStorage(final PermissionRequest request) {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.permission_write_external_storage_rationale)
                .setPositiveButton(R.string.button_allow, (dialogInterface, i) -> request.proceed())
                .setNegativeButton(R.string.button_deny, (dialogInterface, i) -> request.cancel())
                .setCancelable(false)
                .show();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDeniedForWriteExternalStorage() {
        showSnackbar(R.string.permission_write_external_storage__denied);
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showNeverAskForWriteExternalStorage() {
        Snackbar.make(mLayout, R.string.permission_write_external_storage_never_ask,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.button_allow, view -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CustomerListActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    CustomerEntity customer = mDataStore.findByKey(CustomerEntity.class, extras.getInt(Constants.CUSTOMER_ID, 0)).blockingGet();
                    if (customer != null) {
                        if (mTwoPane) {
                            Bundle arguments = new Bundle();
                            arguments.putLong(CustomerDetailFragment.ARG_ITEM_ID, customer.getId());
                            CustomerDetailFragment customerDetailFragment = new CustomerDetailFragment();
                            customerDetailFragment.setArguments(arguments);
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.customer_detail_container, customerDetailFragment)
                                    .commit();
                        } else {
                            Intent intent = new Intent(mContext, CustomerDetailActivity.class);
                            intent.putExtra(CustomerDetailFragment.ARG_ITEM_ID, customer.getId());
                            startActivity(intent);
                        }
                    }
                }
            }
        } else if (requestCode == REQUEST_CHOOSE_PROGRAM) {
           if (resultCode == RESULT_OK) {
               int programId = data.getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
               LoyaltyProgramEntity loyaltyProgramEntity = mDataStore.findByKey(LoyaltyProgramEntity.class, programId).blockingGet();
               initiateSalesProcess(loyaltyProgramEntity);
           }
        }
    }

    private void startSale() {
        mDataStore.count(LoyaltyProgramEntity.class)
                .get()
                .single()
                .toObservable()
                .compose(bindToLifecycle())
                .subscribe(integer -> {
                    if (integer == 0) {
                        new AlertDialog.Builder(mContext)
                                .setTitle("No Loyalty Program Found!")
                                .setMessage("To record a sale, you would have to start a loyalty program.")
                                .setPositiveButton(mContext.getString(R.string.start_loyalty_program_btn_label), (dialog, which) -> {
                                    dialog.dismiss();
                                    startLoyaltyProgram();
                                })
                                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss()).show();
                    } else if (integer == 1) {
                        LoyaltyProgramEntity loyaltyProgramEntity = merchantEntity.getLoyaltyPrograms().get(0);
                        initiateSalesProcess(loyaltyProgramEntity);
                    } else {
                        chooseProgram();
                    }
                });
    }

    private void initiateSalesProcess(@NonNull LoyaltyProgramEntity loyaltyProgramEntity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
        if (isPosTurnedOn) {
            if (loyaltyProgramEntity.getProgramType().equals(getString(R.string.simple_points))) {
                startPointsSaleWithPos(loyaltyProgramEntity.getId());
            } else if (loyaltyProgramEntity.getProgramType().equals(getString(R.string.stamps_program))) {
                startStampsSaleWithPos(loyaltyProgramEntity.getId());
            }
        } else {
            startSaleWithoutPos(loyaltyProgramEntity.getId());
        }
    }

    private void chooseProgram() {
        Intent intent = new Intent(this, ChooseProgramActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_PROGRAM);
    }

    private void startPointsSaleWithPos(int programId) {
        Intent intent = new Intent(this, PointsSaleWithPosActivity.class);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startStampsSaleWithPos(int programId) {
        Intent intent = new Intent(this, StampsSaleWithPosActivity.class);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startSaleWithoutPos(int programId) {
        Intent intent = new Intent(this, SaleWithoutPosActivity.class);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startLoyaltyProgram() {
        Intent intent = new Intent(mContext, LoyaltyProgramListActivity.class);
        intent.putExtra(Constants.CREATE_LOYALTY_PROGRAM, true);
        startActivity(intent);
    }
}
