package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Filter;
import android.widget.Filterable;

import com.github.clans.fab.FloatingActionButton;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
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
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
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
public class CustomerListActivity extends AppCompatActivity
        implements DialogInterface.OnClickListener, SearchView.OnQueryTextListener {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 104;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        searchFilterText = getString(R.string.all_contacts);
        myAlertDialog = new MyAlertDialog();
        mLayout = findViewById(R.id.customer_list_wrapper);
        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);

        mAdapter = new CustomerListAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.customers_count, mDataStore.count(CustomerEntity.class).get().value()));
        }

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
                if (mDataStore.count(CustomerEntity.class).get().value() > 0) {
                    Result<CustomerEntity> customerEntities = mAdapter.performQuery();
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
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.customer_list_empty_container);
        BrandButtonNormal addBtn = emptyView.findViewById(R.id.no_customer_add_customer_btn);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent(mContext, AddFarmerActivity.class);
                startActivity(intent);*/
            }
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
                    Log.e(TAG, "onClick: " );
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
                    /*Intent addCustomerIntent = new Intent(mContext, AddNewCustomerActivity.class);
                    startActivityForResult(addCustomerIntent, ADD_NEW_CUSTOMER_REQUEST);*/
                    break;
                case R.id.activity_customer_list_fab_rewards:
                    /*Intent rewardCustomerIntent = new Intent(mContext, RewardCustomersActivity.class);
                    startActivity(rewardCustomerIntent);*/
                    break;
                case R.id.activity_customer_list_fab_send_blast:
                    /*Intent sendBlastIntent = new Intent(mContext, SendSMSBroadcast.class);
                    startActivity(sendBlastIntent);*/
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
            MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                    .where(MerchantEntity.ID.eq(mSessionManager.getMerchantId()))
                    .get()
                    .firstOrNull();

            if (merchantEntity == null) {
                return null;
            }

            if (searchFilterText.equals(getString(R.string.all_contacts))) {
                Selection<ReactiveResult<CustomerEntity>> customerSelection = mDataStore.select(CustomerEntity.class);
                customerSelection.where(CustomerEntity.DELETED.notEqual(true));
                customerSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                return customerSelection.orderBy(CustomerEntity.UPDATED_AT.desc()).get();
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
                return customerSelection.orderBy(CustomerEntity.UPDATED_AT.desc()).get();
            } else if (searchFilterText.equals(getString(R.string.male_only))) {
                Selection<ReactiveResult<CustomerEntity>> maleSelection = mDataStore.select(CustomerEntity.class);
                maleSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                maleSelection.where(CustomerEntity.DELETED.notEqual(true));
                maleSelection.where(CustomerEntity.SEX.eq("M"));
                return maleSelection.get();
            } else if (searchFilterText.equals(getString(R.string.female_only))) {
                Selection<ReactiveResult<CustomerEntity>> femaleSelection = mDataStore.select(CustomerEntity.class);
                femaleSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                femaleSelection.where(CustomerEntity.DELETED.notEqual(true));
                femaleSelection.where(CustomerEntity.SEX.eq("F"));
                return femaleSelection.get();
            } else {
                String query = searchFilterText.substring(0, 1).equals("0") ? searchFilterText.substring(1) : searchFilterText;
                String searchQuery = "%" + query.toLowerCase() + "%";
                if (TextUtilsHelper.isInteger(searchFilterText)) {
                    Selection<ReactiveResult<CustomerEntity>> phoneSelection = mDataStore.select(CustomerEntity.class);
                    phoneSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                    phoneSelection.where(CustomerEntity.DELETED.notEqual(true));
                    phoneSelection.where(CustomerEntity.PHONE_NUMBER.like(searchQuery));
                    return phoneSelection.get();
                } else {
                    Selection<ReactiveResult<CustomerEntity>> nameSelection = mDataStore.select(CustomerEntity.class);
                    nameSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                    nameSelection.where(CustomerEntity.DELETED.notEqual(true));
                    nameSelection.where(CustomerEntity.FIRST_NAME.like(searchQuery));
                    return nameSelection.get();
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
                filter =  new CustomerFilter<>(new ArrayList<>(mAdapter.performQuery().toList()));
            }
            return filter;
        }

        private class CustomerFilter<T> extends Filter {
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
        DownloadCustomerList downloadCustomerList = new DownloadCustomerList();
        downloadCustomerList.execute();
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showRationaleForWriteExternalStorage(final PermissionRequest request) {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.permission_write_external_storage_rationale)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
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

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDeniedForWriteExternalStorage() {
        showSnackbar(R.string.permission_write_external_storage__denied);
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showNeverAskForWriteExternalStorage() {
        Snackbar.make(mLayout, R.string.permission_write_external_storage_neverask,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.button_allow, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }
                })
                .show();
    }

    private class DownloadCustomerList extends AsyncTask<String, Integer, Boolean> {
        private final ProgressDialog dialog = new ProgressDialog(mContext);

        @Override
        protected void onPreExecute() {

            this.dialog.setMessage("Exporting customer list...");
            this.dialog.show();

        }

        @Override
        protected Boolean doInBackground(String... params) {

            String fileName = "MyLoystarCustomerList.xls";
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + File.separator + "Loystar");

            if (!directory.exists()) {
                directory.mkdirs();
            }

            //file path
            File file = new File(directory, fileName);

            WorkbookSettings wbSettings = new WorkbookSettings();
            wbSettings.setLocale(new Locale("en", "EN"));
            WritableWorkbook workbook;
            try {
                workbook = Workbook.createWorkbook(file, wbSettings);
                WritableSheet sheet = workbook.createSheet("MyLoystarCustomerList", 0);
                try {
                    sheet.addCell(new Label(0, 0, "First Name")); //column and row
                    sheet.addCell(new Label(1, 0, "Last Name"));
                    sheet.addCell(new Label(2, 0, "Phone Number"));
                    sheet.addCell(new Label(3, 0, "Email"));
                    sheet.addCell(new Label(4, 0, "Gender"));
                    sheet.addCell(new Label(5, 0, "Total Points"));
                    sheet.addCell(new Label(6, 0, "Total Stamps"));
                    sheet.addCell(new Label(7, 0, "Date of Birth"));

                    int index = 1;

                    Result<CustomerEntity> customerEntities = mAdapter.performQuery();
                    for (CustomerEntity customer: customerEntities.toList()) {
                        Date dt = customer.getDateOfBirth();
                        String date = "";
                        if (dt != null) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(dt);
                            date = TextUtilsHelper.getFormattedDateString(calendar);
                        }

                        DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
                        int customer_stamps = databaseManager.getTotalUserStampsForMerchant(mSessionManager.getMerchantId(), customer.getId());
                        int customer_points = databaseManager.getTotalUserPointsForMerchant(mSessionManager.getMerchantId(), customer.getId());

                        sheet.addCell(new Label(0, index, customer.getFirstName()));
                        sheet.addCell(new Label(1, index, customer.getLastName()));
                        sheet.addCell(new Label(2, index, customer.getPhoneNumber()));
                        sheet.addCell(new Label(3, index, customer.getEmail()));
                        sheet.addCell(new Label(4, index, customer.getSex()));
                        sheet.addCell(new Label(5, index, String.valueOf(customer_points)));
                        sheet.addCell(new Label(6, index, String.valueOf(customer_stamps)));
                        sheet.addCell(new Label(7, index, date));

                        index += 1;
                    }

                }  catch (RowsExceededException e) {
                    e.printStackTrace();
                    return false;
                }catch (WriteException e) {
                    e.printStackTrace();
                    return false;
                }
                workbook.write();
                try {
                    workbook.close();
                } catch (WriteException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (this.dialog.isShowing()){
                this.dialog.dismiss();
            }
            if (success){
                String fileName = "MyLoystarCustomerList.xls";
                File sdCard = Environment.getExternalStorageDirectory();
                File filePath = new File(sdCard.getAbsolutePath() + File.separator + "Loystar");
                final File file = new File(filePath, fileName);
                final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(".XLS");

                new AlertDialog.Builder(mContext)
                        .setTitle("Export successful!")
                        .setMessage("Click the button below to open your file or open 'MyLoystarCustomerList.xls' later from inside the Loystar folder on your phone. Thanks.")
                        .setPositiveButton(getString(R.string.open_file), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                PackageManager packageManager = getPackageManager();
                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(file), mime);
                                List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                                if (list.size() > 0) {
                                    startActivity(intent);
                                } else {
                                    new AlertDialog.Builder(mContext)
                                            .setTitle("Sorry! No Excel reader found.")
                                            .setMessage("We couldn't open the file because you don't have an Excel reader installed.")
                                            .setPositiveButton("Download Excel Reader", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(Intent.ACTION_VIEW)
                                                            .setData(Uri.parse("https://play.google.com/store/apps/details?id=cn.wps.moffice_eng&hl=en"));
                                                    startActivity(intent);
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                            }
                        })
                        .show();
            }
            else {
                showSnackbar(R.string.error_export_failed);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CustomerListActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
}
