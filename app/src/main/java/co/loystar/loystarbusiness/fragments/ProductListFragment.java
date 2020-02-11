package co.loystar.loystarbusiness.fragments;


import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

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
import co.loystar.loystarbusiness.activities.AddProductActivity;
import co.loystar.loystarbusiness.activities.InvoicePayActivity;
import co.loystar.loystarbusiness.activities.ProductDetailActivity;
import co.loystar.loystarbusiness.activities.ProductListActivity;
import co.loystar.loystarbusiness.activities.SaleWithPosConfirmationActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.databinding.OrderSummaryItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.ProductCategory;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Product;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
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
import co.loystar.loystarbusiness.utils.ui.UserLockBottomSheetBehavior;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.CartCountButton;
import co.loystar.loystarbusiness.utils.ui.buttons.FullRectangleButton;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;
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
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;
import static co.loystar.loystarbusiness.activities.ProductListActivity.TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProductListFragment extends Fragment
        implements CustomerAutoCompleteDialog.SelectedCustomerListener,
        PayOptionsDialog.PayOptionsDialogClickListener,
        CashPaymentDialog.CashPaymentDialogOnCompleteListener,
        CardPaymentDialog.CardPaymentDialogOnCompleteListener,
OnBackPressed,
SearchView.OnQueryTextListener{

    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    private ProgressDialog progressDialog;
    private OrderSummaryAdapter orderSummaryAdapter;
    private ReactiveEntityStore<Persistable> mDataStore;
    private Context context;
    private EmptyRecyclerView mProductsRecyclerView;
    private ProductsAdapter mProductAdapter;
    private String merchantCurrencySymbol;
    private ExecutorService executor;
    private CustomerEntity mSelectedCustomer;
    private BottomSheetBehavior.BottomSheetCallback mOrderSummaryBottomSheetCallback;
    private boolean orderSummaryDraggingStateUp = false;
    private BottomSheetBehavior orderSummaryBottomSheetBehavior;
    private View collapsedToolbar;
    private Toolbar orderSummaryExpandedToolbar;
    private FullRectangleButton orderSummaryCheckoutBtn;
    private CartCountButton proceedToCheckoutBtn;
    private View orderSummaryCheckoutWrapper;
    private ImageView cartCountImageView;
    private CustomerAutoCompleteDialog customerAutoCompleteDialog;
    private int proceedToCheckoutBtnHeight = 0;
    private PayOptionsDialog payOptionsDialog;
    private double totalCharge = 0;
    private EmptyRecyclerView mOrderSummaryRecyclerView;
    private SparseIntArray mSelectedProducts = new SparseIntArray();
    private MyAlertDialog myAlertDialog;
    private final String KEY_PRODUCTS_RECYCLER_STATE = "products_recycler_state";
    private final String KEY_SELECTED_PRODUCTS_STATE = "selected_products_state";
    private final String KEY_ORDER_SUMMARY_RECYCLER_STATE = "order_summary_recycler_state";
    private final String KEY_SAVED_CUSTOMER_ID = "saved_customer_id";
    private boolean isPaidWithCash = false;
    private boolean isPaidWithCard = false;
    private boolean isPaidWithMobile = false;
    private int customerId;
    private boolean isDual;
    private String searchFilterText;
    private SharedViewModel viewModel;
    private View sortByCategoryButton;
    private ProductCategoryEntity mSelectedProductCategory;
    private DatabaseManager mDatabaseManager;
    private ApiClient mApiClient;

    public ProductListFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_product_list, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);
        viewModel.getCustomer().observeForever(integer -> {

        });
        customerId = viewModel.getCustomer().getValue();
        viewModel.setSelectedProducts(mSelectedProducts);
        viewModel.getDeleted().observeForever(integer -> {
            if (integer != null) {
                mSelectedProducts = new SparseIntArray();
                mProductAdapter.queryAsync();
            }
        });
        viewModel.getDamn().observeForever(array -> {
            if (array != null){
                mSelectedProducts = array;
                mProductAdapter.queryAsync();
            }
        });
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle bundle = getArguments();
        isDual = bundle.getBoolean("isDual");
        if (!isDual) {
            Toolbar myToolbar = view.findViewById(R.id.toolbar);
            ((AppCompatActivity)getActivity()).setSupportActionBar(myToolbar);
            ActionBar actionbar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
            setHasOptionsMenu(true);
        }
        context = getActivity();
        mDatabaseManager = DatabaseManager.getInstance(context);
        mApiClient = new ApiClient(context);
        mDataStore = DatabaseManager.getDataStore(context);
        mSessionManager = new SessionManager(context);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class,
                mSessionManager.getMerchantId()).blockingGet();
        merchantCurrencySymbol = CurrenciesFetcher
                .getCurrencies(context)
                .getCurrency(mSessionManager.getCurrency())
                .getSymbol();
        mSelectedCustomer = mDataStore.findByKey(CustomerEntity.class, customerId).blockingGet();
        if (isDual) {
//            sortByCategoryButton = view.findViewById(R.id.sortByCategory);
//            sortByCategoryButton.setVisibility(View.VISIBLE);
//
//            SpinnerButton productCategoriesSpinner = view.findViewById(R.id.productCategoriesSelectSpinner);
//            List<ProductCategoryEntity> productCategories = mDatabaseManager
//                    .getMerchantProductCategories(mSessionManager.getMerchantId());
//            if (productCategories.isEmpty()) {
//                SpinnerButton.CreateNewItemListener createNewItemListener = () -> {
//                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
//                    LayoutInflater li = LayoutInflater.from(alertDialogBuilder.getContext());
//                    @SuppressLint("InflateParams") View createCategoryView = li.inflate(R.layout.add_product_category, null);
//                    EditText msgBox = createCategoryView.findViewById(R.id.category_text_box);
//                    TextView charCounterView = createCategoryView.findViewById(R.id.category_name_char_counter);
//
//                    RxTextView.textChangeEvents(msgBox).subscribe(textViewTextChangeEvent -> {
//                        CharSequence s = textViewTextChangeEvent.text();
//                        String char_temp = "%s %s / %s";
//                        String char_temp_unit = s.length() == 1 ? "Character" : "Characters";
//                        String char_counter_text = String.format(char_temp, s.length(), char_temp_unit, 30);
//                        charCounterView.setText(char_counter_text);
//                    });
//
//                    alertDialogBuilder.setView(createCategoryView);
//                    alertDialogBuilder.setTitle("Create new category");
//                    alertDialogBuilder.setPositiveButton("Create", (dialogInterface, i) -> {
//                        if (TextUtils.isEmpty(msgBox.getText().toString())) {
//                            msgBox.setError(getString(R.string.error_name_required));
//                            msgBox.requestFocus();
//                            return;
//                        }
//                        showProgressDialog();
//
//                        try {
//                            JSONObject jsonObject = new JSONObject();
//                            jsonObject.put("name", msgBox.getText().toString());
//                            JSONObject requestData = new JSONObject();
//                            requestData.put("data", jsonObject);
//
//                            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
//                            mApiClient.getLoystarApi(false).addProductCategory(requestBody).enqueue(new Callback<ProductCategory>() {
//                                @Override
//                                public void onResponse(@NonNull Call<ProductCategory> call, @NonNull Response<ProductCategory> response) {
//                                    dismissProgressDialog();
//                                    if (response.isSuccessful()) {
//                                        ProductCategory productCategory = response.body();
//                                        if (productCategory == null) {
//                                            Toast.makeText(context, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
//                                        } else {
//                                            ProductCategoryEntity productCategoryEntity = new ProductCategoryEntity();
//                                            productCategoryEntity.setId(productCategory.getId());
//                                            productCategoryEntity.setDeleted(false);
//                                            productCategoryEntity.setName(productCategory.getName());
//                                            productCategoryEntity.setCreatedAt(new Timestamp(productCategory.getCreated_at().getMillis()));
//                                            productCategoryEntity.setUpdatedAt(new Timestamp(productCategory.getUpdated_at().getMillis()));
//                                            productCategoryEntity.setOwner(merchantEntity);
//
//                                            mDatabaseManager.insertNewProductCategory(productCategoryEntity);
//
//                                            mSelectedProductCategory = merchantEntity.getProductCategories().get(0);
//                                            CharSequence[] spinnerItems = new CharSequence[1];
//                                            spinnerItems[0] = productCategory.getName();
//                                            productCategoriesSpinner.setEntries(spinnerItems);
//                                            productCategoriesSpinner.setSelection(0);
//
//                                            Toast.makeText(context, getString(R.string.product_category_create_success), Toast.LENGTH_LONG).show();
//                                        }
//                                    } else {
//                                        Toast.makeText(context, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
//                                    }
//
//                                }
//
//                                @Override
//                                public void onFailure(@NonNull Call<ProductCategory> call, @NonNull Throwable t) {
//                                    dismissProgressDialog();
//                                    Toast.makeText(context, getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();
//                                }
//                            });
//                        } catch (JSONException e) {
//                            dismissProgressDialog();
//                            e.printStackTrace();
//                        }
//                    });
//                    alertDialogBuilder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
//                    alertDialogBuilder.setCancelable(false);
//
//                    AlertDialog alertDialog = alertDialogBuilder.create();
//                    alertDialog.show();
//                };
//                productCategoriesSpinner.setCreateNewItemListener(createNewItemListener);
//                productCategoriesSpinner.setCreateNewItemDialogTitle("No Categories Found!");
//            } else {
//                CharSequence[] spinnerItems = new CharSequence[productCategories.size()];
//                for (int i = 0; i < productCategories.size(); i++) {
//                    spinnerItems[i] = productCategories.get(i).getName();
//                }
//                productCategoriesSpinner.setEntries(spinnerItems);
//
//                SpinnerButton.OnItemSelectedListener onItemSelectedListener = position -> {
//                    searchFilterText = productCategories.get(position).getName();
//                    mProductAdapter.queryAsync();
//                    ImageView resetProduct = view.findViewById(R.id.sortByCategoryImage);
//                    resetProduct.setVisibility(View.VISIBLE);
//                    resetProduct.setOnClickListener(v -> {
//                        searchFilterText = null;
//                        mProductAdapter.queryAsync();
//                        resetProduct.setVisibility(View.GONE);
//                        productCategoriesSpinner.setSelection(-1);
//                    });
//                };
//                productCategoriesSpinner.setListener(onItemSelectedListener);
//            }
        }

        mProductAdapter = new ProductsAdapter();
        orderSummaryAdapter = new OrderSummaryAdapter();
        executor = Executors.newSingleThreadExecutor();
        orderSummaryAdapter.setExecutor(executor);
        mProductAdapter.setExecutor(executor);
        setHasOptionsMenu(true);

