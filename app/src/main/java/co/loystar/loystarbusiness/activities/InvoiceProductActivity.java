package co.loystar.loystarbusiness.activities;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.OrderSummaryItemBinding;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Invoice;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceTransactionEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Product;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.CircleAnimationUtil;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.OrderItemDividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.UserLockBottomSheetBehavior;
import co.loystar.loystarbusiness.utils.ui.buttons.CartCountButton;
import co.loystar.loystarbusiness.utils.ui.buttons.FullRectangleButton;
import io.reactivex.Completable;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class InvoiceProductActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    private ProductsAdapter mProductAdapter;
    private ExecutorService executor;
    private String searchFilterText;
    private EmptyRecyclerView mProductsRecyclerView;
    private EmptyRecyclerView mOrderSummaryRecyclerView;
    private Context mContext;
    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SparseIntArray mSelectedProducts = new SparseIntArray();
    private String merchantCurrencySymbol;
    private BottomSheetBehavior orderSummaryBottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback mOrderSummaryBottomSheetCallback;
    private boolean orderSummaryDraggingStateUp = false;
    private OrderSummaryAdapter orderSummaryAdapter;
    private DatabaseManager databaseManager;
    AlertDialog dialog;
    AlertDialog.Builder builder;
    private CustomerEntity mSelectedCustomer;
    int customerId;
    private ApiClient mApiClient;
    private int invoiceId;

    private ImageView cartCountImageView;
    private View collapsedToolbar;
    private CartCountButton proceedToCheckoutBtn;
    private int proceedToCheckoutBtnHeight = 0;
    private double totalCharge = 0;
    private Toolbar orderSummaryExpandedToolbar;
    private FullRectangleButton orderSummaryCheckoutBtn;
    private View orderSummaryCheckoutWrapper;

    private final String KEY_PRODUCTS_RECYCLER_STATE = "products_recycler_state";
    private final String KEY_SELECTED_PRODUCTS_STATE = "selected_products_state";
    private final String KEY_ORDER_SUMMARY_RECYCLER_STATE = "order_summary_recycler_state";

    HashMap<Integer, Integer> mSelectedProductHash = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_product);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setDisplayShowHomeEnabled(true);

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class,
                mSessionManager.getMerchantId()).blockingGet();
        merchantCurrencySymbol = CurrenciesFetcher
                .getCurrencies(this).getCurrency(
                        mSessionManager.getCurrency()).getSymbol();
        mApiClient = new ApiClient(this);
        databaseManager = DatabaseManager.getInstance(this);
        customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        mSelectedCustomer = mDataStore
                .findByKey(CustomerEntity.class, customerId).blockingGet();
        mProductAdapter = new ProductsAdapter();
        orderSummaryAdapter = new OrderSummaryAdapter();
        executor = Executors.newSingleThreadExecutor();
        mProductAdapter.setExecutor(executor);
        orderSummaryAdapter.setExecutor(executor);
        mSelectedProductHash =
                (HashMap<Integer, Integer>)
                        getIntent().getSerializableExtra("PRODUCTS_ARRAY");

        findViewById(R.id.order_summary_invoice_wrapper).bringToFront();
        invoiceId = getIntent().getIntExtra(Constants.INVOICE_ID, 0);

        mOrderSummaryBottomSheetCallback = new
                BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        orderSummaryDraggingStateUp = true;
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        orderSummaryDraggingStateUp = false;
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View view, float slideOffset) {
                Runnable draggingStateUpAction = () -> {
                    if (orderSummaryBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        collapsedToolbar.setVisibility(View.GONE);
                        collapsedToolbar.setAlpha(0f);
                        orderSummaryExpandedToolbar.setAlpha(1f);
                    }
                };

                Runnable draggingStateDownAction = () -> {
                    if (orderSummaryBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        orderSummaryExpandedToolbar.setVisibility(View.GONE);
                        collapsedToolbar.setAlpha(1f);
                        orderSummaryExpandedToolbar.setAlpha(0f);
                    }
                };

                if (orderSummaryDraggingStateUp) {

                    orderSummaryExpandedToolbar.setVisibility(View.VISIBLE);
                    orderSummaryExpandedToolbar.animate().alpha(slideOffset).setDuration((long) slideOffset);

                    float offset = 1f - slideOffset;
                    collapsedToolbar.setVisibility(View.VISIBLE);
                    collapsedToolbar.animate().alpha(offset).setDuration((long) offset).withEndAction(draggingStateDownAction);
                } else {
                    float offset = 1f - slideOffset;
                    collapsedToolbar.setVisibility(View.VISIBLE);
                    collapsedToolbar.animate().alpha(offset).setDuration((long) offset);

                    orderSummaryExpandedToolbar.setVisibility(View.VISIBLE);
                    orderSummaryExpandedToolbar.animate().alpha(slideOffset).setDuration((long) slideOffset).withEndAction(draggingStateUpAction);
                }
            }
        };

        builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.layout_loading_dialog);
        dialog = builder.create();

        EmptyRecyclerView productsRecyclerView =
                findViewById(R.id.points_invoice_order_items_rv);
        assert productsRecyclerView != null;
        setupProductsRecyclerView(productsRecyclerView);

        EmptyRecyclerView orderSummaryRecyclerView = findViewById(R.id.order_list);
        assert orderSummaryRecyclerView != null;
        setupOrderSummaryRecyclerView(orderSummaryRecyclerView);
        setupBottomSheetView();

        checkifproductExists();
    }

    private void setupProductsRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        mProductsRecyclerView = recyclerView;
        mProductsRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, 2);
        mProductsRecyclerView.setLayoutManager(mLayoutManager);
        mProductsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mProductsRecyclerView.setAdapter(mProductAdapter);
        mProductsRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
    }

    private void setupBottomSheetView() {
        collapsedToolbar = findViewById(R.id.order_summary_collapsed_toolbar);
        proceedToCheckoutBtn = collapsedToolbar.findViewById(R.id.proceed_to_check_out);
        cartCountImageView = proceedToCheckoutBtn.getCartImageView();
        orderSummaryCheckoutBtn = findViewById(R.id.order_summary_checkout_btn);
        orderSummaryCheckoutWrapper = findViewById(R.id.order_summary_checkout_wrapper);
        orderSummaryExpandedToolbar = findViewById(R.id.order_summary_expanded_toolbar);
        orderSummaryBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.order_summary_invoice_wrapper));
        orderSummaryBottomSheetBehavior.setBottomSheetCallback(mOrderSummaryBottomSheetCallback);

        ViewTreeObserver treeObserver = proceedToCheckoutBtn.getViewTreeObserver();
        treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver obs = proceedToCheckoutBtn.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
                proceedToCheckoutBtnHeight = proceedToCheckoutBtn.getMeasuredHeight();
            }
        });

        RxView.clicks(proceedToCheckoutBtn).subscribe(o -> showBottomSheet(true));

        if (orderSummaryBottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
            ((UserLockBottomSheetBehavior) orderSummaryBottomSheetBehavior).setAllowUserDragging(false);
        }

        findViewById(R.id.clear_cart).setOnClickListener(view -> new AlertDialog.Builder(mContext)
                .setTitle("Are you sure?")
                .setMessage("All items will be permanently removed from your cart!")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialogInterface, which) -> {
                    mSelectedProducts.clear();
                    totalCharge = 0;
                    mProductAdapter.queryAsync();
                    orderSummaryAdapter.queryAsync();
                    refreshCartCount();
                    setCheckoutValue();
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel())
                .setIcon(AppCompatResources.getDrawable(mContext, android.R.drawable.ic_dialog_alert))
                .show());

        orderSummaryExpandedToolbar.setNavigationOnClickListener(view -> showBottomSheet(false));

    }


    private void setupOrderSummaryRecyclerView (@NonNull EmptyRecyclerView recyclerview) {
        View emptyView = findViewById(R.id.empty_cart);
        mOrderSummaryRecyclerView = recyclerview;
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mOrderSummaryRecyclerView.setHasFixedSize(true);
        mOrderSummaryRecyclerView.setLayoutManager(mLayoutManager);
        mOrderSummaryRecyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mOrderSummaryRecyclerView.addItemDecoration(new OrderItemDividerItemDecoration(this));
        mOrderSummaryRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mOrderSummaryRecyclerView.setAdapter(orderSummaryAdapter);
        mOrderSummaryRecyclerView.setEmptyView(emptyView);
    }

    private void showBottomSheet(boolean show) {
        if (show) {
            orderSummaryBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            orderSummaryBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void showCheckoutBtn(boolean show) {
        if (show) {
            orderSummaryCheckoutWrapper.setVisibility(View.VISIBLE);
        } else {
            orderSummaryCheckoutWrapper.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            searchFilterText = null;
            ((ProductsAdapter) mProductsRecyclerView.getAdapter()).getFilter().filter(null);
        } else {
            ((ProductsAdapter) mProductsRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        mProductsRecyclerView.scrollToPosition(0);
        return true;
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void checkifproductExists () {
        for (Map.Entry<Integer, Integer> item: mSelectedProductHash.entrySet()) {
            mSelectedProducts.put(item.getKey(), item.getValue());
        }
    }


    private class ProductsAdapter extends QueryRecyclerAdapter<ProductEntity,
            BindingHolder<PosProductItemBinding>> implements Filterable {

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

                return productsSelection.orderBy(ProductEntity.NAME.asc()).get();
            } else {
                String query = "%" + searchFilterText.toLowerCase() + "%";
                ProductCategoryEntity categoryEntity = mDataStore.select(ProductCategoryEntity.class)
                        .where(ProductCategoryEntity.NAME.like(query))
                        .get()
                        .firstOrNull();

                Selection<ReactiveResult<ProductEntity>> productsSelection = mDataStore.select(ProductEntity.class);
                productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
                productsSelection.where(ProductEntity.NAME.like(query)).or(ProductEntity.CATEGORY.equal(categoryEntity));
                productsSelection.where(ProductEntity.DELETED.notEqual(true));

                return productsSelection.orderBy(ProductEntity.NAME.asc()).get();
            }
        }

        @SuppressLint("CheckResult")
        @Override
        public void onBindViewHolder(ProductEntity item,
                                     BindingHolder<PosProductItemBinding> holder, int position) {
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
            if (mSelectedProducts.get(item.getId()) == 0) {
                holder.binding.productDecrementWrapper.setVisibility(View.GONE);
            } else {
                holder.binding.productDecrementWrapper.setVisibility(View.VISIBLE);
                holder.binding.productCount.setText(
                        getString(R.string.product_count,
                                String.valueOf(mSelectedProducts.get(item.getId()))));
            }
            holder.binding.productPrice.setText(
                    getString(R.string.product_price, merchantCurrencySymbol,
                            String.valueOf(item.getPrice())));
            holder.binding.addImage.bringToFront();
            holder.binding.decrementCount.setImageDrawable(
                    AppCompatResources.getDrawable(mContext,
                            R.drawable.ic_remove_circle_outline_white_24px));
        }

        @NonNull
        @Override
        public BindingHolder<PosProductItemBinding> onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final PosProductItemBinding binding = PosProductItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            binding.decrementCount.setTag(binding);
            binding.productImageWrapper.setTag(binding);
            binding.productImageWrapper.setOnClickListener(view -> {
                PosProductItemBinding posProductItemBinding = (PosProductItemBinding) view.getTag();
                if (posProductItemBinding != null) {
                    Product product = posProductItemBinding.getProduct();
                    if (mSelectedProducts.get(product.getId()) == 0) {
                        mSelectedProducts.put(product.getId(), 1);
                        mProductAdapter.queryAsync();
                        orderSummaryAdapter.queryAsync();
                        setCheckoutValue();
                    } else {
                        mSelectedProducts.put(product.getId(),
                                (mSelectedProducts.get(product.getId()) + 1));
                        mProductAdapter.queryAsync();
                        orderSummaryAdapter.queryAsync();
                        setCheckoutValue();
                    }

                    binding.productDecrementWrapper.setVisibility(View.VISIBLE);
                    makeFlyAnimation(posProductItemBinding.productImageCopy, cartCountImageView);
                }
            }
            );

            binding.decrementCount.setOnClickListener(view -> {
                PosProductItemBinding posProductItemBinding = (PosProductItemBinding) view.getTag();
                if (posProductItemBinding != null) {
                    Product product = posProductItemBinding.getProduct();
                    if (mSelectedProducts.get(product.getId()) != 0) {
                        int newValue = mSelectedProducts.get(product.getId()) - 1;
                        setProductCountValue(newValue, product.getId());
                        if (newValue == 0) {
                            posProductItemBinding.productCount.setText("0");
                            posProductItemBinding.productDecrementWrapper.setVisibility(View.GONE);
                        } else {
                            posProductItemBinding.productCount.setText(
                                    getString(R.string.product_count, String.valueOf(newValue)));
                        }
                    }
                }
            });
            return new BindingHolder<>(binding);
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new ProductFilter(new ArrayList<>(mProductAdapter.performQuery().toList()));
            }
            return filter;
        }

        private class ProductFilter extends Filter {

            private ArrayList<ProductEntity> productEntities;

            ProductFilter(ArrayList<ProductEntity> productEntities) {
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
                mProductAdapter.queryAsync();
            }
        }
    }

    private class OrderSummaryAdapter
            extends QueryRecyclerAdapter<ProductEntity,
            BindingHolder<OrderSummaryItemBinding>> {

        OrderSummaryAdapter() {
            super(ProductEntity.$TYPE);
        }

        @Override
        public Result<ProductEntity> performQuery() {
            ArrayList<Integer> ids = new ArrayList<>();
            for (int i = 0; i < mSelectedProducts.size(); i ++){
                ids.add(mSelectedProducts.keyAt(i));
            }
            return mDataStore.select(ProductEntity.class)
                    .where(ProductEntity.ID.in(ids))
                    .orderBy(ProductEntity.UPDATED_AT.desc())
                    .get();
        }

        @Override
        public void onBindViewHolder(ProductEntity item,
                                     BindingHolder<OrderSummaryItemBinding> holder,
                                     int position) {
            holder.binding.setProduct(item);
            RequestOptions options = new RequestOptions();
            options.fitCenter().centerCrop().apply(RequestOptions.placeholderOf(
                    AppCompatResources.getDrawable(
                            getApplicationContext(),
                            R.drawable.ic_photo_black_24px)
            ));
            Glide.with(mContext)
                    .load(item.getPicture())
                    .apply(options)
                    .into(holder.binding.productImage);
            holder.binding.productName.setText(item.getName());
            holder.binding.deleteCartItem.setImageDrawable(AppCompatResources
                    .getDrawable(getApplicationContext(), R.drawable.ic_close_white_24px));

            holder.binding.orderItemIncDecBtn.setNumber(String.valueOf(
                    mSelectedProducts.get(item.getId())));


            String template = "%.2f";
            double totalCostOfItem = item.getPrice() * mSelectedProducts.get(item.getId());
            String cText = String.format(Locale.UK, template, totalCostOfItem);
            holder.binding.productCost.setText(getString(R.string.product_price,
                    merchantCurrencySymbol, cText));
        }

        @NonNull
        @Override
        public BindingHolder<OrderSummaryItemBinding> onCreateViewHolder(
                @NonNull ViewGroup parent, int i) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final OrderSummaryItemBinding binding = OrderSummaryItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            binding.deleteCartItem.setTag(binding);

            binding.deleteCartItem.setOnClickListener(view ->
                    new AlertDialog.Builder(InvoiceProductActivity.this)
                            .setTitle("Are you sure?")
                            .setMessage("This item will be permanently removed from your cart!")
                            .setCancelable(false)
                            .setPositiveButton("Yes", (dialog, which) -> {
                                OrderSummaryItemBinding orderSummaryItemBinding
                                        = (OrderSummaryItemBinding) view.getTag();
                                if (orderSummaryItemBinding != null
                                        && orderSummaryItemBinding.getProduct() != null) {
                                    mSelectedProducts
                                            .delete(orderSummaryItemBinding.getProduct().getId());
                                    orderSummaryAdapter.queryAsync();
                                    mProductAdapter.queryAsync();
                                    refreshCartCount();
                                    setCheckoutValue();
                                }
                            })
                            .setNegativeButton("Cancel",
                                    (dialogInterface, i1) -> dialogInterface.cancel())
                            .setIcon(AppCompatResources.getDrawable(getApplicationContext(),
                                    android.R.drawable.ic_dialog_alert))
                            .show());
            binding.orderItemIncDecBtn.setOnValueChangeListener((view, oldValue, newValue)
                    -> setProductCountValue(newValue, binding.getProduct().getId()));
            return new BindingHolder<>(binding);
        }
    }

    @Override
    protected void onResume() {
        mProductAdapter.queryAsync();
        orderSummaryAdapter.queryAsync();
        super.onResume();
    }

    private void setCheckoutValue() {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < mSelectedProducts.size(); i++) {
            ids.add(mSelectedProducts.keyAt(i));
        }
        Result<ProductEntity> result = mDataStore.select(ProductEntity.class)
                .where(ProductEntity.ID.in(ids))
                .orderBy(ProductEntity.UPDATED_AT.desc())
                .get();

        showCheckoutBtn(!result.toList().isEmpty());
        showProceedToCheckoutBtn(!result.toList().isEmpty());

        double tc = 0;
        for (ProductEntity product: result.toList()) {
            double totalCostOfItem = product.getPrice() * mSelectedProducts.get(product.getId());
            tc += totalCostOfItem;
        }
        String template = "%.2f";
        totalCharge = Double.valueOf(String.format(Locale.UK, template, tc));
        String cText = String.format(Locale.UK, template, totalCharge);
        orderSummaryCheckoutBtn.setText(getString(R.string.charge, merchantCurrencySymbol, cText));
        proceedToCheckoutBtn.setCheckoutText(merchantCurrencySymbol, cText);
        orderSummaryCheckoutBtn.setOnClickListener(view -> {
            if (isOnline(this)){
                updateInvoiceProduct(invoiceId, ids);
            }else {
                Toast.makeText(this,
                        "Please connect to the internet", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setProductCountValue(int newValue, int productId) {
        if (newValue > 0){
            mSelectedProducts.put(productId, newValue);
            orderSummaryAdapter.queryAsync();
            mProductAdapter.queryAsync();
            setCheckoutValue();
            refreshCartCount();
        } else {
            mSelectedProducts.delete(productId);
            orderSummaryAdapter.queryAsync();
            mProductAdapter.queryAsync();
            setCheckoutValue();
            refreshCartCount();
        }
    }



    private void showProceedToCheckoutBtn(boolean show) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );

        if (show) {
            orderSummaryBottomSheetBehavior.setPeekHeight(proceedToCheckoutBtnHeight);
            params.setMargins(0, 0, 0, proceedToCheckoutBtnHeight);
            mProductsRecyclerView.setLayoutParams(params);
        } else {
            orderSummaryBottomSheetBehavior.setPeekHeight(0);
            params.setMargins(0, 0, 0, 0);
            mProductsRecyclerView.setLayoutParams(params);
        }
    }
    private void makeFlyAnimation(View targetView, View destinationView) {

        new CircleAnimationUtil().attachActivity(this)
                .setTargetView(targetView)
                .setMoveDuration(500)
                .setDestView(destinationView)
                .setAnimationListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation)
                    {
                        refreshCartCount();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).startAnimation();
    }

    private void refreshCartCount() {
        proceedToCheckoutBtn.setCartCount(String.valueOf(mSelectedProducts.size()));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mProductAdapter.close();
        orderSummaryAdapter.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (orderSummaryBottomSheetBehavior.getState()
                == BottomSheetBehavior.STATE_EXPANDED) {
            showBottomSheet(false);
        } else {
            finish();
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems
                = new HashMap<>(mSelectedProducts.size());
        for (int x = 0; x < mSelectedProducts.size(); x++) {
            orderSummaryItems.put(mSelectedProducts.keyAt(x), mSelectedProducts.valueAt(x));
        }
        outState.putSerializable(KEY_SELECTED_PRODUCTS_STATE, orderSummaryItems);
        Parcelable productsListState = mProductsRecyclerView.getLayoutManager().onSaveInstanceState();
        Parcelable orderSummaryListState = mOrderSummaryRecyclerView.getLayoutManager().onSaveInstanceState();

        outState.putParcelable(KEY_PRODUCTS_RECYCLER_STATE, productsListState);
        outState.putParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE, orderSummaryListState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Parcelable productsListState = savedInstanceState.getParcelable(KEY_PRODUCTS_RECYCLER_STATE);
        Parcelable orderSummaryListState = savedInstanceState.getParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE);

        if (productsListState != null) {
            mProductsRecyclerView.getLayoutManager().onRestoreInstanceState(productsListState);
        }

        if (orderSummaryListState != null) {
            mOrderSummaryRecyclerView.getLayoutManager().onRestoreInstanceState(orderSummaryListState);
        }

        @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems =
                (HashMap<Integer, Integer>) savedInstanceState.getSerializable(KEY_SELECTED_PRODUCTS_STATE);

        if (orderSummaryItems != null) {
            for (Map.Entry<Integer, Integer> orderItem: orderSummaryItems.entrySet()) {
                mSelectedProducts.put(orderItem.getKey(), orderItem.getValue());
                setProductCountValue(orderItem.getValue(), orderItem.getKey());
            }
        }

        showBottomSheet(false);
        ViewTreeObserver treeObserver = proceedToCheckoutBtn.getViewTreeObserver();
        treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver obs = proceedToCheckoutBtn.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
                proceedToCheckoutBtnHeight = proceedToCheckoutBtn.getHeight();
                orderSummaryBottomSheetBehavior.setPeekHeight(proceedToCheckoutBtnHeight);
            }
        });
    }

    private void updateInvoiceProduct(int id, List<Integer> productId) {
        dialog.show();
        dialog.setCancelable(false);
        dialog.setTitle("Updating products");
        InvoiceEntity invoiceEntity = databaseManager.getInvoiceById(id);
        invoiceEntity.setUpdatedAt(new Timestamp(new DateTime().getMillis()));
        invoiceEntity.setPaymentMethod(invoiceEntity.getPaymentMethod());
        invoiceEntity.setPaidAmount(invoiceEntity.getPaidAmount());
        invoiceEntity.setSynced(true);
        invoiceEntity.setOwner(merchantEntity);
        invoiceEntity.setCustomer(mSelectedCustomer);
        invoiceEntity.setDueDate(invoiceEntity.getDueDate());
        invoiceEntity.setPaymentMessage(invoiceEntity.getPaymentMessage());
        mDataStore.upsert(invoiceEntity).subscribe(update -> {
            List<InvoiceTransactionEntity> transactionEntityList = update.getTransactions();
            mDataStore.delete(transactionEntityList).subscribe();

            Result<ProductEntity> result = mDataStore.select(ProductEntity.class)
                    .where(ProductEntity.ID.in(productId))
                    .orderBy(ProductEntity.UPDATED_AT.desc())
                    .get();
            List<ProductEntity> productEntities = result.toList();

            for (int i = 0; i < productEntities.size(); i++) {

                ProductEntity product = productEntities.get(i);
                LoyaltyProgramEntity loyaltyProgram = product.getLoyaltyProgram();

                if (loyaltyProgram != null) {
                    InvoiceTransactionEntity newTransaction = new InvoiceTransactionEntity();
                    newTransaction.setInvoice(update);
                    newTransaction.setMerchantLoyaltyProgramId(loyaltyProgram.getId());
                    String template = "%.2f";
                    double tc = product.getPrice() * mSelectedProducts.get(product.getId());
                    double totalCosts = Double.valueOf(String.format(Locale.UK, template, tc));
                    newTransaction.setAmount(totalCosts);

                    if (loyaltyProgram.getProgramType().equals(getString(R.string.simple_points))){
                        newTransaction.setPoints(Double.valueOf(totalCosts).intValue());
                        newTransaction.setProgramType(getString(R.string.simple_points));
                    } else if (loyaltyProgram.getProgramType().equals(getString(R.string.stamps_program))) {
                        int stampsEarned = mSelectedProducts.get(product.getId());
                        newTransaction.setStamps(stampsEarned);
                        newTransaction.setProgramType(getString(R.string.stamps_program));
                    }
                    newTransaction.setCreatedAt(new Timestamp(new DateTime().getMillis()));
                    newTransaction.setProductId(product.getId());
                    if (mSelectedCustomer != null) {
                        newTransaction.setUserId(mSelectedCustomer.getUserId());
                        newTransaction.setCustomer(mSelectedCustomer);
                    }
                    newTransaction.setMerchant(merchantEntity);
                    newTransaction.setQuantity(mSelectedProducts.get(product.getId()));
                    newTransaction.setSynced(false);
                    mDataStore.upsert(newTransaction).subscribe();
                    if (i + 1 == productEntities.size()) {
                        Completable.complete()
                                .delay(1, TimeUnit.SECONDS)
                                .compose(bindToLifecycle())
                                .doOnComplete(() -> {
                                    if (isOnline(this)) {
                                        List<InvoiceTransactionEntity> checkAgain = databaseManager
                                                .getInvoiceTransaction(id);
                                        try {
                                            JSONObject jsonObjectData = new JSONObject();
                                            if (update.getCustomer() != null) {
                                                jsonObjectData.put("user_id", update.getCustomer().getUserId());
                                            }
                                            if (update.getStatus() != null) {
                                                jsonObjectData.put("status", update.getStatus());
                                            }
                                            if (update.getPaymentMethod() != null) {
                                                jsonObjectData.put("payment_method", update.getPaymentMethod());
                                            }
                                            jsonObjectData.put("due_date", update.getDueDate());
                                            if (update.getPaidAmount() != null) {
                                                if (Double.valueOf(update.getPaidAmount()) >= Double.valueOf(update.getAmount())) {
                                                    jsonObjectData.put("paid_amount", update.getPaidAmount());
                                                    jsonObjectData.put("status", "paid");
                                                } else if ( Double.valueOf(update.getPaidAmount()) < Double.valueOf(update.getAmount())) {
                                                    jsonObjectData.put("paid_amount", update.getPaidAmount());
                                                    jsonObjectData.put("status", "partial");
                                                }else {
                                                    jsonObjectData.put("paid_amount", update.getPaidAmount());
                                                    jsonObjectData.put("status", "unpaid");
                                                }
                                            }
                                            JSONArray jsonArray = new JSONArray();
                                            for (InvoiceTransactionEntity transactionEntity: checkAgain) {
                                                LoyaltyProgramEntity programEntity =
                                                        databaseManager.getLoyaltyProgramById(
                                                                transactionEntity.getMerchantLoyaltyProgramId());
                                                if (programEntity != null) {
                                                    JSONObject jsonObject = new JSONObject();

                                                    if (transactionEntity.getUserId() > 0) {
                                                        jsonObject.put("user_id", transactionEntity.getUserId());
                                                    }
                                                    jsonObject.put("merchant_id", merchantEntity.getId());
                                                    jsonObject.put("amount", transactionEntity.getAmount());

                                                    if (transactionEntity.getProductId() > 0) {
                                                        jsonObject.put("id", transactionEntity.getProductId());
                                                    }
                                                    jsonObject.put("merchant_loyalty_program_id",
                                                            transactionEntity.getMerchantLoyaltyProgramId());
                                                    jsonObject.put("program_type", transactionEntity.getProgramType());

                                                    if (programEntity.getProgramType().equals(getString(R.string.simple_points))) {
                                                        jsonObject.put("points", transactionEntity.getPoints());
                                                    }
                                                    else if (programEntity.getProgramType().equals(getString(R.string.stamps_program))) {
                                                        jsonObject.put("stamps", transactionEntity.getStamps());
                                                    }

                                                    jsonArray.put(jsonObject);
                                                }
                                            }
                                            jsonObjectData.put("items", jsonArray);
                                            JSONObject requestData = new JSONObject();
                                            requestData.put("data", jsonObjectData);
                                            RequestBody requestBody = RequestBody
                                                    .create(MediaType.parse(
                                                            "application/json; charset=utf-8"), requestData.toString());

                                            mApiClient.getLoystarApi(false)
                                                    .updateInvoice(update.getId(), requestBody).enqueue(new Callback<Invoice>() {
                                                @Override
                                                public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                                                    if (response.isSuccessful()) {
                                                        if (dialog.isShowing()) {
                                                            dialog.dismiss();
                                                        }
                                                        Invoice invoice =  response.body();
                                                        update.setAmount(invoice.getSubtotal());
                                                        update.setUpdatedAt(new Timestamp(new DateTime().getMillis()));
                                                        update.setPaymentMethod(update.getPaymentMethod());
                                                        update.setPaidAmount(invoice.getPaidAmount());
                                                        update.setSynced(true);
                                                        update.setStatus(invoice.getStatus());
                                                        update.setOwner(merchantEntity);
                                                        update.setCustomer(mSelectedCustomer);
                                                        update.setDueDate(invoice.getDueDate());
                                                        update.setPaymentMessage(invoice.getPaymentMessage());
                                                        mDataStore.upsert(update).subscribe();
                                                        Toast.makeText(getApplicationContext(),
                                                                "Invoice has been updated successfully",
                                                                Toast.LENGTH_LONG).show();
                                                        SyncAdapter.performSync(getApplicationContext(), mSessionManager.getEmail());
                                                        Intent nextIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                                                        nextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(nextIntent);
                                                    } else {
                                                        if (dialog.isShowing()) {
                                                            dialog.dismiss();
                                                        }
                                                        Toast.makeText(getApplicationContext(),
                                                                "Invoice could not be updated", Toast.LENGTH_SHORT).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<Invoice> call, Throwable t) {

                                                }
                                            });
                                        } catch (JSONException e) {
                                            Timber.e(e);
                                        }
                                    } else {
                                        runOnUiThread(() -> {
                                            Toast.makeText(getApplicationContext(),
                                                    "Invoice could not be updated successfully", Toast.LENGTH_LONG).show();
                                            dialog.dismiss();
                                        });
                                    }
                                }).subscribe();
                    }

                }
            }
        });
    }

    public boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }

}
