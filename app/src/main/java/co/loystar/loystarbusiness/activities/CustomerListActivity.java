package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.CustomerDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.MyAlertDialog.MyAlertDialog;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.ItemClickSupport;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SimpleDividerItemDecoration;
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
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.ADD_NEW_CUSTOMER_REQUEST;

/**
 * Created by ordgen on 7/4/17.
 */

@RuntimePermissions
public class CustomerListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener,
        CustomerDetailFragment.OnCustomerDetailInteractionListener, DialogInterface.OnClickListener {
    /*constants*/
    private final String KEY_RECYCLER_STATE = "recycler_state";
    private static Bundle mBundleRecyclerViewState;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 104;
    public static final int REQUEST_PERMISSION_SETTING_GET_ACCOUNTS = 102;
    public static final String CUSTOMER_ID = "customerId";
    public static final String CUSTOMER_UPDATE_SUCCESS = "customerUpdateSuccess";

    private boolean mTwoPane;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private RecyclerView.LayoutManager mLayoutManager;
    private EmptyRecyclerView mRecyclerView;
    private Context mContext;
    private ArrayList<DBCustomer> mCustomerList;
    private SimpleItemRecyclerViewAdapter mAdapter;
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private View mLayout;
    private MyAlertDialog myAlertDialog;
    private DBCustomer mSelectedCustomer;
    private int adapterPosition = -1;
    private CustomerDetailFragment mCustomerDetailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);
        mLayout = findViewById(R.id.customer_list_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mContext = this;
        myAlertDialog = new MyAlertDialog();
        mCustomerList = databaseHelper.listMerchantCustomers(sessionManager.getMerchantId());
        String tmt = "Customers (%s)";
        if (toolbar != null) {
            toolbar.setTitle(String.format(tmt, mCustomerList.size()));
        }
        Collections.sort(mCustomerList, new Comparator<DBCustomer>() {
            @Override
            public int compare(DBCustomer user1, DBCustomer user2) {
                return user1.getFirst_name().compareToIgnoreCase(user2.getFirst_name());
            }
        });

        setSupportActionBar(toolbar);


        FloatingActionButton addCustomer = (FloatingActionButton) findViewById(R.id.activity_customer_list_fab_add_customer);
        FloatingActionButton rewardCustomer = (FloatingActionButton) findViewById(R.id.activity_customer_list_fab_rewards);
        FloatingActionButton sendAnnouncement = (FloatingActionButton) findViewById(R.id.activity_customer_list_fab_send_blast);


        addCustomer.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_person_add_white_24px));
        rewardCustomer.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_loyalty_white_24px));
        sendAnnouncement.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_megaphone));

        addCustomer.setOnClickListener(clickListener);
        rewardCustomer.setOnClickListener(clickListener);
        sendAnnouncement.setOnClickListener(clickListener);

        boolean customerUpdated = getIntent().getBooleanExtra(CUSTOMER_UPDATE_SUCCESS, false);

        if (customerUpdated) {
            Snackbar.make(mLayout, getString(R.string.customer_update_success), Snackbar.LENGTH_LONG).show();
        }


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        View recyclerView = findViewById(R.id.customer_list);
        assert recyclerView != null;
        mRecyclerView = (EmptyRecyclerView) recyclerView;
        mLayoutManager = new LinearLayoutManager(this);


        setupRecyclerView(mRecyclerView);

        if (findViewById(R.id.customer_detail_container) != null) {
            mTwoPane = true;
        }

        if (getIntent() != null) {
            if (getIntent().hasExtra(MerchantBackOffice.CUSTOMER_ID)){
                Long customerId = getIntent().getLongExtra(MerchantBackOffice.CUSTOMER_ID, 0L);
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putLong(CustomerDetailFragment.ARG_ITEM_ID, customerId);
                    mCustomerDetailFragment = new CustomerDetailFragment();
                    mCustomerDetailFragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.customer_detail_container, mCustomerDetailFragment)
                            .commit();
                } else {
                    Intent intent = new Intent(mContext, CustomerDetailActivity.class);
                    intent.putExtra(CustomerDetailFragment.ARG_ITEM_ID, customerId);
                    startActivity(intent);
                }
            }
            else {
                if (mTwoPane) {
                    if (!mCustomerList.isEmpty()) {
                        Long firstCustomerId =  mCustomerList.get(0).getId();
                        Bundle arguments = new Bundle();
                        arguments.putLong(CustomerDetailFragment.ARG_ITEM_ID, firstCustomerId);
                        mCustomerDetailFragment = new CustomerDetailFragment();
                        mCustomerDetailFragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.customer_detail_container, mCustomerDetailFragment)
                                .commit();
                    }
                }
            }
        }
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
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.download_customer_list:
                CustomerListActivityPermissionsDispatcher.startCustomerListDownloadWithCheck(CustomerListActivity.this);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDeniedForWriteExternalStorage() {
        Snackbar.make(mLayout, R.string.permission_write_external_storage__denied, Snackbar.LENGTH_LONG).show();
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showNeverAskForWriteExternalStorage() {
        Snackbar.make(mLayout, R.string.permission_write_external_storage_neverask,
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
                        startActivityForResult(intent, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }
                })
                .show();
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
    public void syncData() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(CustomerListActivity.this);
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
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING_GET_ACCOUNTS);
                    }
                })
                .show();
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {

        View emptyView = findViewById(R.id.customer_list_empty_container);
        BrandButtonNormal addBtn = (BrandButtonNormal) emptyView.findViewById(R.id.no_customer_add_customer_btn);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, AddNewCustomerActivity.class);
                startActivityForResult(intent, ADD_NEW_CUSTOMER_REQUEST);
            }
        });

        mAdapter = new SimpleItemRecyclerViewAdapter(mCustomerList);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(
                CustomerListActivity.this
        ));
        recyclerView.setEmptyView(emptyView);

        ItemClickSupport itemClick = ItemClickSupport.addTo(recyclerView);
        itemClick.setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                adapterPosition = position;
                Long customerId = recyclerView.getAdapter().getItemId(position);
                mSelectedCustomer = databaseHelper.getCustomerById(customerId);
                if (mSelectedCustomer != null) {
                    myAlertDialog.setTitle("Are you sure?");
                    myAlertDialog.setMessage("All sales records for " + mSelectedCustomer.getFirst_name() + " will be deleted as well.");
                    myAlertDialog.setPositiveButton(getString(R.string.confirm_delete_positive), CustomerListActivity.this);
                    myAlertDialog.setNegativeButtonText(getString(R.string.confirm_delete_negative));
                    myAlertDialog.setDialogIcon(AppCompatResources.getDrawable(mContext, android.R.drawable.ic_dialog_alert));
                    myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);
                }
                return false;
            }
        });
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.activity_customer_list_fab_add_customer:
                    Intent addCustomerIntent = new Intent(mContext, AddNewCustomerActivity.class);
                    startActivityForResult(addCustomerIntent, ADD_NEW_CUSTOMER_REQUEST);
                    break;
                case R.id.activity_customer_list_fab_rewards:
                    Intent rewardCustomerIntent = new Intent(mContext, RewardCustomersActivity.class);
                    startActivity(rewardCustomerIntent);
                    break;
                case R.id.activity_customer_list_fab_send_blast:
                    Intent sendBlastIntent = new Intent(mContext, SendSMSBroadcast.class);
                    startActivity(sendBlastIntent);
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_customer_list_activity, menu);

        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (TextUtils.isEmpty(query)) {
            ((SimpleItemRecyclerViewAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
        }
        else {
            ((SimpleItemRecyclerViewAdapter) mRecyclerView.getAdapter()).getFilter().filter(query);
        }
        mRecyclerView.scrollToPosition(0);
        return true;
    }

    @Override
    public void onCustomerDetailInteraction(Long customerId) {
        Intent intent = new Intent(CustomerListActivity.this, EditCustomerDetailsActivity.class);
        intent.putExtra(CUSTOMER_ID, customerId);
        startActivity(intent);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case BUTTON_NEGATIVE:
                dialogInterface.dismiss();
                break;
            case BUTTON_POSITIVE:
                myAlertDialog.dismiss();
                if (mSelectedCustomer != null && adapterPosition > -1) {
                    mSelectedCustomer.setDeleted(true);
                    databaseHelper.updateCustomer(mSelectedCustomer);
                    mCustomerList.remove(adapterPosition);
                    mAdapter.notifyItemRemoved(adapterPosition);
                    mAdapter.notifyItemRangeChanged(adapterPosition, mCustomerList.size());
                    mAdapter.notifyDataSetChanged();
                    Snackbar.make(mLayout, mSelectedCustomer.getFirst_name() + " has been deleted!", Snackbar.LENGTH_LONG).show();

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            CustomerListActivityPermissionsDispatcher.syncDataWithCheck(CustomerListActivity.this);
                        }
                    }, 200);
                }
                break;
        }
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> implements Filterable {

        private ArrayList<DBCustomer> mValues;
        private Filter filter;

        SimpleItemRecyclerViewAdapter(ArrayList<DBCustomer> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.customer_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            String fullCustomerName = mValues.get(position).getFirst_name() + " " + mValues.get(position).getLast_name();
            holder.mItem = mValues.get(position);
            holder.mIdView.setText(fullCustomerName);
            holder.mContentView.setText(holder.mItem.getPhone_number());

            holder.mView.setOnClickListener(new View.OnClickListener() {
                Long customerId =  holder.mItem.getId();
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putLong(CustomerDetailFragment.ARG_ITEM_ID, customerId);
                        mCustomerDetailFragment = new CustomerDetailFragment();
                        mCustomerDetailFragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.customer_detail_container, mCustomerDetailFragment)
                                .commit();
                    } else {
                        Intent intent = new Intent(mContext, CustomerDetailActivity.class);
                        intent.putExtra(CustomerDetailFragment.ARG_ITEM_ID, customerId);
                        startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        @Override
        public long getItemId(int position) {
            return mValues.get(position).getId();
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter =  new CustomerFilter<>(mValues);
            }
            return filter;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            final TextView mIdView;
            final TextView mContentView;
            DBCustomer mItem;

            ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.customer_list_title);
                mContentView = (TextView) view.findViewById(R.id.customer_list_title_subtitle);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }

        private class CustomerFilter<T> extends Filter {
            private ArrayList<DBCustomer> mCustomers;

            CustomerFilter(ArrayList<DBCustomer> customers) {
                mCustomers = new ArrayList<>();
                synchronized (this) {
                    mCustomers.addAll(customers);
                }
            }

            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults result = new FilterResults();
                if (!TextUtils.isEmpty(charSequence.toString())) {
                    ArrayList<DBCustomer> iFilterList = databaseHelper.searchCustomersByNameOrNumber(
                            charSequence.toString(), sessionManager.getMerchantId());
                    result.count = iFilterList.size();
                    result.values = iFilterList;
                }
                else {
                    synchronized (this) {
                        result.count = mCustomers.size();
                        result.values = mCustomers;
                    }
                }
                return result;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                ArrayList<DBCustomer> filtered = (ArrayList<DBCustomer>) filterResults.values;
                if (filtered != null) {
                    animateTo(filtered);

                }
                else {
                    animateTo(mCustomers);
                }
            }
        }

        void animateTo(ArrayList<DBCustomer> customers) {
            applyAndAnimateRemovals(customers);
            applyAndAnimateAdditions(customers);
            applyAndAnimateMovedItems(customers);
        }

        private void applyAndAnimateRemovals(ArrayList<DBCustomer> newList) {
            for (int i = mCustomerList.size() - 1; i >= 0; i--) {
                final DBCustomer user = mCustomerList.get(i);
                if (!newList.contains(user)) {
                    removeItem(i);
                }
            }
        }

        private void applyAndAnimateAdditions(ArrayList<DBCustomer> newList) {
            for (int i = 0, count = newList.size(); i < count; i++) {
                final DBCustomer customer = newList.get(i);
                if (!mCustomerList.contains(customer)) {
                    addItem(i, customer);
                }
            }
        }

        private void applyAndAnimateMovedItems(ArrayList<DBCustomer> newList) {
            for (int toPosition = newList.size() - 1; toPosition >= 0; toPosition--) {
                final DBCustomer customer = newList.get(toPosition);
                final int fromPosition = mCustomerList.indexOf(customer);
                if (fromPosition >= 0 && fromPosition != toPosition) {
                    moveItem(fromPosition, toPosition);
                }
            }
        }

        DBCustomer removeItem(int position) {
            final DBCustomer model = mCustomerList.remove(position);
            notifyItemRemoved(position);
            return model;
        }

        void addItem(int position, DBCustomer customer) {
            mCustomerList.add(position, customer);
            notifyItemInserted(position);
        }

        void moveItem(int fromPosition, int toPosition) {
            final DBCustomer model = mCustomerList.remove(fromPosition);
            mCustomerList.add(toPosition, model);
            notifyItemMoved(fromPosition, toPosition);
        }
    }

    private class DownloadCustomerList extends AsyncTask<String, Integer, Boolean> {
        private final ProgressDialog dialog = new ProgressDialog(mContext);

        @Override
        protected void onPreExecute() {

            this.dialog.setMessage("Exporting customer mCustomerList...");
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

                    for (DBCustomer customer: mCustomerList) {
                        Date dt = customer.getDate_of_birth();
                        String date = "";
                        if (dt != null) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(dt);
                            date = TextUtilsHelper.getFormattedDateString(calendar);
                        }
                        int customer_stamps = databaseHelper.getTotalUserStampsForMerchant(customer.getUser_id(), sessionManager.getMerchantId());
                        int customer_points = databaseHelper.getTotalUserPointsForMerchant(customer.getUser_id(), sessionManager.getMerchantId());

                        sheet.addCell(new Label(0, index, customer.getFirst_name()));
                        sheet.addCell(new Label(1, index, customer.getLast_name()));
                        sheet.addCell(new Label(2, index, customer.getPhone_number()));
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
                Snackbar.make(mLayout, getString(R.string.error_export_failed), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CustomerListActivityPermissionsDispatcher.onRequestPermissionsResult(CustomerListActivity.this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PERMISSION_SETTING_GET_ACCOUNTS) {
            if (resultCode == RESULT_OK) {
                CustomerListActivityPermissionsDispatcher.syncDataWithCheck(CustomerListActivity.this);
            }
        }
        else if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (resultCode == RESULT_OK) {
                CustomerListActivityPermissionsDispatcher.startCustomerListDownloadWithCheck(CustomerListActivity.this);
            }
        }
        else if (requestCode == ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                DBCustomer customer = databaseHelper.getCustomerById(extras.getLong(AddNewCustomerActivity.CUSTOMER_ID, 0L));
                if (customer != null) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putLong(CustomerDetailFragment.ARG_ITEM_ID, customer.getId());
                        mCustomerDetailFragment = new CustomerDetailFragment();
                        mCustomerDetailFragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.customer_detail_container, mCustomerDetailFragment)
                                .commit();
                    } else {
                        Intent intent = new Intent(mContext, CustomerDetailActivity.class);
                        intent.putExtra(CustomerDetailFragment.ARG_ITEM_ID, customer.getId());
                        startActivity(intent);
                    }
                }
            }
        }
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
    }
}
