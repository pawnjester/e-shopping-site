package co.loystar.loystarbusiness.activities;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.OrderSummaryItemBinding;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Product;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.Foreground;
import co.loystar.loystarbusiness.utils.ui.CircleAnimationUtil;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.OrderItemDividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.UserLockBottomSheetBehavior;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.CartCountButton;
import co.loystar.loystarbusiness.utils.ui.buttons.FullRectangleButton;
import co.loystar.loystarbusiness.utils.ui.dialogs.CardPaymentDialog;
import co.loystar.loystarbusiness.utils.ui.dialogs.CashPaymentDialog;
import co.loystar.loystarbusiness.utils.ui.dialogs.CustomerAutoCompleteDialog;
import co.loystar.loystarbusiness.utils.ui.dialogs.MyAlertDialog;
import co.loystar.loystarbusiness.utils.ui.dialogs.PayOptionsDialog;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

public class SaleWithPosActivity extends BaseActivity implements
        CustomerAutoCompleteDialog.SelectedCustomerListener,
        PayOptionsDialog.PayOptionsDialogClickListener,
        CashPaymentDialog.CashPaymentDialogOnCompleteListener,
        CardPaymentDialog.CardPaymentDialogOnCompleteListener,
    SearchView.OnQueryTextListener{

    public static final String TAG = SaleWithPosActivity.class.getSimpleName();

    private ReactiveEntityStore<Persistable> mDataStore;
    private Context mContext;
    private ProductsAdapter mProductsAdapter;
    private OrderSummaryAdapter orderSummaryAdapter;
    private ExecutorService executor;
    private SessionManager mSessionManager;
    private BottomSheetBehavior.BottomSheetCallback mOrderSummaryBottomSheetCallback;
    private SparseIntArray mSelectedProducts = new SparseIntArray();
    private BottomSheetBehavior orderSummaryBottomSheetBehavior;
    private boolean orderSummaryDraggingStateUp = false;
    private double totalCharge = 0;
    private String merchantCurrencySymbol;
    private MerchantEntity merchantEntity;
    private CustomerEntity mSelectedCustomer;
    private boolean isPaidWithCash = false;
    private boolean isPaidWithCard = false;
    private boolean isPaidWithMobile = false;
    private boolean isPaidWithInvoice = false;

    /*Views*/
    private View collapsedToolbar;
    private Toolbar orderSummaryExpandedToolbar;
    private FullRectangleButton orderSummaryCheckoutBtn;
    private CartCountButton proceedToCheckoutBtn;
    private View orderSummaryCheckoutWrapper;
    private ImageView cartCountImageView;
    private String searchFilterText;
    private EmptyRecyclerView mProductsRecyclerView;
    private EmptyRecyclerView mOrderSummaryRecyclerView;
    private CustomerAutoCompleteDialog customerAutoCompleteDialog;
    private PayOptionsDialog payOptionsDialog;
    private int proceedToCheckoutBtnHeight = 0;
    private MyAlertDialog myAlertDialog;

    private final String KEY_PRODUCTS_RECYCLER_STATE = "products_recycler_state";
    private final String KEY_SELECTED_PRODUCTS_STATE = "selected_products_state";
    private final String KEY_ORDER_SUMMARY_RECYCLER_STATE = "order_summary_recycler_state";
    private final String KEY_SAVED_CUSTOMER_ID = "saved_customer_id";
    private int customerId;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_with_pos);

        mLayout = findViewById(R.id.activity_sale_with_pos_container);
        boolean productCreatedIntent = getIntent().getBooleanExtra(getString(R.string.product_create_success), false);
        if (productCreatedIntent) {
            Snackbar.make(mLayout, getString(R.string.product_create_success), Snackbar.LENGTH_LONG).show();
        }

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();
        merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(this).getCurrency(mSessionManager.getCurrency()).getSymbol();
        customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, customerId).blockingGet();

        mProductsAdapter = new ProductsAdapter();
        orderSummaryAdapter = new OrderSummaryAdapter();
        executor = Executors.newSingleThreadExecutor();
        orderSummaryAdapter.setExecutor(executor);
        mProductsAdapter.setExecutor(executor);

        findViewById(R.id.order_summary_bs_wrapper).bringToFront();

        mOrderSummaryBottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
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
            public void onSlide(@NonNull final View bottomSheet, float slideOffset) {

                Runnable draggingStateUpAction = () -> {
                    //after fully expanded the ony way is down
                    if (orderSummaryBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        collapsedToolbar.setVisibility(View.GONE);
                        collapsedToolbar.setAlpha(0f);
                        orderSummaryExpandedToolbar.setAlpha(1f);
                    }
                };

                Runnable draggingStateDownAction = () -> {
                    //after collapsed the only way is up
                    if (orderSummaryBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        orderSummaryExpandedToolbar.setVisibility(View.GONE);
                        collapsedToolbar.setAlpha(1f);
                        orderSummaryExpandedToolbar.setAlpha(0f);
                    }
                };

                if (orderSummaryDraggingStateUp) {
                    //when fully expanded and dragging down
                    orderSummaryExpandedToolbar.setVisibility(View.VISIBLE);
                    orderSummaryExpandedToolbar.animate().alpha(slideOffset).setDuration((long) slideOffset); //expanded toolbar will fade out

                    float offset = 1f - slideOffset;
                    collapsedToolbar.setVisibility(View.VISIBLE);
                    collapsedToolbar.animate().alpha(offset).setDuration((long) offset).withEndAction(draggingStateDownAction); //collapsed toolbar will to fade in

                }
                else {
                    //when collapsed and you are dragging up
                    float offset = 1f - slideOffset; //collapsed toolbar will fade out
                    collapsedToolbar.setVisibility(View.VISIBLE);
                    collapsedToolbar.animate().alpha(offset).setDuration((long) offset);

                    orderSummaryExpandedToolbar.setVisibility(View.VISIBLE);
                    orderSummaryExpandedToolbar.animate().alpha(slideOffset).setDuration((long) slideOffset).withEndAction(draggingStateUpAction); //expanded toolbar will fade in

                }
            }
        };

        customerAutoCompleteDialog = CustomerAutoCompleteDialog.newInstance(getString(R.string.order_owner));
        customerAutoCompleteDialog.setSelectedCustomerListener(this);

        payOptionsDialog = PayOptionsDialog.newInstance();
        payOptionsDialog.setListener(this);

        EmptyRecyclerView productsRecyclerView = findViewById(R.id.points_sale_order_items_rv);
        assert productsRecyclerView != null;
        setupProductsRecyclerView(productsRecyclerView);

        EmptyRecyclerView orderSummaryRecyclerView = findViewById(R.id.order_summary_recycler_view);
        assert orderSummaryRecyclerView != null;
        setUpOrderSummaryRecyclerView(orderSummaryRecyclerView);

        setUpBottomSheetView();

    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
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

        mProductsRecyclerView = recyclerView;

        mProductsRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, 2);
        mProductsRecyclerView.setLayoutManager(mLayoutManager);
        mProductsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mProductsRecyclerView.setAdapter(mProductsAdapter);
        mProductsRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mProductsRecyclerView.setEmptyView(emptyView);
    }

    private void setUpOrderSummaryRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.empty_cart);
        BrandButtonNormal addToCartBtn = emptyView.findViewById(R.id.add_to_cart);
        addToCartBtn.setOnClickListener(view -> showBottomSheet(false));

        mOrderSummaryRecyclerView = recyclerView;
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

    private void setUpBottomSheetView() {
        collapsedToolbar = findViewById(R.id.order_summary_collapsed_toolbar);
        proceedToCheckoutBtn = collapsedToolbar.findViewById(R.id.proceed_to_check_out);
        cartCountImageView = proceedToCheckoutBtn.getCartImageView();
        orderSummaryExpandedToolbar = findViewById(R.id.order_summary_expanded_toolbar);
        orderSummaryCheckoutWrapper = findViewById(R.id.order_summary_checkout_wrapper);
        orderSummaryCheckoutBtn = findViewById(R.id.order_summary_checkout_btn);
        orderSummaryBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.order_summary_bs_wrapper));
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

        RxView.clicks(orderSummaryCheckoutBtn).subscribe(o -> payOptionsDialog.show(getSupportFragmentManager(), PayOptionsDialog.TAG));

        if (orderSummaryBottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
            ((UserLockBottomSheetBehavior) orderSummaryBottomSheetBehavior).setAllowUserDragging(false);
        }

        findViewById(R.id.clear_cart).setOnClickListener(view -> new AlertDialog.Builder(mContext)
                .setTitle("Are you sure?")
                .setMessage("All items will be permanently removed from your cart!")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    mSelectedProducts.clear();
                    totalCharge = 0;
                    orderSummaryAdapter.queryAsync();
                    mProductsAdapter.queryAsync();
                    refreshCartCount();
                    setCheckoutValue();
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel())
                .setIcon(AppCompatResources.getDrawable(mContext, android.R.drawable.ic_dialog_alert))
                .show());

        orderSummaryExpandedToolbar.setNavigationOnClickListener(view -> showBottomSheet(false));
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

    @Override
    public void onCustomerSelected(@NonNull CustomerEntity customerEntity) {
        mSelectedCustomer = customerEntity;
        createSale();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            searchFilterText = null;
            ((ProductsAdapter) mProductsRecyclerView.getAdapter()).getFilter().filter(null);
        }
        else {
            ((ProductsAdapter) mProductsRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        mProductsRecyclerView.scrollToPosition(0);
        return true;
    }

    @Override
    public void onPayWithCashClick() {
        CashPaymentDialog cashPaymentDialog = CashPaymentDialog.newInstance(totalCharge);
        cashPaymentDialog.setListener(this);
        cashPaymentDialog.show(getSupportFragmentManager(), CashPaymentDialog.TAG);
    }

    @Override
    public void onPayWithCardClick() {
        CardPaymentDialog cardPaymentDialog = CardPaymentDialog.newInstance(totalCharge);
        cardPaymentDialog.setListener(this);
        cardPaymentDialog.show(getSupportFragmentManager(), CardPaymentDialog.TAG);
    }

    @Override
    public void onPayWithInvoice() {
        if (!AccountGeneral.isAccountActive(mContext)) {
            Snackbar.make(mLayout,
                    "Your subscription has expired, update subscription to checkout by invoice",
                    Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                startActivity(intent);
            }).show();
        } else {
            Intent startInvoiceIntent = new Intent(this, InvoicePayActivity.class);
            Bundle bundle = new Bundle();
            HashMap<Integer, Integer> hashMap = new HashMap<>();
            ArrayList<Integer> productIds = new ArrayList<>();
            for (int i = 0; i < mSelectedProducts.size(); i++) {
                    hashMap.put(mSelectedProducts.keyAt(i), mSelectedProducts.valueAt(i));
                    productIds.add(mSelectedProducts.keyAt(i));
            }

            bundle.putIntegerArrayList(Constants.SELECTED_PRODUCTS, productIds);
            if (mSelectedCustomer != null){
                startInvoiceIntent.putExtra(Constants.CUSTOMER_ID, mSelectedCustomer.getId());
                startInvoiceIntent.putExtra(Constants.CHARGE, totalCharge);
                startInvoiceIntent.putExtra(Constants.HASH_MAP, hashMap);
            } else {
                startInvoiceIntent.putExtra(Constants.CHARGE, totalCharge);
                startInvoiceIntent.putExtra(Constants.HASH_MAP, hashMap);
            }
            startInvoiceIntent.putExtras(bundle);
            startInvoiceIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startInvoiceIntent);

        }

    }

    @Override
    public void onCashPaymentDialogComplete(boolean showCustomerDialog) {
        isPaidWithCash = true;
        if (showCustomerDialog && customerId == 0) {
            customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
        } else if (showCustomerDialog && customerId > 0) {
            createSale();
        } else {
            createSale();
        }
    }

    private void createSale() {
        DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
        Integer lastSaleId = databaseManager.getLastSaleRecordId();

        SaleEntity newSaleEntity = new SaleEntity();
        if (lastSaleId == null) {
            newSaleEntity.setId(1);
        } else {
            newSaleEntity.setId(lastSaleId + 1);
        }
        newSaleEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
        newSaleEntity.setMerchant(merchantEntity);
        newSaleEntity.setPayedWithCard(isPaidWithCard);
        newSaleEntity.setPayedWithCash(isPaidWithCash);
        newSaleEntity.setPayedWithMobile(isPaidWithMobile);
        newSaleEntity.setTotal(totalCharge);
        newSaleEntity.setSynced(false);
        newSaleEntity.setCustomer(mSelectedCustomer);

        mDataStore.upsert(newSaleEntity).subscribe(saleEntity -> {
            ArrayList<Integer> productIds = new ArrayList<>();
            for (int i = 0; i < mSelectedProducts.size(); i++) {
                productIds.add(mSelectedProducts.keyAt(i));
            }
            Result<ProductEntity> result = mDataStore.select(ProductEntity.class)
                .where(ProductEntity.ID.in(productIds))
                .orderBy(ProductEntity.UPDATED_AT.desc())
                .get();
            List<ProductEntity> productEntities = result.toList();

            Integer lastTransactionId = databaseManager.getLastTransactionRecordId();
            ArrayList<Integer> newTransactionIds = new ArrayList<>();
            for (int x = 0; x < productEntities.size(); x++) {
                if (lastTransactionId == null) {
                    newTransactionIds.add(x, x + 1);
                } else {
                    newTransactionIds.add(x, (lastTransactionId + x + 1));
                }
            }

            for (int i = 0; i < productEntities.size(); i++) {
                ProductEntity product = productEntities.get(i);
                LoyaltyProgramEntity loyaltyProgram = product.getLoyaltyProgram();
                if (loyaltyProgram != null) {
                    SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
                    transactionEntity.setId(newTransactionIds.get(i));

                    String template = "%.2f";
                    double tc = product.getPrice() * mSelectedProducts.get(product.getId());
                    double totalCost = Double.valueOf(String.format(Locale.UK, template, tc));

                    transactionEntity.setAmount(totalCost);
                    transactionEntity.setMerchantLoyaltyProgramId(loyaltyProgram.getId());

                    if (loyaltyProgram.getProgramType().equals(getString(R.string.simple_points))) {
                        transactionEntity.setPoints(Double.valueOf(totalCost).intValue());
                        transactionEntity.setProgramType(getString(R.string.simple_points));
                    } else if (loyaltyProgram.getProgramType().equals(getString(R.string.stamps_program))) {
                        int stampsEarned = mSelectedProducts.get(product.getId());
                        transactionEntity.setStamps(stampsEarned);
                        transactionEntity.setProgramType(getString(R.string.stamps_program));
                    }
                    transactionEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
                    transactionEntity.setProductId(product.getId());
                    if (mSelectedCustomer != null) {
                        transactionEntity.setUserId(mSelectedCustomer.getUserId());
                        transactionEntity.setCustomer(mSelectedCustomer);
                    }

                    transactionEntity.setSynced(false);
                    transactionEntity.setSale(saleEntity);
                    transactionEntity.setMerchant(merchantEntity);
                    mDataStore.upsert(transactionEntity).subscribe(/*no-op*/);

                    if (i + 1 == productEntities.size()) {
                        Log.e("jjjj", "hhh");
                        SyncAdapter.performSync(mContext, mSessionManager.getEmail());
                        Log.e("jjjj", "after");

                        Completable.complete()
                            .delay(1, TimeUnit.SECONDS)
                            .compose(bindToLifecycle())
                            .doOnComplete(() -> {
                                Bundle bundle = new Bundle();
                                if (mSelectedCustomer != null) {
                                    bundle.putInt(Constants.CUSTOMER_ID, mSelectedCustomer.getId());
                                }

                                @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems = new HashMap<>(mSelectedProducts.size());
                                for (int x = 0; x < mSelectedProducts.size(); x++) {
                                    orderSummaryItems.put(mSelectedProducts.keyAt(x), mSelectedProducts.valueAt(x));
                                }
                                bundle.putSerializable(Constants.ORDER_SUMMARY_ITEMS, orderSummaryItems);

                                Intent intent = new Intent(mContext, SaleWithPosConfirmationActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            })
                            .subscribe();
                    }
                }
            }
        });
    }

    @Override
    public void onCardPaymentDialogComplete(boolean showCustomerDialog) {
        isPaidWithCard = true;
        if (showCustomerDialog) {
            customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
        } else {
            createSale();
        }
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
            if (mSelectedProducts.get(item.getId()) == 0) {
                holder.binding.productDecrementWrapper.setVisibility(View.GONE);
            } else {
                holder.binding.productDecrementWrapper.setVisibility(View.VISIBLE);
                holder.binding.productCount.setText(getString(R.string.product_count, String.valueOf(mSelectedProducts.get(item.getId()))));
            }
            holder.binding.productPrice.setText(getString(R.string.product_price, merchantCurrencySymbol, String.valueOf(item.getPrice())));
            holder.binding.addImage.bringToFront();
            holder.binding.decrementCount.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_remove_circle_outline_white_24px));
        }

        @Override
        public BindingHolder<PosProductItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
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
                        mProductsAdapter.queryAsync();
                        orderSummaryAdapter.queryAsync();
                        setCheckoutValue();
                    } else {
                        mSelectedProducts.put(product.getId(), (mSelectedProducts.get(product.getId()) + 1));
                        mProductsAdapter.queryAsync();
                        orderSummaryAdapter.queryAsync();
                        setCheckoutValue();
                    }

                    binding.productDecrementWrapper.setVisibility(View.VISIBLE);
                    makeFlyAnimation(posProductItemBinding.productImageCopy, cartCountImageView);
                }
            });

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
                            posProductItemBinding.productCount.setText(getString(R.string.product_count, String.valueOf(newValue)));
                        }
                    }
                }
            });
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

    private class OrderSummaryAdapter extends QueryRecyclerAdapter<ProductEntity, BindingHolder<OrderSummaryItemBinding>> {

        OrderSummaryAdapter() {
            super(ProductEntity.$TYPE);
        }

        @Override
        public Result<ProductEntity> performQuery() {
            ArrayList<Integer> ids = new ArrayList<>();
            for (int i = 0; i < mSelectedProducts.size(); i++) {
                ids.add(mSelectedProducts.keyAt(i));
            }
            return mDataStore.select(ProductEntity.class).where(ProductEntity.ID.in(ids)).orderBy(ProductEntity.UPDATED_AT.desc()).get();
        }

        @SuppressLint("CheckResult")
        @Override
        public void onBindViewHolder(ProductEntity item, BindingHolder<OrderSummaryItemBinding> holder, int position) {
            holder.binding.setProduct(item);
            RequestOptions options = new RequestOptions();
            options.fitCenter().centerCrop().apply(RequestOptions.placeholderOf(
                    AppCompatResources.getDrawable(mContext, R.drawable.ic_photo_black_24px)
            ));
            Glide.with(mContext)
                    .load(item.getPicture())
                    .apply(options)
                    .into(holder.binding.productImage);

            holder.binding.productName.setText(item.getName());
            holder.binding.deleteCartItem.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_close_white_24px));
            holder.binding.orderItemIncDecBtn.setNumber(String.valueOf(mSelectedProducts.get(item.getId())));

            String template = "%.2f";
            double totalCostOfItem = item.getPrice() * mSelectedProducts.get(item.getId());
            String cText = String.format(Locale.UK, template, totalCostOfItem);
            holder.binding.productCost.setText(getString(R.string.product_price, merchantCurrencySymbol, cText));
        }

        @Override
        public BindingHolder<OrderSummaryItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final OrderSummaryItemBinding binding = OrderSummaryItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            binding.deleteCartItem.setTag(binding);

            binding.deleteCartItem.setOnClickListener(view -> new AlertDialog.Builder(mContext)
                    .setTitle("Are you sure?")
                    .setMessage("This item will be permanently removed from your cart!")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        OrderSummaryItemBinding orderSummaryItemBinding = (OrderSummaryItemBinding) view.getTag();
                        if (orderSummaryItemBinding != null && orderSummaryItemBinding.getProduct() != null) {
                            mSelectedProducts.delete(orderSummaryItemBinding.getProduct().getId());
                            orderSummaryAdapter.queryAsync();
                            mProductsAdapter.queryAsync();
                            refreshCartCount();
                            setCheckoutValue();
                        }
                    })
                    .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel())
                    .setIcon(AppCompatResources.getDrawable(mContext, android.R.drawable.ic_dialog_alert))
                    .show());

            binding.orderItemIncDecBtn.setOnValueChangeListener((view, oldValue, newValue) -> setProductCountValue(newValue, binding.getProduct().getId()));
            return new BindingHolder<>(binding);
        }
    }

    @Override
    protected void onResume() {
        mProductsAdapter.queryAsync();
        orderSummaryAdapter.queryAsync();

        if (myAlertDialog == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                myAlertDialog = MyAlertDialog.newInstance(android.R.style.Theme_Material_Dialog_Alert);
            } else {
                myAlertDialog = new MyAlertDialog();
            }
        }
        myAlertDialog.setShowNegativeButton(false);

        List<ProductEntity> productEntities = mProductsAdapter.performQuery().toList();
        List<ProductEntity> productsWithoutLoyaltyProgram = new ArrayList<>();
        for (ProductEntity productEntity: productEntities) {
            if (productEntity.getLoyaltyProgram() == null) {
                productsWithoutLoyaltyProgram.add(productEntity);
            }
        }
        if (!productsWithoutLoyaltyProgram.isEmpty()) {
            myAlertDialog.setTitle("Products Notice");
            if (productEntities.size() == productsWithoutLoyaltyProgram.size()) {
                myAlertDialog.setMessage("Your products or services don't have loyalty programs set. For each product, go to the products edit screen, select a loyalty program and save.");
                myAlertDialog.setPositiveButton(getString(R.string.pref_my_products_title), (dialogInterface, i) -> {
                    Intent intent = new Intent(mContext, ProductListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                });
            } else if (productsWithoutLoyaltyProgram.size() == 1){
                myAlertDialog.setMessage("1 of your products or services don't have a loyalty program set. Click the button below, select a loyalty program and save.");
                myAlertDialog.setPositiveButton(getString(R.string.update_product), (dialogInterface, i) -> {
                    Intent intent = new Intent(mContext, ProductDetailActivity.class);
                    intent.putExtra(ProductDetailActivity.ARG_ITEM_ID, productsWithoutLoyaltyProgram.get(0).getId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                });
            } else {
                myAlertDialog.setMessage(productsWithoutLoyaltyProgram.size() + " of your products or services " + "don't have loyalty programs set. For each product, go to the products edit screen. Select a loyalty program and save.");
                myAlertDialog.setPositiveButton(getString(R.string.pref_my_products_title), (dialogInterface, i) -> {
                    Intent intent = new Intent(mContext, ProductListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                });
            }
            myAlertDialog.setCancelable(false);
            if (Foreground.get().isForeground()) {
                myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);
            }
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mProductsAdapter.close();
        orderSummaryAdapter.close();
        super.onDestroy();
    }

    private void refreshCartCount() {
        proceedToCheckoutBtn.setCartCount(String.valueOf(mSelectedProducts.size()));
    }

    private void setProductCountValue(int newValue, int productId) {
        if (newValue > 0) {
            mSelectedProducts.put(productId, newValue);
            mProductsAdapter.queryAsync();
            orderSummaryAdapter.queryAsync();
            refreshCartCount();
            setCheckoutValue();
        } else {
            mSelectedProducts.delete(productId);
            mProductsAdapter.queryAsync();
            orderSummaryAdapter.queryAsync();
            refreshCartCount();
            setCheckoutValue();
        }
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
                    public void onAnimationEnd(Animator animation) {
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
                                    createSale();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems = new HashMap<>(mSelectedProducts.size());
        for (int x = 0; x < mSelectedProducts.size(); x++) {
            orderSummaryItems.put(mSelectedProducts.keyAt(x), mSelectedProducts.valueAt(x));
        }
        outState.putSerializable(KEY_SELECTED_PRODUCTS_STATE, orderSummaryItems);

        Parcelable productsListState = mProductsRecyclerView.getLayoutManager().onSaveInstanceState();
        Parcelable orderSummaryListState = mOrderSummaryRecyclerView.getLayoutManager().onSaveInstanceState();
        outState.putParcelable(KEY_PRODUCTS_RECYCLER_STATE, productsListState);
        outState.putParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE, orderSummaryListState);

        if (mSelectedCustomer != null) {
            outState.putInt(KEY_SAVED_CUSTOMER_ID, mSelectedCustomer.getId());
        }

        super.onSaveInstanceState(outState);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Parcelable productsListState = savedInstanceState.getParcelable(KEY_PRODUCTS_RECYCLER_STATE);
        Parcelable orderSummaryListState = savedInstanceState.getParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE);

        /*restore RecyclerView state*/
        if (productsListState != null) {
            mProductsRecyclerView.getLayoutManager().onRestoreInstanceState(productsListState);
        }
        if (orderSummaryListState != null) {
            mOrderSummaryRecyclerView.getLayoutManager().onRestoreInstanceState(orderSummaryListState);
        }

        @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems = (HashMap<Integer, Integer>) savedInstanceState.getSerializable(KEY_SELECTED_PRODUCTS_STATE);
        if (orderSummaryItems != null) {
            for (Map.Entry<Integer, Integer> orderItem: orderSummaryItems.entrySet()) {
                mSelectedProducts.put(orderItem.getKey(), orderItem.getValue());
                setProductCountValue(orderItem.getValue(), orderItem.getKey());
            }
        }

        if (savedInstanceState.containsKey(KEY_SAVED_CUSTOMER_ID)) {
            mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, savedInstanceState.getInt(KEY_SAVED_CUSTOMER_ID)).blockingGet();
        }

        showBottomSheet(false);
        ViewTreeObserver treeObserver = proceedToCheckoutBtn.getViewTreeObserver();
        treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver obs = proceedToCheckoutBtn.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
                proceedToCheckoutBtnHeight = proceedToCheckoutBtn.getMeasuredHeight();
                orderSummaryBottomSheetBehavior.setPeekHeight(proceedToCheckoutBtnHeight);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (orderSummaryBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            showBottomSheet(false);
        } else {
            super.onBackPressed();
        }
    }
}
