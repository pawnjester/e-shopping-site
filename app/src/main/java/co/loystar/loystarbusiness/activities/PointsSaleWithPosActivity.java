package co.loystar.loystarbusiness.activities;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jakewharton.rxbinding2.view.RxView;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.joda.time.DateTime;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.OrderSummaryItemBinding;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Product;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.CircleAnimationUtil;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.OrderItemDividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.UserLockBottomSheetBehavior;
import co.loystar.loystarbusiness.utils.ui.buttons.AddCustomerButton;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.CartCountButton;
import co.loystar.loystarbusiness.utils.ui.buttons.FullRectangleButton;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

public class PointsSaleWithPosActivity extends RxAppCompatActivity
        implements CustomerAutoCompleteDialog.SelectedCustomerListener {

    private static final String TAG = PointsSaleWithPosActivity.class.getSimpleName();
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
    private int mProgramId;
    CustomerAutoCompleteDialog customerAutoCompleteDialog;

    /*Views*/
    private View collapsedToolbar;
    private Toolbar orderSummaryExpandedToolbar;
    private FullRectangleButton orderSummaryCheckoutBtn;
    private CartCountButton proceedToCheckoutBtn;
    private View orderSummaryCheckoutWrapper;
    private CustomerEntity mSelectedCustomer;
    private ImageView cartCountImageView;
    private int proceedToCheckoutBtnHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points_sale_with_pos);
        Toolbar toolbar = findViewById(R.id.activity_points_sale_with_pos_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();
        merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(this).getCurrency(mSessionManager.getCurrency()).getSymbol();
        mProgramId = getIntent().getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);

        mProductsAdapter = new ProductsAdapter();
        orderSummaryAdapter = new OrderSummaryAdapter();
        executor = Executors.newSingleThreadExecutor();
        orderSummaryAdapter.setExecutor(executor);
        mProductsAdapter.setExecutor(executor);

        mOrderSummaryBottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        orderSummaryDraggingStateUp = true;
                        if (orderSummaryBottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
                            ((UserLockBottomSheetBehavior) orderSummaryBottomSheetBehavior).setAllowUserDragging(true);
                        }
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        orderSummaryDraggingStateUp = false;
                        if (mSelectedCustomer == null) {
                            if (orderSummaryBottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
                                ((UserLockBottomSheetBehavior) orderSummaryBottomSheetBehavior).setAllowUserDragging(false);
                            }
                        }
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        if (mSelectedCustomer == null) {
                            if (orderSummaryBottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
                                ((UserLockBottomSheetBehavior) orderSummaryBottomSheetBehavior).setAllowUserDragging(false);
                            }
                        }
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
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

        EmptyRecyclerView productsRecyclerView = findViewById(R.id.points_sale_order_items_rv);
        assert productsRecyclerView != null;
        setupProductsRecyclerView(productsRecyclerView);

        EmptyRecyclerView orderSummaryRecyclerView = findViewById(R.id.order_summary_recycler_view);
        assert orderSummaryRecyclerView != null;
        setUpOrderSummaryRecyclerView(orderSummaryRecyclerView);

        setUpBottomSheetView();
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
            startActivity(intent);
        });

        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, 3);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mProductsAdapter);
        recyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        recyclerView.setEmptyView(emptyView);
    }

    private void setUpOrderSummaryRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.empty_cart);
        BrandButtonNormal addToCartBtn = emptyView.findViewById(R.id.add_to_cart);
        addToCartBtn.setOnClickListener(view -> orderSummaryBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        recyclerView.addItemDecoration(new OrderItemDividerItemDecoration(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(orderSummaryAdapter);
        recyclerView.setEmptyView(emptyView);

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

        RxView.clicks(proceedToCheckoutBtn).subscribe(o -> {
            if (mSelectedCustomer == null) {
                if (!customerAutoCompleteDialog.isAdded()) {
                    customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
                }
            } else {
                showOrderSummaryView(mSelectedCustomer);
            }
        });

        RxView.clicks(orderSummaryCheckoutBtn).subscribe(o -> {
            ArrayList<Integer> ids = new ArrayList<>();
            for (int i = 0; i < mSelectedProducts.size(); i++) {
                ids.add(mSelectedProducts.keyAt(i));
            }
            Result<ProductEntity> result = mDataStore.select(ProductEntity.class)
                    .where(ProductEntity.ID.in(ids))
                    .orderBy(ProductEntity.UPDATED_AT.desc())
                    .get();

            List<ProductEntity> productEntities = result.toList();
            SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
            DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
            SalesTransactionEntity lastTransactionRecord = databaseManager.getMerchantTransactionsLastRecord(mSessionManager.getMerchantId());
            int totalCustomerPoints  = databaseManager.getTotalCustomerPointsForProgram(mProgramId, mSelectedCustomer.getId());
            for (int i = 0; i < productEntities.size(); i ++) {
                ProductEntity product = productEntities.get(i);
                String template = "%.2f";
                double tc = product.getPrice() * mSelectedProducts.get(product.getId());
                int totalCost = Double.valueOf(String.format(Locale.UK, template, tc)).intValue();

                // set temporary id
                if (lastTransactionRecord == null) {
                    transactionEntity.setId(1);
                } else {
                    int id = lastTransactionRecord.getId() + i + 1;
                    transactionEntity.setId(id);
                }
                transactionEntity.setSynced(false);
                transactionEntity.setAmount(totalCost);
                transactionEntity.setMerchantLoyaltyProgramId(mProgramId);
                transactionEntity.setPoints(totalCost);
                transactionEntity.setStamps(0);
                transactionEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
                transactionEntity.setProductId(product.getId());
                transactionEntity.setProgramType(getString(R.string.simple_points));
                if (mSelectedCustomer != null) {
                    transactionEntity.setUserId(mSelectedCustomer.getUserId());
                }

                transactionEntity.setMerchant(merchantEntity);
                transactionEntity.setCustomer(mSelectedCustomer);

                if (i + 1 == productEntities.size()) {
                    transactionEntity.setSendSms(true);
                        databaseManager.insertNewSalesTransaction(transactionEntity);
                        SyncAdapter.performSync(mContext, mSessionManager.getEmail());

                        final Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            Bundle bundle = new Bundle();
                            int tp = totalCustomerPoints + Double.valueOf(totalCharge).intValue();
                            bundle.putInt(Constants.TOTAL_CUSTOMER_POINTS, tp);
                            bundle.putBoolean(Constants.PRINT_RECEIPT, true);
                            bundle.putBoolean(Constants.SHOW_CONTINUE_BUTTON, true);
                            bundle.putInt(Constants.LOYALTY_PROGRAM_ID, mProgramId);
                            bundle.putInt(Constants.CUSTOMER_ID, mSelectedCustomer.getId());

                            @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems = new HashMap<>(mSelectedProducts.size());
                            for (int x = 0; x < mSelectedProducts.size(); x++) {
                                orderSummaryItems.put(mSelectedProducts.keyAt(x), mSelectedProducts.valueAt(x));
                            }
                            bundle.putSerializable(Constants.ORDER_SUMMARY_ITEMS, orderSummaryItems);

                            Intent intent = new Intent(mContext, TransactionsConfirmation.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtras(bundle);
                            startActivity(intent);
                        }, 200);
                } else {
                    transactionEntity.setSendSms(false);
                    databaseManager.insertNewSalesTransaction(transactionEntity);
                    SyncAdapter.performSync(mContext, mSessionManager.getEmail());
                }
            }
        });

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
                    addItemToCart();
                    setCheckoutValue();
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel())
                .setIcon(AppCompatResources.getDrawable(mContext, android.R.drawable.ic_dialog_alert))
                .show());

        orderSummaryExpandedToolbar.setNavigationOnClickListener(view -> orderSummaryBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));
    }

    private void showOrderSummaryView(@Nullable CustomerEntity customerEntity) {
        View customerDetailWrapper = findViewById(R.id.customerDetailWrapper);
        View AddCustomerWrapper = findViewById(R.id.addCustomerWrapper);
        AddCustomerButton addCustomerButton = AddCustomerWrapper.findViewById(R.id.addCustomerButton);
        RxView.clicks(addCustomerButton).subscribe(o -> {
            if (!customerAutoCompleteDialog.isAdded()) {
                customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
            }
        });
        if (customerEntity == null) {
            AddCustomerWrapper.setVisibility(View.VISIBLE);
            customerDetailWrapper.setVisibility(View.GONE);
        } else {
            AddCustomerWrapper.setVisibility(View.GONE);
            customerDetailWrapper.setVisibility(View.VISIBLE);

            ((TextView) customerDetailWrapper.findViewById(R.id.customerName)).setText(customerEntity.getFirstName());
            ((TextView) customerDetailWrapper.findViewById(R.id.customerNumber)).setText(customerEntity.getPhoneNumber());

            RxView.clicks(customerDetailWrapper.findViewById(R.id.changeCustomerBtn)).subscribe(o -> {
                mSelectedCustomer = null;
                AddCustomerWrapper.setVisibility(View.VISIBLE);
                customerDetailWrapper.setVisibility(View.GONE);
            });
        }
        orderSummaryBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showCheckoutBtn(boolean show) {
        if (show) {
            orderSummaryCheckoutWrapper.setVisibility(View.VISIBLE);
        } else {
            orderSummaryCheckoutWrapper.setVisibility(View.GONE);
        }
    }

    private void showProceedToCheckoutBtn(boolean show) {
        if (show) {
            orderSummaryBottomSheetBehavior.setPeekHeight(proceedToCheckoutBtnHeight);
        } else {
            orderSummaryBottomSheetBehavior.setPeekHeight(0);
        }
    }

    @Override
    public void onCustomerSelected(@NonNull CustomerEntity customerEntity) {
        mSelectedCustomer = customerEntity;
        showOrderSummaryView(mSelectedCustomer);
    }

    private class ProductsAdapter extends QueryRecyclerAdapter<ProductEntity, BindingHolder<PosProductItemBinding>> {

        ProductsAdapter() {
            super(ProductEntity.$TYPE);
        }

        @Override
        public Result<ProductEntity> performQuery() {
            if (merchantEntity == null) {
                return null;
            }

            Selection<ReactiveResult<ProductEntity>> productsSelection = mDataStore.select(ProductEntity.class);
            productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
            productsSelection.where(ProductEntity.DELETED.notEqual(true));

            return productsSelection.orderBy(ProductEntity.UPDATED_AT.desc()).get();
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
            options.centerCrop().apply(RequestOptions.placeholderOf(
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
                        if (orderSummaryItemBinding != null) {
                            mSelectedProducts.delete(orderSummaryItemBinding.getProduct().getId());
                            orderSummaryAdapter.queryAsync();
                            mProductsAdapter.queryAsync();
                            addItemToCart();
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
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mProductsAdapter.close();
        orderSummaryAdapter.close();
        super.onDestroy();
    }

    private void addItemToCart() {
        proceedToCheckoutBtn.setCartCount(String.valueOf(mSelectedProducts.size()));
    }

    private void setProductCountValue(int newValue, int productId) {
        if (newValue > 0) {
            mSelectedProducts.put(productId, newValue);
            orderSummaryAdapter.queryAsync();
            orderSummaryAdapter.queryAsync();
            addItemToCart();
            setCheckoutValue();
        } else {
            mSelectedProducts.delete(productId);
            orderSummaryAdapter.queryAsync();
            orderSummaryAdapter.queryAsync();
            addItemToCart();
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
                        addItemToCart();
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
                                    showOrderSummaryView(customerEntity);
                                }
                            });
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
