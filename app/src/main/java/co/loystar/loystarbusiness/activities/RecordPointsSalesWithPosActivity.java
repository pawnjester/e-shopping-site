package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration;

import java.util.ArrayList;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LinearLayoutManagerWrapper;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TimeUtils;
import co.loystar.loystarbusiness.utils.ui.Buttons.AddCustomerButton;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.Buttons.FullRectangleButton;
import co.loystar.loystarbusiness.utils.ui.Buttons.FullRectangleImageButton;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog.CustomerAutoCompleteDialog;
import co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.OrderSummary.OrderSummaryItem;
import co.loystar.loystarbusiness.utils.ui.OrderSummary.OrderSummaryItemListAdapter;
import co.loystar.loystarbusiness.utils.ui.PosProducts.IPosProductsEntityFactory;
import co.loystar.loystarbusiness.utils.ui.PosProducts.PosProductEntity;
import co.loystar.loystarbusiness.utils.ui.PosProducts.PosProductsCountListener;
import co.loystar.loystarbusiness.utils.ui.PosProducts.PosProductsEntityFactory;
import co.loystar.loystarbusiness.utils.ui.PosProducts.PosProductsGridViewAdapter;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogOnItemSelectedListener;
import co.loystar.loystarbusiness.utils.ui.UserLockBottomSheetBehavior;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static co.loystar.loystarbusiness.activities.MerchantBackOffice.CUSTOMER_NAME;
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.CUSTOMER_PROGRAM_WORTH;
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE;
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.REQUEST_PERMISSION_SETTING;

/**
 * Created by ordgen on 7/4/17.
 */

/**Intent params
 * Long mSelectedProgramId = selected programId
 * Long mCustomerId = preselected customer id (optional)
 * */

