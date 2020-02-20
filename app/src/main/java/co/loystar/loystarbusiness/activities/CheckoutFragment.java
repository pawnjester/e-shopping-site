package co.loystar.loystarbusiness.activities;


import android.animation.Animator;
import android.annotation.SuppressLint;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.DateTime;

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
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.databinding.OrderSummaryItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.models.viewmodel.SharedViewModel;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.Foreground;
import co.loystar.loystarbusiness.utils.ui.CircleAnimationUtil;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.OrderItemDividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
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
import io.requery.reactivex.ReactiveEntityStore;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class CheckoutFragment extends Fragment
        implements PayOptionsDialog.PayOptionsDialogClickListener,
        CashPaymentDialog.CashPaymentDialogOnCompleteListener,
        CardPaymentDialog.CardPaymentDialogOnCompleteListener,
        CustomerAutoCompleteDialog.SelectedCustomerListener{

    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    private ReactiveEntityStore<Persistable> mDataStore;
    private Context context;
    private EmptyRecyclerView mOrdersRecyclerView;
    private OrderSummaryAdapter mOrderSummaryAdapter;
    private View orderSummaryCheckoutWrapper;
    private String merchantCurrencySymbol;
    private ExecutorService executor;
    private SparseIntArray mSelectedProducts;
    private CustomerEntity mSelectedCustomer;
    private MyAlertDialog myAlertDialog;
    private final String KEY_PRODUCTS_RECYCLER_STATE = "products_recycler_state";
    private final String KEY_SELECTED_PRODUCTS_STATE = "selected_products_state";
    private final String KEY_ORDER_SUMMARY_RECYCLER_STATE = "order_summary_recycler_state";
    private final String KEY_SAVED_CUSTOMER_ID = "saved_customer_id";
    private boolean isPaidWithCash = false;
    private boolean isPaidWithCard = false;
    private boolean isPaidWithMobile = false;
    private int customerId;
    private double totalCharge = 0;
    private PayOptionsDialog payOptionsDialog;
    private SharedViewModel viewModel;
    private FullRectangleButton orderSummaryCheckoutBtn;
    private CustomerAutoCompleteDialog customerAutoCompleteDialog;
    private MutableLiveData<SparseIntArray> selectedProductObserver = new MutableLiveData<>();
    private SparseIntArray entities;
    private ArrayList<Integer> ids;


    public CheckoutFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkout, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        customerId = viewModel.getCustomer().getValue();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getActivity();
        mDataStore = DatabaseManager.getDataStore(context);
        mSessionManager = new SessionManager(context);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class,
                mSessionManager.getMerchantId()).blockingGet();
        merchantCurrencySymbol = CurrenciesFetcher
                .getCurrencies(context)
                .getCurrency(mSessionManager.getCurrency())
                .getSymbol();
        mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, customerId).blockingGet();

        mOrderSummaryAdapter = new OrderSummaryAdapter();
        executor = Executors.newSingleThreadExecutor();
        mOrderSummaryAdapter.setExecutor(executor);

        payOptionsDialog = PayOptionsDialog.newInstance();
        payOptionsDialog.setListener(this);

        customerAutoCompleteDialog = CustomerAutoCompleteDialog.newInstance(getString(R.string.order_owner));
        customerAutoCompleteDialog.setSelectedCustomerListener(this);

        EmptyRecyclerView orderSummaryRecyclerView = view.findViewById(R.id.order_summary_recycler_view);
        assert orderSummaryRecyclerView != null;
        setUpOrdersRecyclerView(orderSummaryRecyclerView);
        displayCheckout();

        viewModel.getSelectedProducts().observe(getViewLifecycleOwner(), array -> {
            mSelectedProducts = array;
            if (mSelectedProducts != null) {
                setCheckoutValue(mSelectedProducts);
            }
            mOrderSummaryAdapter.updateProduct(array);
        });

    }

    public static Fragment newInstance() {
        return new CheckoutFragment();
    }

    private void displayCheckout() {
        orderSummaryCheckoutWrapper = getActivity().findViewById(R.id.order_summary_checkout_wrapper);
        orderSummaryCheckoutBtn = getActivity().findViewById(R.id.order_summary_checkout_btn);
        RxView.clicks(orderSummaryCheckoutBtn).subscribe(o -> {
            if (!ids.isEmpty()) {
                payOptionsDialog.show(
                        getActivity().getSupportFragmentManager(), PayOptionsDialog.TAG);
            }
        });
        if (entities.size() > 0) {
            getActivity().findViewById(R.id.clear_cart).setOnClickListener(view -> new AlertDialog.Builder(context)
                    .setTitle("Are you sure?")
                    .setMessage("All items will be permanently removed from your cart!")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        mSelectedProducts.clear();
                        totalCharge = 0;
                        mOrderSummaryAdapter.queryAsync();
                        setCheckoutValue(entities);
                        viewModel.setDeleted(1);
                    })
                    .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel())
                    .setIcon(AppCompatResources.getDrawable(context, android.R.drawable.ic_dialog_alert))
                    .show());
        }
    }

    private void setUpOrdersRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = getActivity().findViewById(R.id.empty_cart);
        mOrdersRecyclerView = recyclerView;
        mOrdersRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
        mOrdersRecyclerView.setLayoutManager(mLayoutManager);
        mOrdersRecyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mOrdersRecyclerView.addItemDecoration(new OrderItemDividerItemDecoration(context));
        mOrdersRecyclerView.setAdapter(mOrderSummaryAdapter);
        mOrdersRecyclerView.setEmptyView(emptyView);
    }

    @Override
    public void onPayWithCashClick() {
        CashPaymentDialog cashPaymentDialog = CashPaymentDialog.newInstance(totalCharge);
        cashPaymentDialog.setListener(this);
        cashPaymentDialog.show(getActivity().getSupportFragmentManager(), CashPaymentDialog.TAG);
    }

    @Override
    public void onPayWithCardClick() {
        CardPaymentDialog cardPaymentDialog = CardPaymentDialog.newInstance(totalCharge);
        cardPaymentDialog.setListener(this);
        cardPaymentDialog.show(getActivity().getSupportFragmentManager(), CardPaymentDialog.TAG);
    }

    @Override
    public void onPayWithInvoice() {
        Intent startInvoiceIntent = new Intent(getActivity(), InvoicePayActivity.class);
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

    @Override
    public void onCardPaymentDialogComplete(boolean showCustomerDialog) {
        isPaidWithCard = true;
        if (showCustomerDialog) {
            customerAutoCompleteDialog.show(
                    getActivity().getSupportFragmentManager(),
                    CustomerAutoCompleteDialog.TAG);
        } else {
            createSale(entities);
        }
    }

    @Override
    public void onCashPaymentDialogComplete(boolean showCustomerDialog) {
        isPaidWithCash = true;
        if (showCustomerDialog && customerId == 0) {
            customerAutoCompleteDialog.show(
                    getActivity().getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
        } else if (showCustomerDialog && customerId > 0) {
            createSale(entities);
        } else {
            createSale(entities);
        }
    }

    @Override
    public void onCustomerSelected(@NonNull CustomerEntity customerEntity) {
        mSelectedCustomer = customerEntity;
        createSale(entities);
    }

    private class OrderSummaryAdapter extends
            QueryRecyclerAdapter<ProductEntity, BindingHolder<OrderSummaryItemBinding>> {

        OrderSummaryAdapter() {
            super(ProductEntity.$TYPE);
            entities = new SparseIntArray();
        }

        @Override
        public Result<ProductEntity> performQuery()
        {
            ids = new ArrayList<>();
            for (int i = 0; i < entities.size(); i++) {
                ids.add(entities.keyAt(i));
            }
            return mDataStore.select(ProductEntity.class)
                    .where(ProductEntity.ID.in(ids))
                    .orderBy(ProductEntity.UPDATED_AT.desc()).get();
        }

        public void updateProduct(SparseIntArray newproductEntity){
            entities = newproductEntity;
            mOrderSummaryAdapter.queryAsync();
        }

        @Override
        public void onBindViewHolder(ProductEntity item,
                                     BindingHolder<OrderSummaryItemBinding> holder,
                                     int position) {
            holder.binding.setProduct(item);
            RequestOptions options = new RequestOptions();
            options.fitCenter().centerCrop().apply(RequestOptions.placeholderOf(
                    AppCompatResources.getDrawable(context, R.drawable.ic_photo_black_24px)
            ));
            if (item.getPicture() != null) {
                Glide.with(context)
                        .load(item.getPicture())
                        .apply(options)
                        .into(holder.binding.productImage);
            } else {
                TextDrawable drawable = TextDrawable.builder()
                        .beginConfig().textColor(Color.GRAY)
                        .useFont(Typeface.DEFAULT)
                        .toUpperCase().endConfig()
                        .buildRect(item.getName().substring(0,2), Color.WHITE);
                holder.binding.productImage.setImageDrawable(drawable);
            }

            holder.binding.productName.setText(item.getName());
            holder.binding.deleteCartItem.setImageDrawable(AppCompatResources.getDrawable(
                    context, R.drawable.ic_close_white_24px));
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

            binding.deleteCartItem.setOnClickListener(view -> new AlertDialog.Builder(context)
                    .setTitle("Are you sure?")
                    .setMessage("This item will be permanently removed from your cart!")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        OrderSummaryItemBinding orderSummaryItemBinding = (OrderSummaryItemBinding) view.getTag();
                        if (orderSummaryItemBinding != null && orderSummaryItemBinding.getProduct() != null) {
                            mSelectedProducts.delete(orderSummaryItemBinding.getProduct().getId());
                            mOrderSummaryAdapter.queryAsync();
                            setCheckoutValue(entities);
                            viewModel.setSelectedProducts(mSelectedProducts);
                            viewModel.updateSelected(mSelectedProducts);
                        }
                    })
                    .setNegativeButton("Cancel", (dialogInterface, inst) -> dialogInterface.cancel())
                    .setIcon(AppCompatResources.getDrawable(context, android.R.drawable.ic_dialog_alert))
                    .show());

            binding.orderItemIncDecBtn.setOnValueChangeListener((view, oldValue, newValue) ->
                    setProductCountValue(newValue, binding.getProduct().getId(), entities));
            return new BindingHolder<>(binding);
        }
    }

    private void createSale(SparseIntArray products) {
        DatabaseManager databaseManager = DatabaseManager.getInstance(context);
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
            for (int i = 0; i < products.size(); i++) {
                productIds.add(products.keyAt(i));
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
                    double tc = product.getPrice() * products.get(product.getId());
                    double totalCost = Double.valueOf(String.format(Locale.UK, template, tc));

                    transactionEntity.setAmount(totalCost);
                    transactionEntity.setMerchantLoyaltyProgramId(loyaltyProgram.getId());

                    if (loyaltyProgram.getProgramType().equals(getString(R.string.simple_points))) {
                        transactionEntity.setPoints(Double.valueOf(totalCost).intValue());
                        transactionEntity.setProgramType(getString(R.string.simple_points));
                    } else if (loyaltyProgram.getProgramType().equals(getString(R.string.stamps_program))) {
                        int stampsEarned = products.get(product.getId());
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
                        SyncAdapter.performSync(context, mSessionManager.getEmail());

                        Completable.complete()
                                .delay(1, TimeUnit.SECONDS)
                                .doOnComplete(() -> {
                                    Bundle bundle = new Bundle();
                                    if (mSelectedCustomer != null) {
                                        bundle.putInt(Constants.CUSTOMER_ID, mSelectedCustomer.getId());
                                    }

                                    @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems = new HashMap<>(products.size());
                                    for (int x = 0; x < products.size(); x++) {
                                        orderSummaryItems.put(products.keyAt(x), products.valueAt(x));
                                    }
                                    bundle.putSerializable(Constants.ORDER_SUMMARY_ITEMS, orderSummaryItems);

                                    Intent intent = new Intent(context, SaleWithPosConfirmationActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtras(bundle);
                                    intent.putExtra("isDual", "tablet");
                                    startActivity(intent);
                                })
                                .subscribe();
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        mOrderSummaryAdapter.queryAsync();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        mOrderSummaryAdapter.close();
        super.onDestroy();
    }

    private void setProductCountValue(int newValue, int productId, SparseIntArray array) {
        if (newValue > 0) {
            mSelectedProducts.put(productId, newValue);
            mOrderSummaryAdapter.queryAsync();
            setCheckoutValue(array);
            viewModel.updateSelected(mSelectedProducts);
        } else {
            mSelectedProducts.delete(productId);
            mOrderSummaryAdapter.queryAsync();
            setCheckoutValue(array);
            viewModel.updateSelected(mSelectedProducts);
        }
    }

    private void setCheckoutValue(SparseIntArray mSelectedProducts) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < mSelectedProducts.size(); i++) {
            ids.add(mSelectedProducts.keyAt(i));
        }
        Result<ProductEntity> result = mDataStore.select(ProductEntity.class)
                .where(ProductEntity.ID.in(ids))
                .orderBy(ProductEntity.UPDATED_AT.desc())
                .get();

        double tc = 0;
        for (ProductEntity product: result.toList()) {
            double totalCostOfItem = product.getPrice() * mSelectedProducts.get(product.getId());
            tc += totalCostOfItem;
        }
        String template = "%.2f";
        totalCharge = Double.valueOf(String.format(Locale.UK, template, tc));
        String cText = String.format(Locale.UK, template, totalCharge);
        orderSummaryCheckoutBtn.setText(getString(R.string.charge, merchantCurrencySymbol, cText));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CustomerAutoCompleteDialog.ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data.hasExtra(Constants.CUSTOMER_ID)) {
                    mDataStore.findByKey(CustomerEntity.class, data.getIntExtra(Constants.CUSTOMER_ID, 0))
                            .toObservable()
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(customerEntity -> {
                                if (customerEntity == null) {
                                    Toast.makeText(context, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                                } else {
                                    mSelectedCustomer = customerEntity;
                                    createSale(entities);
                                }
                            });
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

//    @Override
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        if (savedInstanceState != null) {
//            Log.e(">>>save", "not");
//            Parcelable orderSummaryListState =
//                    savedInstanceState.getParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE);
//
//            /*restore RecyclerView state*/
//            if (orderSummaryListState != null) {
//                mOrdersRecyclerView
//                        .getLayoutManager().onRestoreInstanceState(orderSummaryListState);
//            }
////
//            @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems =
//                    (HashMap<Integer, Integer>)
//                            savedInstanceState.getSerializable(KEY_SELECTED_PRODUCTS_STATE);
//            if (orderSummaryItems != null) {
//                for (Map.Entry<Integer, Integer> orderItem: orderSummaryItems.entrySet()) {
//                    mSelectedProducts.put(orderItem.getKey(), orderItem.getValue());
//                    setProductCountValue(orderItem.getValue(), orderItem.getKey(), entities);
//                }
//            }
////
////            if (savedInstanceState.containsKey(KEY_SAVED_CUSTOMER_ID)) {
////                mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class,
////                        savedInstanceState.getInt(KEY_SAVED_CUSTOMER_ID)).blockingGet();
////            }
//        }
//    }
//
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems =
                new HashMap<>(mSelectedProducts.size());
        for (int x = 0; x < mSelectedProducts.size(); x++) {
            orderSummaryItems.put(mSelectedProducts.keyAt(x), mSelectedProducts.valueAt(x));
        }
        outState.putSerializable(KEY_SELECTED_PRODUCTS_STATE, orderSummaryItems);

        Parcelable orderSummaryListState = mOrdersRecyclerView.getLayoutManager().onSaveInstanceState();
        outState.putParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE, orderSummaryListState);

        if (mSelectedCustomer != null) {
            outState.putInt(KEY_SAVED_CUSTOMER_ID, mSelectedCustomer.getId());
        }
        super.onSaveInstanceState(outState);
    }
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            Log.e("not", "saved");
            Parcelable orderSummaryListState = savedInstanceState.getParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE);

            /*restore RecyclerView state*/
            if (orderSummaryListState != null) {
                mOrdersRecyclerView.getLayoutManager().onRestoreInstanceState(orderSummaryListState);
            }
//            @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems =
//                    (HashMap<Integer, Integer>) savedInstanceState.getSerializable(KEY_SELECTED_PRODUCTS_STATE);
//            Log.e("orddd", orderSummaryItems + "");
//            if (orderSummaryItems != null) {
//                for (Map.Entry<Integer, Integer> orderItem: orderSummaryItems.entrySet()) {
//                    mSelectedProducts.put(orderItem.getKey(), orderItem.getValue());
////                    if (i)
////                    setProductCountValue(orderItem.getValue(), orderItem.getKey(), entities);
//                }
//            }

            if (savedInstanceState.containsKey(KEY_SAVED_CUSTOMER_ID)) {
                mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, savedInstanceState.getInt(KEY_SAVED_CUSTOMER_ID)).blockingGet();
            }
        }
    }
}