//        if (!isDual) {
//            view.findViewById(R.id.order_summary_bs_wrapper).bringToFront();
//        }

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

        EmptyRecyclerView productsRecyclerview = view.findViewById(R.id.points_sale_order_items_rv);
        assert productsRecyclerview != null;
        setupProductsRecyclerView(productsRecyclerview);

        EmptyRecyclerView orderSummaryRecyclerView = view.findViewById(R.id.order_summary_recycler_view);
        if (!isDual) {
            Log.e("12333", "here");
            assert orderSummaryRecyclerView != null;
            setUpOrderSummaryRecyclerView(orderSummaryRecyclerView);
            setUpBottomSheetView();
        }
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getString(R.string.a_moment));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    private void setupProductsRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = getActivity().findViewById(R.id.empty_items_container);
        ImageView stateWelcomeImageView = emptyView.findViewById(R.id.stateImage);
        TextView stateWelcomeTextView = emptyView.findViewById(R.id.stateIntroText);
        TextView stateDescriptionTextView = emptyView.findViewById(R.id.stateDescriptionText);
        BrandButtonNormal stateActionBtn = emptyView.findViewById(R.id.stateActionBtn);

        String merchantBusinessType = mSessionManager.getBusinessType();
        if (merchantBusinessType.equals(getString(R.string.hair_and_beauty))) {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_no_product_beauty));
        } else if (merchantBusinessType.equals(getString(R.string.fashion_and_accessories))) {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_no_product_fashion));
        } else if (merchantBusinessType.equals(getString(R.string.beverages_and_deserts)) || merchantBusinessType.equals(getString(R.string.bakery_and_pastry))) {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_no_product_food));
        } else {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_no_product_others));
        }

        stateWelcomeTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateDescriptionTextView.setText(getString(R.string.no_products_found));

        stateActionBtn.setText(getString(R.string.start_adding_products_label));
        stateActionBtn.setOnClickListener(view -> {
            Intent intent = new Intent(context, AddProductActivity.class);
            intent.putExtra(Constants.ACTIVITY_INITIATOR, TAG);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        mProductsRecyclerView = recyclerView;
        mProductsRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager;
        if (isDual) {
            mLayoutManager = new GridLayoutManager(context, 4);
        } else {
            mLayoutManager = new GridLayoutManager(context, 2);
        }
        mProductsRecyclerView.setLayoutManager(mLayoutManager);
        mProductsRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium)
        ));
        mProductsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mProductsRecyclerView.setAdapter(mProductAdapter);
        mProductsRecyclerView.setEmptyView(emptyView);
    }

    private void setUpOrderSummaryRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = getActivity().findViewById(R.id.empty_cart);
        BrandButtonNormal addToCartBtn = emptyView.findViewById(R.id.add_to_cart);
        addToCartBtn.setOnClickListener(view -> showBottomSheet(false));

        mOrderSummaryRecyclerView = recyclerView;
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
        mOrderSummaryRecyclerView.setHasFixedSize(true);
        mOrderSummaryRecyclerView.setLayoutManager(mLayoutManager);
        mOrderSummaryRecyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mOrderSummaryRecyclerView.addItemDecoration(new OrderItemDividerItemDecoration(context));
        mOrderSummaryRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mOrderSummaryRecyclerView.setAdapter(orderSummaryAdapter);
        mOrderSummaryRecyclerView.setEmptyView(emptyView);
    }

    @Override
    public void onCustomerSelected(@NonNull CustomerEntity customerEntity) {
        mSelectedCustomer = customerEntity;
        createSale();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        if (!isDual) {
            inflater.inflate(R.menu.search, menu);
            final MenuItem item = menu.findItem(R.id.action_search);
            final SearchView searchView = (SearchView) item.getActionView();
            searchView.setOnQueryTextListener(this);
//        }
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

    private void setUpBottomSheetView() {
        collapsedToolbar = getActivity().findViewById(R.id.order_summary_collapsed_toolbar);
        proceedToCheckoutBtn = collapsedToolbar.findViewById(R.id.proceed_to_check_out);
        cartCountImageView = proceedToCheckoutBtn.getCartImageView();
        orderSummaryExpandedToolbar = getActivity().findViewById(R.id.order_summary_expanded_toolbar);
        orderSummaryCheckoutWrapper = getActivity().findViewById(R.id.order_summary_checkout_wrapper);
        orderSummaryCheckoutBtn = getActivity().findViewById(R.id.order_summary_checkout_btn);
        orderSummaryBottomSheetBehavior = BottomSheetBehavior.from(getActivity().findViewById(R.id.order_summary_bs_wrapper));
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

        RxView.clicks(orderSummaryCheckoutBtn).subscribe(o -> payOptionsDialog.show(getActivity().getSupportFragmentManager(), PayOptionsDialog.TAG));

        if (orderSummaryBottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
            ((UserLockBottomSheetBehavior) orderSummaryBottomSheetBehavior).setAllowUserDragging(false);
        }

        getActivity().findViewById(R.id.clear_cart).setOnClickListener(view -> new AlertDialog.Builder(context)
                .setTitle("Are you sure?")
                .setMessage("All items will be permanently removed from your cart!")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    mSelectedProducts.clear();
                    totalCharge = 0;
                    orderSummaryAdapter.queryAsync();
                    mProductAdapter.queryAsync();
                    refreshCartCount();
                    setCheckoutValue();
                    viewModel.setSelectedProducts(mSelectedProducts);
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel())
                .setIcon(AppCompatResources.getDrawable(context, android.R.drawable.ic_dialog_alert))
                .show());

        orderSummaryExpandedToolbar.setNavigationOnClickListener(view -> showBottomSheet(false));
    }

    @Override
    public void onCardPaymentDialogComplete(boolean showCustomerDialog) {
        isPaidWithCard = true;
        if (showCustomerDialog) {
            customerAutoCompleteDialog.show(getActivity().getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
        } else {
            createSale();
        }
    }

    @Override
    public void onCashPaymentDialogComplete(boolean showCustomerDialog) {
        isPaidWithCash = true;
        if (showCustomerDialog && customerId == 0) {
            customerAutoCompleteDialog.show(getActivity().getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
        } else if (showCustomerDialog && customerId > 0) {
            createSale();
        } else {
            createSale();
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
        }
        else {
            ((ProductsAdapter) mProductsRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        mProductsRecyclerView.scrollToPosition(0);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (orderSummaryBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            showBottomSheet(false);
        } else {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }


    private class ProductsAdapter extends
            QueryRecyclerAdapter<ProductEntity,
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
                Selection<ReactiveResult<ProductEntity>> productsSelection =
                        mDataStore.select(ProductEntity.class);
                productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
                productsSelection.where(ProductEntity.DELETED.notEqual(true));
                return productsSelection.orderBy(ProductEntity.NAME.asc()).get();
            }
            else {
                String query = "%" + searchFilterText.toLowerCase() + "%";
                ProductCategoryEntity categoryEntity = mDataStore.select(ProductCategoryEntity.class)
                        .where(ProductCategoryEntity.NAME.like(query))
                        .get()
                        .firstOrNull();

                Selection<ReactiveResult<ProductEntity>> productsSelection =
                        mDataStore.select(ProductEntity.class);
                productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
                productsSelection.where(ProductEntity.NAME.like(query))
                        .or(ProductEntity.CATEGORY.equal(categoryEntity));
                productsSelection.where(ProductEntity.DELETED.notEqual(true));

                return productsSelection.orderBy(ProductEntity.NAME.asc()).get();
            }
        }

        @SuppressLint("CheckResult")
        @Override
        public void onBindViewHolder(ProductEntity item,
                                     BindingHolder<PosProductItemBinding> holder,
                                     int position) {
            holder.binding.setProduct(item);
            RequestOptions options = new RequestOptions();
            options.centerCrop().apply(RequestOptions.placeholderOf(
                    AppCompatResources.getDrawable(context, R.drawable.ic_photo_black_24px)
            ));
            if (item.getPicture() != null) {
                Glide.with(context)
                        .load(item.getPicture())
                        .apply(options)
                        .into(holder.binding.productImage);
                Glide.with(context)
                        .load(item.getPicture())
                        .apply(options)
                        .into(holder.binding.productImageCopy);
            } else {
                TextDrawable drawable = TextDrawable.builder()
                        .beginConfig().textColor(Color.GRAY)
                        .useFont(Typeface.DEFAULT)
                        .toUpperCase().endConfig()
                        .buildRect(item.getName().substring(0,2), Color.WHITE);
                holder.binding.productImage.setImageDrawable(drawable);
                holder.binding.productImageCopy.setImageDrawable(drawable);
            }
            holder.binding.productName.setText(item.getName());
            if (mSelectedProducts.get(item.getId()) == 0
                    || viewModel.getSelectedProducts().getValue().get(item.getId()) == 0) {
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
                    AppCompatResources.getDrawable(context,
                            R.drawable.ic_remove_circle_outline_white_24px));
        }


        @NonNull
        @Override
        public BindingHolder<PosProductItemBinding> onCreateViewHolder(
                @NonNull ViewGroup parent, int i) {
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
                                if (!isDual) {
                                    orderSummaryAdapter.queryAsync();
                                    setCheckoutValue();
                                }
                                viewModel.setSelectedProducts(mSelectedProducts);
                            } else {
                                mSelectedProducts.put(product.getId(),
                                        (mSelectedProducts.get(product.getId()) + 1));
                                mProductAdapter.queryAsync();
                                if (!isDual) {
                                    orderSummaryAdapter.queryAsync();
                                    setCheckoutValue();
                                }
                                viewModel.setSelectedProducts(mSelectedProducts);
                            }

                            binding.productDecrementWrapper.setVisibility(View.VISIBLE);
                            if (!isDual) {
                                makeFlyAnimation(posProductItemBinding.productImageCopy, cartCountImageView);
                            }
                        }
                    }
            );

            binding.decrementCount.setOnClickListener(view -> {
                PosProductItemBinding posProductItemBinding = (PosProductItemBinding) view.getTag();
                if (posProductItemBinding != null) {
                    Product product = posProductItemBinding.getProduct();
                    if (mSelectedProducts.get(product.getId()) != 0) {
                        int newValue = mSelectedProducts.get(product.getId()) - 1;
                        if (!isDual) {
                            setProductCountValue(newValue, product.getId());
                        } else {
                            setProductValue(newValue, product.getId());
                        }
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

    private class OrderSummaryAdapter extends QueryRecyclerAdapter<ProductEntity,
            BindingHolder<OrderSummaryItemBinding>> {

        OrderSummaryAdapter() {
            super(ProductEntity.$TYPE);
        }

        @Override
        public Result<ProductEntity> performQuery() {
            ArrayList<Integer> ids = new ArrayList<>();
            for (int i = 0; i < mSelectedProducts.size(); i++) {
                ids.add(mSelectedProducts.keyAt(i));
            }
            return mDataStore.select(ProductEntity.class)
                    .where(ProductEntity.ID.in(ids))
                    .orderBy(ProductEntity.UPDATED_AT.desc()).get();
        }

        @SuppressLint("CheckResult")
        @Override
        public void onBindViewHolder(ProductEntity item,
                                     BindingHolder<OrderSummaryItemBinding> holder, int position) {
            holder.binding.setProduct(item);
            RequestOptions options = new RequestOptions();
            options.fitCenter().centerCrop().apply(RequestOptions.placeholderOf(
                    AppCompatResources.getDrawable(context, R.drawable.ic_photo_black_24px)
            ));
//            Glide.with(context)
//                    .load(item.getPicture())
//                    .apply(options)
//                    .into(holder.binding.productImage);

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
            holder.binding.deleteCartItem.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.ic_close_white_24px));
            holder.binding.orderItemIncDecBtn.setNumber(
                    String.valueOf(mSelectedProducts.get(item.getId())));

            String template = "%.2f";
            double totalCostOfItem = item.getPrice() * mSelectedProducts.get(item.getId());
            String cText = String.format(Locale.UK, template, totalCostOfItem);
            holder.binding.productCost.setText(getString(R.string.product_price, merchantCurrencySymbol, cText));
        }

        @Override
        public BindingHolder<OrderSummaryItemBinding> onCreateViewHolder(
                ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final OrderSummaryItemBinding binding = OrderSummaryItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            binding.deleteCartItem.setTag(binding);

            binding.deleteCartItem.setOnClickListener(view -> new AlertDialog.Builder(context)
                    .setTitle("Are you sure?")
                    .setMessage("This item will be permanently removed from your cart!")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        OrderSummaryItemBinding orderSummaryItemBinding =
                                (OrderSummaryItemBinding) view.getTag();
                        if (orderSummaryItemBinding != null
                                && orderSummaryItemBinding.getProduct() != null) {
                            mSelectedProducts.delete(orderSummaryItemBinding.getProduct().getId());
                            orderSummaryAdapter.queryAsync();
                            mProductAdapter.queryAsync();
                            refreshCartCount();
                            setCheckoutValue();
                        }
                    })
                    .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel())
                    .setIcon(AppCompatResources.getDrawable(context, android.R.drawable.ic_dialog_alert))
                    .show());

            binding.orderItemIncDecBtn.setOnValueChangeListener((view, oldValue, newValue) -> setProductCountValue(newValue, binding.getProduct().getId()));
            return new BindingHolder<>(binding);
        }
    }

    private void createSale() {
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
                        SyncAdapter.performSync(context, mSessionManager.getEmail());

                        Completable.complete()
                                .delay(1, TimeUnit.SECONDS)
                                .doOnComplete(() -> {
                                    Bundle bundle = new Bundle();
                                    if (mSelectedCustomer != null) {
                                        bundle.putInt(Constants.CUSTOMER_ID, mSelectedCustomer.getId());
                                    }

                                    @SuppressLint("UseSparseArrays") HashMap<Integer, Integer>
                                            orderSummaryItems = new HashMap<>(mSelectedProducts.size());
                                    for (int x = 0; x < mSelectedProducts.size(); x++) {
                                        orderSummaryItems.put(mSelectedProducts.keyAt(x), mSelectedProducts.valueAt(x));
                                    }
                                    bundle.putSerializable(Constants.ORDER_SUMMARY_ITEMS, orderSummaryItems);

                                    Intent intent = new Intent(context, SaleWithPosConfirmationActivity.class);
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
    public void onResume() {
        mProductAdapter.queryAsync();
        orderSummaryAdapter.queryAsync();

        if (myAlertDialog == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                myAlertDialog = MyAlertDialog.newInstance(android.R.style.Theme_Material_Dialog_Alert);
            } else {
                myAlertDialog = new MyAlertDialog();
            }
        }
        myAlertDialog.setShowNegativeButton(false);

        List<ProductEntity> productEntities = mProductAdapter.performQuery().toList();
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
                    Intent intent = new Intent(context, ProductListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                });
            } else if (productsWithoutLoyaltyProgram.size() == 1){
                myAlertDialog.setMessage("1 of your products or services don't have a loyalty program set. Click the button below, select a loyalty program and save.");
                myAlertDialog.setPositiveButton(getString(R.string.update_product), (dialogInterface, i) -> {
                    Intent intent = new Intent(context, ProductDetailActivity.class);
                    intent.putExtra(ProductDetailActivity.ARG_ITEM_ID, productsWithoutLoyaltyProgram.get(0).getId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                });
            } else {
                myAlertDialog.setMessage(productsWithoutLoyaltyProgram.size() + " of your products or services " + "don't have loyalty programs set. For each product, go to the products edit screen. Select a loyalty program and save.");
                myAlertDialog.setPositiveButton(getString(R.string.pref_my_products_title), (dialogInterface, i) -> {
                    Intent intent = new Intent(context, ProductListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                });
            }
            myAlertDialog.setCancelable(false);
            if (Foreground.get().isForeground()) {
                myAlertDialog.show(getActivity().getSupportFragmentManager(), MyAlertDialog.TAG);
            }
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        mProductAdapter.close();
        orderSummaryAdapter.close();
        super.onDestroy();
    }

    private void refreshCartCount() {
        proceedToCheckoutBtn.setCartCount(String.valueOf(mSelectedProducts.size()));
    }

    private void setProductCountValue(int newValue, int productId) {
        if (newValue > 0) {
            mSelectedProducts.put(productId, newValue);
            mProductAdapter.queryAsync();
            orderSummaryAdapter.queryAsync();
            refreshCartCount();
            setCheckoutValue();
        } else {
            mSelectedProducts.delete(productId);
            mProductAdapter.queryAsync();
            orderSummaryAdapter.queryAsync();
            refreshCartCount();
            setCheckoutValue();
        }
    }

    private void setProductValue(int newValue, int productId) {
        if (newValue > 0) {
            mSelectedProducts.put(productId, newValue);
            mProductAdapter.queryAsync();
            viewModel.setSelectedProducts(mSelectedProducts);
        } else {
            mSelectedProducts.delete(productId);
            mProductAdapter.queryAsync();
            viewModel.setSelectedProducts(mSelectedProducts);
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

        new CircleAnimationUtil().attachActivity(getActivity())
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
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(customerEntity -> {
                                if (customerEntity == null) {
                                    Toast.makeText(context, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
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
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Log.e("fireeeee", "LOOOOO");
        if (savedInstanceState != null) {
            Parcelable productsListState = savedInstanceState.getParcelable(KEY_PRODUCTS_RECYCLER_STATE);
            Parcelable orderSummaryListState = savedInstanceState.getParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE);

            Log.e("KKKKK>>>", productsListState + "  " + orderSummaryListState);

            /*restore RecyclerView state*/
            if (productsListState != null) {
                mProductsRecyclerView.getLayoutManager().onRestoreInstanceState(productsListState);
            }
            if (orderSummaryListState != null) {
                mOrderSummaryRecyclerView.getLayoutManager().onRestoreInstanceState(orderSummaryListState);
            }

            @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems =
                    (HashMap<Integer, Integer>) savedInstanceState.getSerializable(KEY_SELECTED_PRODUCTS_STATE);
            Log.e("dddd", orderSummaryItems + "");
            if (orderSummaryItems != null) {
                for (Map.Entry<Integer, Integer> orderItem: orderSummaryItems.entrySet()) {
                    mSelectedProducts.put(orderItem.getKey(), orderItem.getValue());
                    setProductCountValue(orderItem.getValue(), orderItem.getKey());
                    if (isDual) {
                        setProductValue(orderItem.getValue(), orderItem.getKey());
                    }
                }
            }

            if (savedInstanceState.containsKey(KEY_SAVED_CUSTOMER_ID)) {
                mSelectedCustomer = mDataStore
                        .findByKey(CustomerEntity.class,
                                savedInstanceState.getInt(KEY_SAVED_CUSTOMER_ID)).blockingGet();
                Log.e("next", mSelectedCustomer +"");
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
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            Log.e("is", "not");
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
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.e("here", ">>>>>");
        @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> orderSummaryItems =
                new HashMap<>(mSelectedProducts.size());
        for (int x = 0; x < mSelectedProducts.size(); x++) {
            orderSummaryItems.put(mSelectedProducts.keyAt(x), mSelectedProducts.valueAt(x));
        }
        outState.putSerializable(KEY_SELECTED_PRODUCTS_STATE, orderSummaryItems);

        Parcelable productsListState = mProductsRecyclerView.getLayoutManager().onSaveInstanceState();
        outState.putParcelable(KEY_PRODUCTS_RECYCLER_STATE, productsListState);
//        if (!isDual) {
            Parcelable orderSummaryListState = mOrderSummaryRecyclerView.getLayoutManager().onSaveInstanceState();
            outState.putParcelable(KEY_ORDER_SUMMARY_RECYCLER_STATE, orderSummaryListState);
//        }

        if (mSelectedCustomer != null) {
            outState.putInt(KEY_SAVED_CUSTOMER_ID, mSelectedCustomer.getId());
        }

        super.onSaveInstanceState(outState);
    }


}
