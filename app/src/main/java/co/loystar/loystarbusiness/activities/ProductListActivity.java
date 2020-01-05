package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.ProductItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Product;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

public class ProductListActivity
    extends BaseActivity implements SearchView.OnQueryTextListener {

    public static final String TAG = ProductListActivity.class.getSimpleName();
    private ReactiveEntityStore<Persistable> mDataStore;
    private final String KEY_RECYCLER_STATE = "recycler_state";
    private Bundle mBundleRecyclerViewState;

    private EmptyRecyclerView mRecyclerView;
    private Context mContext;
    private View mLayout;
    private String searchFilterText;
    private ProductsListAdapter mAdapter;
    private ExecutorService executor;
    private SessionManager mSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        FloatingActionButton fab = findViewById(R.id.activity_product_list_toolbar_fab);
        fab.setOnClickListener(view -> {
            if (!AccountGeneral.isAccountActive(mContext)) {
                Snackbar.make(mLayout,
                        "Your subscription has expired, update subscription to add a product",
                        Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                    Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                    startActivity(intent);
                }).show();
            } else {
                Intent intent = new Intent(mContext, AddProductActivity.class);
                intent.putExtra(Constants.ACTIVITY_INITIATOR, TAG);
                startActivity(intent);
            }
        });

        mLayout = findViewById(R.id.activity_product_list_container);
        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);

        mAdapter = new ProductsListAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        boolean productCreatedIntent = getIntent().getBooleanExtra(getString(R.string.product_create_success), false);
        if (productCreatedIntent) {
            Snackbar.make(mLayout, getString(R.string.product_create_success), Snackbar.LENGTH_LONG).show();
        }

        boolean productUpdatedIntent = getIntent().getBooleanExtra(getString(R.string.product_edit_success), false);
        if (productUpdatedIntent) {
            Snackbar.make(mLayout, getString(R.string.product_edit_success), Snackbar.LENGTH_LONG).show();
        }

        EmptyRecyclerView recyclerView = findViewById(R.id.products_list_rv);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.products_list_empty_container);
        ImageView stateImageView = emptyView.findViewById(R.id.stateImage);
        TextView stateIntroTextView = emptyView.findViewById(R.id.stateIntroText);
        TextView stateDescriptionTextView = emptyView.findViewById(R.id.stateDescriptionText);
        BrandButtonNormal stateActionBtn = emptyView.findViewById(R.id.stateActionBtn);

        String merchantBusinessType = mSessionManager.getBusinessType();
        if (merchantBusinessType.equals(getString(R.string.hair_and_beauty))) {
            stateImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_product_beauty));
        } else if (merchantBusinessType.equals(getString(R.string.fashion_and_accessories))) {
            stateImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_product_fashion));
        } else if (merchantBusinessType.equals(getString(R.string.beverages_and_deserts)) || merchantBusinessType.equals(getString(R.string.bakery_and_pastry))) {
            stateImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_product_food));
        } else {
            stateImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_product_others));
        }

        stateIntroTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateDescriptionTextView.setText(getString(R.string.no_products_found));

        stateActionBtn.setText(getString(R.string.start_adding_products_label));
        stateActionBtn.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, AddProductActivity.class);
            intent.putExtra(Constants.ACTIVITY_INITIATOR, TAG);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        mRecyclerView = recyclerView;
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, 3);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.setEmptyView(emptyView);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                if (!AccountGeneral.isAccountActive(mContext)) {
                    Snackbar.make(mLayout,
                            "Your subscription has expired, update subscription to edit/delete a product",
                            Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                        Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                        startActivity(intent);
                    }).show();
                } else {
                    ProductItemBinding productItemBinding = (ProductItemBinding) view.getTag();
                    if (productItemBinding != null) {
                        Product product = productItemBinding.getProduct();
                        Intent intent = new Intent(mContext, ProductDetailActivity.class);
                        intent.putExtra(ProductDetailActivity.ARG_ITEM_ID, product.getId());
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                if (!AccountGeneral.isAccountActive(mContext)) {
                    Snackbar.make(mLayout,
                            "Your subscription has expired, update subscription to delete product",
                            Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                        Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                        startActivity(intent);
                    }).show();
                } else {
                    ProductItemBinding productItemBinding = (ProductItemBinding) view.getTag();
                    if (productItemBinding != null) {
                        Product product = productItemBinding.getProduct();
                        new AlertDialog.Builder(ProductListActivity.this)
                                .setTitle("Are you sure?")
                                .setMessage("You won't be able to recover this product.")
                                .setPositiveButton(getString(R.string.confirm_delete_positive), (dialog, which) -> {
                                    dialog.dismiss();
                                    ProductEntity productEntity =
                                            mDataStore.findByKey(ProductEntity.class,
                                                    product.getId()).blockingGet();
                                    if (productEntity != null) {
                                        productEntity.setDeleted(true);
                                        mDataStore.update(productEntity).subscribe(/*no-op*/);
                                        mAdapter.queryAsync();
                                        SyncAdapter.performSync(mContext, mSessionManager.getEmail());

                                        String deleteText =  product.getName() + " has been deleted!";
                                        Snackbar.make(mLayout, deleteText, Snackbar.LENGTH_LONG).show();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
            }
        }));

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            searchFilterText = null;
            ((ProductsListAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
        }
        else {
            ((ProductsListAdapter) mRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        mRecyclerView.scrollToPosition(0);
        return true;
    }

    private class ProductsListAdapter extends QueryRecyclerAdapter<ProductEntity, BindingHolder<ProductItemBinding>> implements Filterable {
        private Filter filter;

        ProductsListAdapter() {
            super(ProductEntity.$TYPE);
        }

        @Override
        public Result<ProductEntity> performQuery() {
            MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                    .where(MerchantEntity.ID.eq(mSessionManager.getMerchantId()))
                    .get()
                    .firstOrNull();

            if (merchantEntity == null) {
                return null;
            }

            if (searchFilterText == null || TextUtils.isEmpty(searchFilterText)) {
                Selection<ReactiveResult<ProductEntity>> productsSelection = mDataStore.select(ProductEntity.class);
                productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
                productsSelection.where(ProductEntity.DELETED.notEqual(true));

                return productsSelection.orderBy(ProductEntity.NAME.asc()).get();
            } else {
                String query = "%" + searchFilterText.toLowerCase() + "%";
                Selection<ReactiveResult<ProductEntity>> productsSelection = mDataStore.select(ProductEntity.class);
                productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
                productsSelection.where(ProductEntity.NAME.like(query));
                productsSelection.where(ProductEntity.DELETED.notEqual(true));

                return productsSelection.orderBy(ProductEntity.NAME.asc()).get();
            }
        }

        @Override
        public void onBindViewHolder(ProductEntity item, BindingHolder<ProductItemBinding> holder, int position) {
            holder.binding.setProduct(item);
            holder.binding.productPrice.setText(String.valueOf(item.getPrice()));
            Glide.with(mContext)
                    .load(item.getPicture())
                    .apply(RequestOptions.centerCropTransform())
                    .apply(RequestOptions.placeholderOf(AppCompatResources.getDrawable(mContext, R.drawable.ic_photo_black_24px)))
                    .into(holder.binding.productImg);
        }

        @Override
        public BindingHolder<ProductItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ProductItemBinding binding = ProductItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            return new BindingHolder<>(binding);
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new ProductsFilter(new ArrayList<>(mAdapter.performQuery().toList()));
            }
            return filter;
        }

        private class ProductsFilter extends Filter {

            private ArrayList<ProductEntity> productEntities;

            ProductsFilter(ArrayList<ProductEntity> productEntities) {
                this.productEntities = new ArrayList<>();
                synchronized (this) {
                    this.productEntities.addAll(productEntities);
                }
            }

            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults result = new FilterResults();
                String searchString = charSequence.toString();
                if (TextUtils.isEmpty(searchString)) {
                    synchronized (this) {
                        result.count = productEntities.size();
                        result.values = productEntities;
                    }
                } else {
                    searchFilterText = searchString;
                    result.count = 0;
                    result.values = new ArrayList<>();
                }
                return result;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mAdapter.queryAsync();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search, menu);

        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(this);

        return true;
    }
}
