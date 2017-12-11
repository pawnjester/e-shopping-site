package co.loystar.loystarbusiness.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Product;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

public class StampsSaleWithPosActivity
    extends RxAppCompatActivity implements CustomerAutoCompleteDialog.SelectedCustomerListener,
    SearchView.OnQueryTextListener{

    private static final String TAG = StampsSaleWithPosActivity.class.getSimpleName();

    private ReactiveEntityStore<Persistable> mDataStore;
    private Context mContext;
    private ProductsAdapter mProductsAdapter;
    private ExecutorService executor;
    private SessionManager mSessionManager;
    private MerchantEntity merchantEntity;
    private CustomerEntity mSelectedCustomer;
    private ProductEntity mSelectedProduct;
    private int mProgramId;
    private String merchantCurrencySymbol;
    private String searchFilterText;

    /*Views*/
    CustomerAutoCompleteDialog customerAutoCompleteDialog;
    private EmptyRecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stamps_sale_with_pos);
        Toolbar toolbar = findViewById(R.id.activity_stamps_sale_with_pos_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        View mLayout = findViewById(R.id.activity_stamps_sale_with_pos_container);
        boolean productCreatedIntent = getIntent().getBooleanExtra(getString(R.string.product_create_success), false);
        if (productCreatedIntent) {
            Snackbar.make(mLayout, getString(R.string.product_create_success), Snackbar.LENGTH_LONG).show();
        }

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();
        merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(this).getCurrency(mSessionManager.getCurrency()).getSymbol();
        mProgramId = getIntent().getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
        int customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, customerId).blockingGet();

        mProductsAdapter = new ProductsAdapter();
        executor = Executors.newSingleThreadExecutor();
        mProductsAdapter.setExecutor(executor);

        customerAutoCompleteDialog = CustomerAutoCompleteDialog.newInstance(getString(R.string.order_owner));
        customerAutoCompleteDialog.setSelectedCustomerListener(this);

        EmptyRecyclerView productsRecyclerView = findViewById(R.id.stamps_sale_order_items_rv);
        assert productsRecyclerView != null;
        setupProductsRecyclerView(productsRecyclerView);
    }

    private void setupProductsRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.empty_items_container);
        ImageView stateWelcomeImageView = emptyView.findViewById(R.id.stateImage);
        TextView stateWelcomeTextView = emptyView.findViewById(R.id.stateIntroText);
        TextView stateDescriptionTextView = emptyView.findViewById(R.id.stateDescriptionText);
        BrandButtonNormal stateActionBtn = emptyView.findViewById(R.id.stateActionBtn);

        String merchantBusinessType = mSessionManager.getBusinessType();
        if (merchantBusinessType.equals(getString(R.string.hair_and_beauty))) {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_product_beauty));
        } else if (merchantBusinessType.equals(getString(R.string.fashion_and_accessories))) {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_product_fashion));
        } else if (merchantBusinessType.equals(getString(R.string.beverages_and_deserts)) || merchantBusinessType.equals(getString(R.string.bakery_and_pastry))) {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_product_food));
        } else {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_no_product_others));
        }

        stateWelcomeTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateDescriptionTextView.setText(getString(R.string.no_products_found));

        stateActionBtn.setText(getString(R.string.start_adding_products_label));
        stateActionBtn.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, AddProductActivity.class);
            intent.putExtra(Constants.ACTIVITY_INITIATOR, TAG);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        mRecyclerView = recyclerView;
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, 3);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mProductsAdapter);
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.setEmptyView(emptyView);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                PosProductItemBinding posProductItemBinding = (PosProductItemBinding) view.getTag();
                if (posProductItemBinding != null) {
                    Product product = posProductItemBinding.getProduct();
                    mSelectedProduct = mDataStore.findByKey(ProductEntity.class, product.getId()).blockingGet();
                    if (mSelectedCustomer == null) {
                        if (!customerAutoCompleteDialog.isAdded()) {
                            customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
                        }
                    } else {
                        goToAddStampsView(mSelectedCustomer, mSelectedProduct);
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    @Override
    public void onCustomerSelected(@NonNull CustomerEntity customerEntity) {
        mSelectedCustomer = customerEntity;
        goToAddStampsView(customerEntity, mSelectedProduct);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            searchFilterText = null;
            ((ProductsAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
        }
        else {
            ((ProductsAdapter) mRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        mRecyclerView.scrollToPosition(0);
        return true;
    }

    private class ProductsAdapter
        extends QueryRecyclerAdapter<ProductEntity, BindingHolder<PosProductItemBinding>> implements Filterable {

        private Filter filter;

        ProductsAdapter() {
            super(ProductEntity.$TYPE);
        }

        @Override
        public Result<ProductEntity> performQuery() {
            if (merchantEntity == null) {
                return null;
            }

            if (searchFilterText == null || TextUtils.isEmpty(searchFilterText)) {
                Selection<ReactiveResult<ProductEntity>> productsSelection = mDataStore.select(ProductEntity.class);
                productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
                productsSelection.where(ProductEntity.DELETED.notEqual(true));

                return productsSelection.orderBy(ProductEntity.UPDATED_AT.desc()).get();
            } else {
                String query = "%" + searchFilterText.toLowerCase() + "%";
                Selection<ReactiveResult<ProductEntity>> productsSelection = mDataStore.select(ProductEntity.class);
                productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
                productsSelection.where(ProductEntity.NAME.like(query));
                productsSelection.where(ProductEntity.DELETED.notEqual(true));

                return productsSelection.orderBy(ProductEntity.UPDATED_AT.desc()).get();
            }
        }

        @SuppressLint("CheckResult")
        @Override
        public void onBindViewHolder(ProductEntity item, BindingHolder<PosProductItemBinding> holder, int position) {
            holder.binding.setProduct(item);
            RequestOptions options = new RequestOptions();
            options.centerCrop().apply(RequestOptions.placeholderOf(
                    AppCompatResources.getDrawable(mContext, R.drawable.ic_photo_black_24px)
            ));
            Glide.with(mContext)
                    .load(item.getPicture())
                    .apply(options)
                    .into(holder.binding.productImage);
            Glide.with(mContext)
                    .load(item.getPicture())
                    .apply(options)
                    .into(holder.binding.productImageCopy);

            holder.binding.productName.setText(item.getName());
            holder.binding.productDecrementWrapper.setVisibility(View.GONE);

            holder.binding.productPrice.setText(getString(R.string.product_price, merchantCurrencySymbol, String.valueOf(item.getPrice())));
            holder.binding.addImage.bringToFront();
        }

        @Override
        public BindingHolder<PosProductItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final PosProductItemBinding binding = PosProductItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            return new BindingHolder<>(binding);
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new ProductsFilter(new ArrayList<>(mProductsAdapter.performQuery().toList()));
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
                mProductsAdapter.queryAsync();
            }
        }
    }

    private void goToAddStampsView(@NonNull CustomerEntity customerEntity, @NonNull ProductEntity productEntity) {
        Intent addStampsIntent = new Intent(mContext, AddStampsActivity.class);
        addStampsIntent.putExtra(Constants.PRODUCT_ID, productEntity.getId());
        addStampsIntent.putExtra(Constants.LOYALTY_PROGRAM_ID, mProgramId);
        addStampsIntent.putExtra(Constants.CUSTOMER_ID, customerEntity.getId());
        startActivity(addStampsIntent);
    }

    @Override
    protected void onResume() {
        mProductsAdapter.queryAsync();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mProductsAdapter.close();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CustomerAutoCompleteDialog.ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data.hasExtra(Constants.CUSTOMER_ID)) {
                    mDataStore.findByKey(CustomerEntity.class, data.getIntExtra(Constants.CUSTOMER_ID, 0))
                            .toObservable()
                            .compose(bindToLifecycle())
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(customerEntity -> {
                                if (customerEntity == null) {
                                    Toast.makeText(mContext, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                                } else {
                                    mSelectedCustomer = customerEntity;
                                    goToAddStampsView(customerEntity, mSelectedProduct);
                                }
                            });
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
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
