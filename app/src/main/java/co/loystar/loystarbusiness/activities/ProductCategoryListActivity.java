package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.ProductCategoryItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductCategory;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.ui.MyAlertDialog;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

public class ProductCategoryListActivity extends RxAppCompatActivity implements DialogInterface.OnClickListener {
    private static final String TAG = ProductCategoryListActivity.class.getSimpleName();
    private final String KEY_RECYCLER_STATE = "recycler_state";
    private Bundle mBundleRecyclerViewState;
    private ReactiveEntityStore<Persistable> mDataStore;
    private EmptyRecyclerView mRecyclerView;
    private Context mContext;
    private View mLayout;
    private ProductCategoryListAdapter mAdapter;
    private ExecutorService executor;
    private SessionManager mSessionManager;
    private MyAlertDialog myAlertDialog;
    private ProductCategory mSelectedCategory;
    private ApiClient mApiClient;
    private MerchantEntity merchantEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_category_list);

        Toolbar toolbar = findViewById(R.id.activity_product_category_list_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mLayout = findViewById(R.id.activity_product_category_list_container);
        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        myAlertDialog = new MyAlertDialog();
        mApiClient = new ApiClient(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        mAdapter = new ProductCategoryListAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        FloatingActionButton fab = findViewById(R.id.activity_product_category_list_fab);
        fab.setOnClickListener(view -> {
            createNewCategory();
        });

        EmptyRecyclerView recyclerView = findViewById(R.id.product_category_list_rv);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.product_category_list_empty_container);
        ImageView stateWelcomeImageView = emptyView.findViewById(R.id.stateImage);
        TextView stateWelcomeTextView = emptyView.findViewById(R.id.stateIntroText);
        TextView stateDescriptionTextView = emptyView.findViewById(R.id.stateDescriptionText);
        BrandButtonNormal stateActionBtn = emptyView.findViewById(R.id.stateActionBtn);

        stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_categories));
        stateWelcomeTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateDescriptionTextView.setText(getString(R.string.no_product_categories_found));

        stateActionBtn.setText(getString(R.string.start_adding_product_categories_label));
        stateActionBtn.setOnClickListener(view -> {
            createNewCategory();
        });

        ChipsLayoutManager layoutManager = ChipsLayoutManager.newBuilder(mContext).build();
        mRecyclerView = recyclerView;
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(getResources().getDimensionPixelOffset(R.dimen.item_space),
                getResources().getDimensionPixelOffset(R.dimen.item_space)));
        mRecyclerView.setEmptyView(emptyView);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                ProductCategoryItemBinding productCategoryItemBinding = (ProductCategoryItemBinding) view.getTag();
                if (productCategoryItemBinding != null) {
                    mSelectedCategory = productCategoryItemBinding.getProductCategory();
                    myAlertDialog.setTitle("Are you sure?");
                    myAlertDialog.setMessage("All products associated with this category will be deleted as well. ");
                    myAlertDialog.setPositiveButton(getString(R.string.confirm_delete_positive), ProductCategoryListActivity.this);
                    myAlertDialog.setNegativeButtonText(getString(R.string.confirm_delete_negative));
                    myAlertDialog.setDialogIcon(AppCompatResources.getDrawable(mContext, android.R.drawable.ic_dialog_alert));
                    myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);
                }
            }

            @Override
            public void onLongClick(View view, int position) {

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
                if (mSelectedCategory != null) {
                    ProductCategoryEntity productCategoryEntity = mDataStore.findByKey(ProductCategoryEntity.class, mSelectedCategory.getId()).blockingGet();
                    if (productCategoryEntity != null) {
                        ProgressDialog progressDialog = new ProgressDialog(this);
                        progressDialog.setMessage(getString(R.string.a_moment));
                        progressDialog.setIndeterminate(true);
                        progressDialog.show();

                        mApiClient.getLoystarApi(false).setMerchantProductCategoryDeleteFlagToTrue(productCategoryEntity.getId()).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                                if (response.isSuccessful()) {
                                    mDataStore.delete(productCategoryEntity)
                                            .subscribeOn(Schedulers.newThread())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .compose(bindToLifecycle()).subscribe(new CompletableObserver() {
                                        @Override
                                        public void onSubscribe(Disposable d) {}

                                        @Override
                                        public void onComplete() {
                                            mAdapter.queryAsync();
                                            SyncAdapter.performSync(mContext, mSessionManager.getEmail());
                                            String deleteText =  mSelectedCategory.getName() + " has been deleted!";
                                            Snackbar.make(mLayout, deleteText, Snackbar.LENGTH_LONG).show();
                                            mSelectedCategory = null;
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            showSnackbar(R.string.error_delete_failed);
                                        }
                                    });
                                } else if (response.code() == 401){
                                    AccountManager accountManager = AccountManager.get(mContext);
                                    accountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mSessionManager.getAccessToken());
                                } else {
                                    showSnackbar(R.string.error_delete_failed);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                showSnackbar(R.string.unknown_error);
                            }
                        });
                    }
                }
                break;
        }
    }

    private class ProductCategoryListAdapter extends QueryRecyclerAdapter<ProductCategoryEntity, BindingHolder<ProductCategoryItemBinding>> {
        ProductCategoryListAdapter() {
            super(ProductCategoryEntity.$TYPE);
        }

        @Override
        public Result<ProductCategoryEntity> performQuery() {
            if (merchantEntity == null) {
                return null;
            }

            Selection<ReactiveResult<ProductCategoryEntity>> selection = mDataStore.select(ProductCategoryEntity.class);
            selection.where(ProductCategoryEntity.OWNER.eq(merchantEntity));
            selection.where(ProductCategoryEntity.DELETED.notEqual(true));

            return selection.orderBy(ProductCategoryEntity.UPDATED_AT.desc()).get();
        }

        @Override
        public void onBindViewHolder(ProductCategoryEntity item, BindingHolder<ProductCategoryItemBinding> holder, int position) {
            holder.binding.setProductCategory(item);
            holder.binding.categoryChip.setClosable(true);
            holder.binding.categoryChip.setChipText(item.getName());
        }

        @Override
        public BindingHolder<ProductCategoryItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ProductCategoryItemBinding binding = ProductCategoryItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            return new BindingHolder<>(binding);
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

        mAdapter.queryAsync();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mAdapter.close();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createNewCategory() {
        LayoutInflater li = LayoutInflater.from(this);
        View createCategoryView = li.inflate(R.layout.add_product_category, null);
        EditText msgBox = createCategoryView.findViewById(R.id.category_text_box);
        TextView charCounterView = createCategoryView.findViewById(R.id.category_name_char_counter);

        RxTextView.textChangeEvents(msgBox).subscribe(textViewTextChangeEvent -> {
            CharSequence s = textViewTextChangeEvent.text();
            String char_temp = "%s %s / %s";
            String char_temp_unit = s.length() == 1 ? "Character" : "Characters";
            String char_counter_text = String.format(char_temp, s.length(), char_temp_unit, 30);
            charCounterView.setText(char_counter_text);
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(createCategoryView);
        alertDialogBuilder.setTitle("Create new category");
        alertDialogBuilder.setPositiveButton("Create", (dialogInterface, i) -> {
            if (TextUtils.isEmpty(msgBox.getText().toString())) {
                msgBox.setError(getString(R.string.error_name_required));
                msgBox.requestFocus();
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.a_moment));
            progressDialog.setIndeterminate(true);
            progressDialog.show();

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", msgBox.getText().toString());
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObject);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                mApiClient.getLoystarApi(false).addProductCategory(requestBody).enqueue(new Callback<co.loystar.loystarbusiness.models.databinders.ProductCategory>() {
                    @Override
                    public void onResponse(@NonNull Call<co.loystar.loystarbusiness.models.databinders.ProductCategory> call, @NonNull Response<co.loystar.loystarbusiness.models.databinders.ProductCategory> response) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        dialogInterface.cancel();
                        if (response.isSuccessful()) {
                            co.loystar.loystarbusiness.models.databinders.ProductCategory productCategory = response.body();
                            if (productCategory == null) {
                                Toast.makeText(mContext, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                            } else {
                                ProductCategoryEntity productCategoryEntity = new ProductCategoryEntity();
                                productCategoryEntity.setId(productCategory.getId());
                                productCategoryEntity.setDeleted(false);
                                productCategoryEntity.setName(productCategory.getName());
                                productCategoryEntity.setCreatedAt(new Timestamp(productCategory.getCreated_at().getMillis()));
                                productCategoryEntity.setUpdatedAt(new Timestamp(productCategory.getUpdated_at().getMillis()));
                                productCategoryEntity.setOwner(merchantEntity);

                                mDataStore.insert(productCategoryEntity).subscribe(/*no-op*/);
                                mAdapter.queryAsync();

                                Toast.makeText(mContext, getString(R.string.product_category_create_success), Toast.LENGTH_LONG).show();
                            }
                        } else if (response.code() == 401) {
                            AccountManager accountManager = AccountManager.get(mContext);
                            accountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mSessionManager.getAccessToken());
                        } else {
                            Toast.makeText(mContext, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<co.loystar.loystarbusiness.models.databinders.ProductCategory> call, @NonNull Throwable t) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(mContext, getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        alertDialogBuilder.setCancelable(false);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