@RuntimePermissions
public class RecordPointsSalesWithPosActivity extends AppCompatActivity implements SearchView.OnQueryTextListener,
        SingleChoiceSpinnerDialogOnItemSelectedListener,
        PosProductsCountListener,
        CustomerAutoCompleteDialog.SelectedCustomerListener,
        OrderSummaryItemListAdapter.OrderSummaryItemUpdateListener {
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private Long mSelectedProgramId = null;
    private double totalCharge = 0.0;
    private String merchantCurrency;
    private boolean draggingStateUp = false;
    private ArrayList<OrderSummaryItem> orderSummaryItems = new ArrayList<>();
    private DBCustomer mCustomerSelected;
    private String chargeTemplate = "COLLECT PAYMENT %s %.2f";
    private SessionManager sessionManager;
    private Context mContext;
    private Long mCustomerId;

    /*Views*/
    private BottomSheetBehavior bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;
    private CustomerAutoCompleteDialog customerAutoCompleteDialog;
    private AddCustomerButton addCustomer;
    private TextView customerName;
    private TextView customerNumber;
    private View customerExistsBlock;
    private EmptyRecyclerView productsRecyclerView;
    private Toolbar expandedToolbar;
    private EmptyRecyclerView orderSummaryRecyclerView;
    private LinearLayout collapsedToolbar;
    private FullRectangleButton checkOutButton;
    private FullRectangleButton collectPayment;
    private View mLayout;
    private IPosProductsEntityFactory itemsFactory = new PosProductsEntityFactory();
    private ArrayList<PosProductEntity> productEntities;
    private RecyclerView.Adapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_points_sales_with_pos);
        mLayout = findViewById(R.id.activity_record_points_sales_with_pos_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.choose_products_or_services));
        }
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mAdapter = createAdapter();

        Bundle extras = getIntent().getExtras();
        mSelectedProgramId = extras.getLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, 0L);
        mCustomerId = extras.getLong(RecordDirectSalesActivity.CUSTOMER_ID, 0L);
        mCustomerSelected = databaseHelper.getCustomerById(mCustomerId);

        bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        draggingStateUp = true;
                        if (bottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
                            ((UserLockBottomSheetBehavior) bottomSheetBehavior).setAllowUserDragging(true);
                        }
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        draggingStateUp = false;
                        if (mCustomerSelected == null) {
                            if (bottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
                                ((UserLockBottomSheetBehavior) bottomSheetBehavior).setAllowUserDragging(false);
                            }
                        }
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        if (mCustomerSelected == null) {
                            if (bottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
                                ((UserLockBottomSheetBehavior) bottomSheetBehavior).setAllowUserDragging(false);
                            }
                        }
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull final View bottomSheet, float slideOffset) {

                Runnable draggingStateUpAction = new Runnable() {
                    public void run() {
                        //after fully expanded the ony way is down
                        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                            collapsedToolbar.setVisibility(View.GONE);
                            collapsedToolbar.setAlpha(0f);
                            expandedToolbar.setAlpha(1f);
                        }
                    }
                };

                Runnable draggingStateDownAction = new Runnable() {
                    public void run() {
                        //after collapsed the only way is up
                        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                            expandedToolbar.setVisibility(View.GONE);
                            collapsedToolbar.setAlpha(1f);
                            expandedToolbar.setAlpha(0f);
                        }
                    }
                };

                if (draggingStateUp) {
                    //when fully expanded and dragging down
                    expandedToolbar.setVisibility(View.VISIBLE);
                    expandedToolbar.animate().alpha(slideOffset).setDuration((long) slideOffset); //expanded toolbar will fade out

                    float offset = 1f - slideOffset;
                    collapsedToolbar.setVisibility(View.VISIBLE);
                    collapsedToolbar.animate().alpha(offset).setDuration((long) offset).withEndAction(draggingStateDownAction); //collapsed toolbar will to fade in

                }
                else {
                    //when collapsed and you are dragging up
                    float offset = 1f - slideOffset; //collapsed toolbar will fade out
                    collapsedToolbar.setVisibility(View.VISIBLE);
                    collapsedToolbar.animate().alpha(offset).setDuration((long) offset);


                    expandedToolbar.setVisibility(View.VISIBLE);
                    expandedToolbar.animate().alpha(slideOffset).setDuration((long) slideOffset).withEndAction(draggingStateUpAction); //expanded toolbar will fade in

                }
            }
        };

        sessionManager = new SessionManager(this);
        merchantCurrency = CurrenciesFetcher.getCurrencies(this).getCurrency(sessionManager.getMerchantCurrency()).getSymbol();

        View emptyProductsView = findViewById(R.id.activity_record_sales_with_pos_products_empty_container);
        BrandButtonNormal addProducts = (BrandButtonNormal) findViewById(R.id.activity_record_sales_with_pos_add_product);
        BrandButtonNormal recordSaleWithoutPos = (BrandButtonNormal) findViewById(R.id.record_sale_without_pos);

        addProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addProductIntent = new Intent(mContext, AddProductActivity.class);
                addProductIntent.putExtra(AddProductActivity.ACTIVITY_INITIATOR, RecordPointsSalesWithPosActivity.class.getSimpleName());
                addProductIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(addProductIntent);
            }
        });

        recordSaleWithoutPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Bundle data = new Bundle();;
                data.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mSelectedProgramId);
                data.putString(RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE, getString(R.string.simple_points));

                Intent intent = new Intent(mContext, RecordDirectSalesActivity.class);
                intent.putExtras(data);
                startActivity(intent);
            }
        });

        productsRecyclerView = (EmptyRecyclerView) findViewById(R.id.select_products_recycler_view);
        final RecyclerView.LayoutManager recyclerViewLayoutManager = new GridLayoutManager(this, 3);

        productsRecyclerView.setLayoutManager(recyclerViewLayoutManager);
        productsRecyclerView.setHasFixedSize(true);
        productsRecyclerView.setAdapter(mAdapter);
        productsRecyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_extra),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_extra))
        );
        productsRecyclerView.setEmptyView(emptyProductsView);

        setUpOrderSummaryBottomSheet();
    }

    @SuppressWarnings("unchecked")
    private RecyclerView.Adapter createAdapter() {
        productEntities = itemsFactory.getProductItems(mContext);
        return itemsFactory.createAdapter(mContext, productEntities, this);
    }


    private void setUpOrderSummaryBottomSheet() {
        /*Initialize Views*/
        addCustomer = (AddCustomerButton) findViewById(R.id.add_customer);
        customerName = (TextView) findViewById(R.id.name);
        customerNumber = (TextView) findViewById(R.id.number);
        View removeCustomer = findViewById(R.id.remove_customer);
        customerExistsBlock = findViewById(R.id.customerExistsBlock);
        LinearLayout collectPaymentBar = (LinearLayout) findViewById(R.id.collect_payment_bar);
        collectPayment = (FullRectangleButton) findViewById(R.id.collect_payment);
        collapsedToolbar = (LinearLayout) findViewById(R.id.order_summary_collapsed_toolbar);
        FullRectangleImageButton clearCartButton = (FullRectangleImageButton) findViewById(R.id.clear_btn);
        clearCartButton.setImageDrawable(AppCompatDrawableManager.get().getDrawable(mContext, R.drawable.ic_remove_shopping_cart_white_24px));
        checkOutButton = (FullRectangleButton) findViewById(R.id.checkout_btn);
        expandedToolbar = (Toolbar) findViewById(R.id.order_summary_expanded_toolbar);
        orderSummaryRecyclerView = (EmptyRecyclerView) findViewById(R.id.order_items_list);
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.mBottomSheet));
        Button addToCartBtn = (Button) findViewById(R.id.add_to_cart);
        LinearLayout emptyCartView = (LinearLayout) findViewById(R.id.empty_cart);

        expandedToolbar.setVisibility(View.GONE);
        collectPayment.setText(String.format(Locale.UK, chargeTemplate, merchantCurrency, totalCharge));

        ArrayList<DBCustomer> customers = databaseHelper.listMerchantCustomers(sessionManager.getMerchantId());
        customerAutoCompleteDialog = CustomerAutoCompleteDialog.newInstance(getString(R.string.order_owner), customers);
        customerAutoCompleteDialog.SetSelectedCustomerListener(this);

        LinearLayoutManagerWrapper linearLayoutManager = new LinearLayoutManagerWrapper(this, LinearLayoutManager.VERTICAL, false);
        OrderSummaryItemListAdapter orderSummaryItemListAdapter = new OrderSummaryItemListAdapter(this, orderSummaryItems, this);

        orderSummaryRecyclerView.setLayoutManager(linearLayoutManager);
        orderSummaryRecyclerView.setHasFixedSize(true);
        orderSummaryRecyclerView.setAdapter(orderSummaryItemListAdapter);
        orderSummaryRecyclerView.addItemDecoration(new SpacingItemDecoration(getResources().getDimensionPixelOffset(R.dimen.item_space),
                getResources().getDimensionPixelOffset(R.dimen.item_space)));
        orderSummaryRecyclerView.setEmptyView(emptyCartView);


        /*set background drawable to collapsedToolbar and collectPaymentBar*/
        Drawable collapsedToolbarDrawable =  AppCompatDrawableManager.get().getDrawable(mContext, R.drawable.default_button_background);;
        Drawable collectPaymentBarDrawable = AppCompatDrawableManager.get().getDrawable(mContext, R.drawable.rectangle);
        assert collapsedToolbarDrawable != null;
        collapsedToolbarDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(RecordPointsSalesWithPosActivity.this, android.R.color.transparent), PorterDuff.Mode.SRC));
        assert collectPaymentBarDrawable != null;
        collectPaymentBarDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(RecordPointsSalesWithPosActivity.this, android.R.color.transparent), PorterDuff.Mode.SRC));

        collapsedToolbar.setBackground(collapsedToolbarDrawable);
        collectPaymentBar.setBackground(collectPaymentBarDrawable);


        /*Setup Listeners*/
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback);
        if (bottomSheetBehavior instanceof UserLockBottomSheetBehavior) {
            ((UserLockBottomSheetBehavior) bottomSheetBehavior).setAllowUserDragging(false);
        }

        addToCartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        checkOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedProgramId == null) {
                    Toast.makeText(RecordPointsSalesWithPosActivity.this, getString(R.string.error_loyalty_program_required), Toast.LENGTH_LONG).show();
                    return;
                }

                if (mCustomerSelected == null) {
                    customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
                    return;
                }

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        clearCartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(RecordPointsSalesWithPosActivity.this)
                        .setTitle("Clear Shopping Cart?")
                        .setPositiveButton(getString(R.string.clear), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                                mAdapter = createAdapter();
                                ((PosProductsGridViewAdapter) productsRecyclerView.getAdapter()).animateTo(productEntities);
                                ((OrderSummaryItemListAdapter) orderSummaryRecyclerView.getAdapter()).clear();

                                totalCharge = 0.0;
                                bottomSheetBehavior.setPeekHeight(0);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        collectPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mCustomerSelected == null) {
                    customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
                    return;
                }

                DBMerchantLoyaltyProgram loyaltyProgram = databaseHelper.getProgramById(mSelectedProgramId, sessionManager.getMerchantId());
                if (loyaltyProgram != null) {
                    int orderSummaryItemsSize = orderSummaryItems.size();
                    for (int i=0; i<orderSummaryItemsSize; i++) {
                        OrderSummaryItem orderSummaryItem = orderSummaryItems.get(i);
                        int count = orderSummaryItem.getItemCount();
                        int amt = (int) Math.round(count * orderSummaryItem.getItemPrice());
                        DBTransaction dbTransaction = new DBTransaction();
                        dbTransaction.setAmount(amt);
                        dbTransaction.setPoints(amt);
                        dbTransaction.setUser_id(mCustomerSelected.getUser_id());
                        dbTransaction.setLocal_db_created_at(TimeUtils.getCurrentDateAndTime());
                        dbTransaction.setMerchant_id(sessionManager.getMerchantId());
                        dbTransaction.setMerchant_loyalty_program_id(mSelectedProgramId);
                        dbTransaction.setProgram_type(getString(R.string.simple_points));
                        dbTransaction.setProduct_id(orderSummaryItem.getItemId());
                        dbTransaction.setSynced(false);

                        if (i + 1 == orderSummaryItemsSize) {
                            databaseHelper.insertTransaction(dbTransaction);

                            RecordPointsSalesWithPosActivityPermissionsDispatcher.syncDataWithCheck(RecordPointsSalesWithPosActivity.this);

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Bundle arguments = new Bundle();
                                    int totalPoints = databaseHelper.getTotalUserPointsForProgram(mCustomerSelected.getUser_id(), mSelectedProgramId, sessionManager.getMerchantId());

                                    arguments.putString(LOYALTY_PROGRAM_TYPE, getString(R.string.simple_points));
                                    arguments.putString(CUSTOMER_PROGRAM_WORTH, String.valueOf(totalPoints));
                                    arguments.putString(CUSTOMER_NAME, mCustomerSelected.getFirst_name());
                                    arguments.putBoolean(TransactionsConfirmation.SHOW_CONTINUE_BUTTON, true);
                                    arguments.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mSelectedProgramId);
                                    arguments.putLong(RecordDirectSalesActivity.CUSTOMER_ID, mCustomerId);

                                    Intent intent = new Intent(mContext, TransactionsConfirmation.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtras(arguments);
                                    startActivity(intent);
                                }
                            }, 200);
                        }
                        else {
                            /*don't send sms for this transaction*/
                            dbTransaction.setSend_sms(false);
                            databaseHelper.insertTransaction(dbTransaction);

                            RecordPointsSalesWithPosActivityPermissionsDispatcher.syncDataWithCheck(RecordPointsSalesWithPosActivity.this);
                        }
                    }
                }
            }
        });

        expandedToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        if (mCustomerSelected != null) {
            customerName.setText(mCustomerSelected.getFirst_name());
            customerNumber.setText(mCustomerSelected.getPhone_number());
        }

        removeCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCustomerSelected = null;
                mCustomerId = null;
                customerExistsBlock.setVisibility(View.GONE);
                addCustomer.setVisibility(View.VISIBLE);
            }
        });

        addCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);

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
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            ((PosProductsGridViewAdapter) productsRecyclerView.getAdapter()).getFilter().filter(null);
        } else {
            ((PosProductsGridViewAdapter) productsRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        productsRecyclerView.smoothScrollToPosition(0);
        return true;
    }

    @Override
    public void itemSelectedId(Long Id) {
        mSelectedProgramId = Id;
    }

    @Override
    public void onCustomerSelected(DBCustomer customer) {
        mCustomerSelected = customer;
        if (mCustomerSelected != null) {
            mCustomerId = mCustomerSelected.getId();
            customerExistsBlock.setVisibility(View.VISIBLE);
            addCustomer.setVisibility(View.GONE);
            customerName.setText(mCustomerSelected.getFirst_name());
            customerNumber.setText(mCustomerSelected.getPhone_number());
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    /*onValueChange listener for IncrementDecrementButton on orderSummaryRecyclerView*/
    @Override
    public void getOrderSummaryItemUpdate(Long itemId, int newCount) {
        int index = indexOfProductEntity(itemId);
        if (index > -1) {
            PosProductEntity productEntity = productEntities.get(index);
            int previousCount = productEntity.getCount();
            productEntity.setCount(newCount);
            productsRecyclerView.getAdapter().notifyItemChanged(index);

            if (newCount > previousCount) {
                totalCharge += productEntity.getPrice();
            }
            else {
                totalCharge -= productEntity.getPrice();
                if (totalCharge == 0.0) {
                    bottomSheetBehavior.setPeekHeight(0);
                }
            }

            for (int i=0; i<orderSummaryItems.size(); i++) {
                OrderSummaryItem summaryItem =  orderSummaryItems.get(i);
                if (summaryItem.getItemId().equals(productEntity.getId())) {
                    if (newCount == 0) {
                        orderSummaryItems.remove(i);
                        (orderSummaryRecyclerView.getAdapter()).notifyItemRemoved(i);
                    }
                    else {
                        summaryItem.setItemCount(newCount);
                        (orderSummaryRecyclerView.getAdapter()).notifyItemChanged(i);
                    }
                }
            }

            String checkoutTemplate = "CHECKOUT %s %.2f";
            totalCharge = OrderSummaryItemListAdapter.getDoubleValue(totalCharge);
            checkOutButton.setText(String.format(Locale.UK, checkoutTemplate, merchantCurrency, totalCharge));
            collectPayment.setText(String.format(Locale.UK, chargeTemplate, merchantCurrency, totalCharge));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CustomerAutoCompleteDialog.ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                customerAutoCompleteDialog.onActivityResult(requestCode, resultCode, data);
            }
        }
        else if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (resultCode == RESULT_OK) {
                RecordPointsSalesWithPosActivityPermissionsDispatcher.syncDataWithCheck(RecordPointsSalesWithPosActivity.this);
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (productEntities.isEmpty() && productsRecyclerView != null && itemsFactory != null) {
            mAdapter = createAdapter();
            ((PosProductsGridViewAdapter) productsRecyclerView.getAdapter()).animateTo(productEntities);
        }
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
    public void syncData() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(RecordPointsSalesWithPosActivity.this);
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
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        RecordPointsSalesWithPosActivityPermissionsDispatcher.onRequestPermissionsResult(
                RecordPointsSalesWithPosActivity.this, requestCode, grantResults);
    }

    @Override
    public void onCountChanged(Long entityId, int oldCount, int newCount) {
        int index = indexOfProductEntity(entityId);
        if (index > -1) {
            PosProductEntity productEntity = productEntities.get(index);
            productEntity.setCount(newCount);
            productsRecyclerView.getAdapter().notifyItemChanged(index);

            String collapsedToolbarTitle = "";
            String checkoutTemplate = "CHECKOUT %s %.2f";
            if (newCount > oldCount) {
                totalCharge += productEntity.getPrice();
                totalCharge = OrderSummaryItemListAdapter.getDoubleValue(totalCharge);
                collapsedToolbarTitle = String.format(Locale.UK, checkoutTemplate, merchantCurrency, totalCharge);

                int count = 0;
                OrderSummaryItem selectedSummaryItem = null;
                for (int i=0; i<orderSummaryItems.size(); i++) {
                    OrderSummaryItem summaryItem =  orderSummaryItems.get(i);
                    if (summaryItem.getItemId().equals(productEntity.getId())) {
                        selectedSummaryItem = summaryItem;
                        count = summaryItem.getItemCount();
                        count++;
                        summaryItem.setItemCount(count);
                        (orderSummaryRecyclerView.getAdapter()).notifyItemChanged(i);

                    }
                }

                /*first item selection*/
                if (selectedSummaryItem == null) {
                    count++;
                    orderSummaryItems.add(new OrderSummaryItem(productEntity.getName(), productEntity.getPrice(), count, productEntity.getId()));
                    (orderSummaryRecyclerView.getAdapter()).notifyItemRangeChanged(0, orderSummaryItems.size());
                }

                if (bottomSheetBehavior.getPeekHeight() == 0){
                    bottomSheetBehavior.setPeekHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics()));
                }
            }
            else {
                totalCharge -= productEntity.getPrice();
                totalCharge = OrderSummaryItemListAdapter.getDoubleValue(totalCharge);
                collapsedToolbarTitle = String.format(Locale.UK, checkoutTemplate, merchantCurrency, totalCharge);
                if (totalCharge == 0.0) {
                    bottomSheetBehavior.setPeekHeight(0);
                }

                int count = 0;
                for (int i=0; i<orderSummaryItems.size(); i++) {
                    OrderSummaryItem summaryItem =  orderSummaryItems.get(i);
                    if (summaryItem.getItemId().equals(productEntity.getId())) {
                        count = summaryItem.getItemCount();
                        if (count >= 1) {
                            count--;
                            if (count == 0) {
                                orderSummaryItems.remove(i);
                                (orderSummaryRecyclerView.getAdapter()).notifyItemRemoved(i);
                            }
                            else {
                                summaryItem.setItemCount(count);
                                (orderSummaryRecyclerView.getAdapter()).notifyItemChanged(i);
                            }
                        }
                    }
                }
            }

            checkOutButton.setText(collapsedToolbarTitle);
            collectPayment.setText(String.format(Locale.UK, chargeTemplate, merchantCurrency, totalCharge));
        }
    }

    private int indexOfProductEntity(Long id) {
        for (int i=0; i < productEntities.size(); i++) {
            if (productEntities.get(i).getId().equals(id)) {
                return i;
            }
        }
        return  -1;
    }
}
