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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.CustomerListAdapter;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.fragments.CustomerDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.Customer;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.DownloadCustomerList;
import co.loystar.loystarbusiness.utils.EventBus.CustomerDetailFragmentEventBus;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.DividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EndlessRecyclerViewScrollListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.WrapContentLinearLayoutManager;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.dialogs.MyAlertDialog;
import io.requery.Persistable;
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

@RuntimePermissions
public class CustomerListActivity extends RxAppCompatActivity implements
    DialogInterface.OnClickListener,
    SearchView.OnQueryTextListener,
    CustomerListAdapter.ItemClickListener,
    CustomerListAdapter.RetryLoadMoreListener {

    private boolean mTwoPane;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 104;
    private static final int REQUEST_CHOOSE_PROGRAM = 110;
    private final String KEY_RECYCLER_STATE = "recycler_state";
    private Bundle mBundleRecyclerViewState;

    @BindView(R.id.customer_list_wrapper)
    View mLayout;

    @BindView(R.id.customers_rv)
    EmptyRecyclerView recyclerView;

    private EmptyRecyclerView mRecyclerView;
    private Context mContext;
    private SessionManager mSessionManager;
    private CustomerListAdapter mAdapter;
    private ReactiveEntityStore<Persistable> mDataStore;
    private MyAlertDialog myAlertDialog;
    private Customer mSelectedCustomer;
    private int customerId;
    private String searchFilterText;
    private MerchantEntity merchantEntity;
    private int limit = 20;
    private int currentTotalItemsCount = 0;
    private int currentPage;
    private int totalPages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);
        setSupportActionBar(findViewById(R.id.toolbar));

        ButterKnife.bind(this);

        //searchFilterText = getString(R.string.all_contacts);
        myAlertDialog = new MyAlertDialog();
        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        mAdapter = new CustomerListAdapter(this, this, this);
        mAdapter.set(getInitialCustomerData());

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
                if (!mAdapter.getDataList().isEmpty()) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(CustomerDetailFragment.ARG_ITEM_ID, mAdapter.getDataList().get(0).getId());
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

        setupRecyclerView(recyclerView);

        mDataStore.count(CustomerEntity.class)
            .where(CustomerEntity.DELETED.notEqual(true))
            .and(CustomerEntity.OWNER.eq(merchantEntity))
            .get()
            .consume(totalItems -> {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    if (totalItems == 1) {
                        actionBar.setTitle(getString(R.string.customer_count, "1"));
                    } else {
                        actionBar.setTitle(getString(R.string.customers_count, String.valueOf(totalItems)));
                    }
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
                double getNumberOfPages = Math.floor((double) totalItems / limit);
                totalPages = (int) getNumberOfPages + 1;
        });

        boolean customerUpdated = getIntent().getBooleanExtra(Constants.CUSTOMER_UPDATE_SUCCESS, false);

        if (customerUpdated) {
            showSnackbar(R.string.customer_update_success);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.empty_items_container);
        ImageView stateImageView = emptyView.findViewById(R.id.stateImage);
        TextView stateIntroTextView = emptyView.findViewById(R.id.stateIntroText);
        TextView stateDescriptionTextView = emptyView.findViewById(R.id.stateDescriptionText);
        BrandButtonNormal stateActionBtn = emptyView.findViewById(R.id.stateActionBtn);

        stateImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_nocustomers));
        stateIntroTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateIntroTextView.setTextColor(ContextCompat.getColor(this, R.color.wallet_hint_foreground_holo_dark));
        stateDescriptionTextView.setText(getString(R.string.no_customers_found));
        stateDescriptionTextView.setTextColor(ContextCompat.getColor(this, R.color.wallet_hint_foreground_holo_dark));

        stateActionBtn.setText(getString(R.string.start_adding_customers_label));
        stateActionBtn.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, AddNewCustomerActivity.class);
            startActivity(intent);
        });

        mRecyclerView = recyclerView;
        mRecyclerView.setHasFixedSize(true);

        // use WrapContentLinearLayoutManager to fix
        // https://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in
        LinearLayoutManager mLayoutManager = new WrapContentLinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, LinearLayoutManager.VERTICAL));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setEmptyView(emptyView);
        mRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                currentTotalItemsCount = totalItemsCount;
                currentPage = page;
                loadNextCustomerData();
            }
        });
    }

    private ArrayList<CustomerEntity> getInitialCustomerData() {
        Selection<ReactiveResult<CustomerEntity>> customerSelection = mDataStore.select(CustomerEntity.class);
        customerSelection.where(CustomerEntity.DELETED.notEqual(true));
        customerSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
        customerSelection.orderBy(CustomerEntity.FIRST_NAME.upper().asc());
        customerSelection.limit(limit);

        return new ArrayList<>(customerSelection.get().toList());
    }

    private void loadNextCustomerData() {
        ArrayList<CustomerEntity> nextEntities;
        if (TextUtils.isEmpty(searchFilterText)) {
            Selection<ReactiveResult<CustomerEntity>> customerSelection = mDataStore.select(CustomerEntity.class);
            customerSelection.where(CustomerEntity.DELETED.notEqual(true));
            customerSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
            customerSelection.orderBy(CustomerEntity.FIRST_NAME.upper().asc());
            customerSelection.limit(currentTotalItemsCount + limit);
            nextEntities = new ArrayList<>(customerSelection.get().toList());
        } else {
            String query = searchFilterText.substring(0, 1).equals("0") ? searchFilterText.substring(1) : searchFilterText;
            String searchQuery = "%" + query.toLowerCase() + "%";
            if (TextUtilsHelper.isInteger(searchFilterText)) {
                Selection<ReactiveResult<CustomerEntity>> phoneSelection = mDataStore.select(CustomerEntity.class);
                phoneSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                phoneSelection.where(CustomerEntity.DELETED.notEqual(true));
                phoneSelection.where(CustomerEntity.PHONE_NUMBER.like(searchQuery));
                phoneSelection.limit(currentTotalItemsCount + limit);
                nextEntities =  new ArrayList<>(phoneSelection.orderBy(CustomerEntity.FIRST_NAME.upper().asc()).get().toList());
            } else {
                Selection<ReactiveResult<CustomerEntity>> nameSelection = mDataStore.select(CustomerEntity.class);
                nameSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                nameSelection.where(CustomerEntity.DELETED.notEqual(true));
                nameSelection.where(CustomerEntity.FIRST_NAME.like(searchQuery));
                nameSelection.limit(currentTotalItemsCount + limit);
                nextEntities =  new ArrayList<>(nameSelection.orderBy(CustomerEntity.FIRST_NAME.upper().asc()).get().toList());
            }

        }

        if (totalPages == currentPage || nextEntities.size() < limit) {
            mAdapter.onReachEnd();
        } else {
            mAdapter.set(nextEntities);
        }
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
                        mAdapter.notifyItemChanged(mAdapter.getDataList().indexOf(customerEntity));
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
                    if (!AccountGeneral.isAccountActive(mContext)) {
                        Snackbar.make(mLayout,
                                "Your subscription has expired, update subscription to add a customer",
                                Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                            Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                            startActivity(intent);
                        }).show();
                    } else {
                        Intent addCustomerIntent = new Intent(mContext, AddNewCustomerActivity.class);
                        startActivityForResult(addCustomerIntent, Constants.ADD_NEW_CUSTOMER_REQUEST);
                    }
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
            searchFilterText = null;
            mAdapter.set(getInitialCustomerData());
        }
        else {
            searchFilterText = newText;
            String query = searchFilterText.substring(0, 1).equals("0") ? searchFilterText.substring(1) : searchFilterText;
            String searchQuery = "%" + query.toLowerCase() + "%";
            ArrayList<CustomerEntity> customerEntities;
            if (TextUtilsHelper.isInteger(searchFilterText)) {
                Selection<ReactiveResult<CustomerEntity>> phoneSelection = mDataStore.select(CustomerEntity.class);
                phoneSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                phoneSelection.where(CustomerEntity.DELETED.notEqual(true));
                phoneSelection.where(CustomerEntity.PHONE_NUMBER.like(searchQuery));
                phoneSelection.limit(limit);
                customerEntities =  new ArrayList<>(phoneSelection.orderBy(CustomerEntity.FIRST_NAME.upper().asc()).get().toList());
            } else {
                Selection<ReactiveResult<CustomerEntity>> nameSelection = mDataStore.select(CustomerEntity.class);
                nameSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                nameSelection.where(CustomerEntity.DELETED.notEqual(true));
                nameSelection.where(CustomerEntity.FIRST_NAME.like(searchQuery)).or(CustomerEntity.LAST_NAME.like(searchQuery));
                nameSelection.limit(limit);
                customerEntities =  new ArrayList<>(nameSelection.orderBy(CustomerEntity.FIRST_NAME.upper().asc()).get().toList());
            }
            mAdapter.set(customerEntities);
        }
        mRecyclerView.scrollToPosition(0);
        return true;
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

        CustomerDetailFragmentEventBus
                .getInstance()
                .getFragmentEventObservable()
                .compose(bindToLifecycle())
                .subscribe(bundle -> {
                    if (bundle.getInt(Constants.FRAGMENT_EVENT_ID, 0) == CustomerDetailFragmentEventBus.ACTION_START_SALE) {
                        customerId = bundle.getInt(Constants.CUSTOMER_ID, 0);
                        startSale();
                    }
                });
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
            case R.id.download_customer_list:
                CustomerListActivityPermissionsDispatcher.startCustomerListDownloadWithCheck(CustomerListActivity.this);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                            arguments.putInt(CustomerDetailFragment.ARG_ITEM_ID, customer.getId());
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
               startSaleWithoutPos(programId);
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
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
                        if (isPosTurnedOn) {
                            startSaleWithPos();
                        } else {
                            startSaleWithoutPos(loyaltyProgramEntity.getId());
                        }
                    } else {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
                        if (isPosTurnedOn) {
                            startSaleWithPos();
                        } else {
                            chooseProgram();
                        }
                    }
                });
    }

    private void chooseProgram() {
        Intent intent = new Intent(this, ChooseProgramActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_PROGRAM);
    }

    private void startSaleWithPos() {
        Intent intent = new Intent(this, SaleWithPosActivity.class);
        intent.putExtra(Constants.CUSTOMER_ID, customerId);
        startActivity(intent);
    }

    private void startSaleWithoutPos(int programId) {
        Intent intent = new Intent(this, SaleWithoutPosActivity.class);
        intent.putExtra(Constants.CUSTOMER_ID, customerId);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startLoyaltyProgram() {
        Intent intent = new Intent(mContext, LoyaltyProgramListActivity.class);
        intent.putExtra(Constants.CREATE_LOYALTY_PROGRAM, true);
        startActivity(intent);
    }

    @Override
    public void onItemClick(View v, int position) {
        mSelectedCustomer = mAdapter.getDataList().get(position);
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

    @Override
    public void onLongItemClick(View v, int position) {
        mSelectedCustomer = mAdapter.getDataList().get(position);
        if (mSelectedCustomer != null) {
            myAlertDialog.setTitle("Are you sure?");
            myAlertDialog.setMessage("All sales records for " + mSelectedCustomer.getFirstName() + " will be deleted as well.");
            myAlertDialog.setPositiveButton(getString(R.string.confirm_delete_positive), CustomerListActivity.this);
            myAlertDialog.setNegativeButtonText(getString(R.string.confirm_delete_negative));
            myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);
        }
    }

    @Override
    public void onRetryLoadMore() {

    }
}
