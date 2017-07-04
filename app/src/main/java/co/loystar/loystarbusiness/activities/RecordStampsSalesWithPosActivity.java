package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.ProductsGridViewAdapter;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog.CustomerAutoCompleteDialog;
import co.loystar.loystarbusiness.utils.ui.PosProducts.PosProductsGridViewAdapter;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerViewOnItemClickListener;

/**
 * Created by ordgen on 7/4/17.
 */

/**Intent params
 * Long mSelectedProgramId = selected programId
 * Long mCustomerId = preselected Customer id
 * */
public class RecordStampsSalesWithPosActivity extends AppCompatActivity  implements SearchView.OnQueryTextListener,
        CustomerAutoCompleteDialog.SelectedCustomerListener,
        RecyclerViewOnItemClickListener {
    /*constants*/
    public static final String PRODUCT_ID = "productId";

    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private ArrayList<DBProduct> products;
    private Long mSelectedProgramId;
    private Long mCustomerId;
    private SessionManager sessionManager;
    private Context mContext;
    private CustomerAutoCompleteDialog customerAutoCompleteDialog;
    private int selectedItemPosition;

    /*Views*/
    private EmptyRecyclerView productsRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_stamps_sales_with_pos);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.choose_product_or_service));
        }
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        sessionManager = new SessionManager(this);
        Bundle extras = getIntent().getExtras();
        mSelectedProgramId = extras.getLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, 0L);
        mCustomerId = extras.getLong(RecordDirectSalesActivity.CUSTOMER_ID, 0L);

        ArrayList<DBCustomer> customers = databaseHelper.listMerchantCustomers(sessionManager.getMerchantId());
        customerAutoCompleteDialog = CustomerAutoCompleteDialog.newInstance(getString(R.string.order_owner), customers);
        customerAutoCompleteDialog.SetSelectedCustomerListener(this);

        View emptyProductsView = findViewById(R.id.activity_record_stamps_sales_with_pos_empty_container);
        BrandButtonNormal addProducts = (BrandButtonNormal) findViewById(R.id.activity_record_stamps_sales_with_pos_add_product);
        BrandButtonNormal recordSaleWithoutPos = (BrandButtonNormal) findViewById(R.id.activity_record_stamps_sales_with_pos_record_sale_without_pos);

        addProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addProductIntent = new Intent(mContext, AddProductActivity.class);
                addProductIntent.putExtra(AddProductActivity.ACTIVITY_INITIATOR, RecordStampsSalesWithPosActivity.class.getSimpleName());
                addProductIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(addProductIntent);
            }
        });

        recordSaleWithoutPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle data = new Bundle();;
                data.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mSelectedProgramId);
                data.putString(RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE, getString(R.string.stamps_program));

                Intent intent = new Intent(mContext, RecordDirectSalesActivity.class);
                intent.putExtras(data);
                startActivity(intent);
            }
        });

        products = databaseHelper.listMerchantProducts(sessionManager.getMerchantId());
        productsRecyclerView = (EmptyRecyclerView) findViewById(R.id.activity_record_stamps_sales_with_pos_products_recycler_view);
        final RecyclerView.LayoutManager recyclerViewLayoutManager = new GridLayoutManager(this, 3);
        ProductsGridViewAdapter gridViewAdapter = new ProductsGridViewAdapter(this, products, this);

        productsRecyclerView.setLayoutManager(recyclerViewLayoutManager);
        productsRecyclerView.setHasFixedSize(true);
        productsRecyclerView.setAdapter(gridViewAdapter);
        productsRecyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_extra),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_extra))
        );
        productsRecyclerView.setEmptyView(emptyProductsView);

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
    public void onItemClicked(int position) {
        if (mCustomerId == 0) {
            customerAutoCompleteDialog.show(getSupportFragmentManager(), CustomerAutoCompleteDialog.TAG);
            return;
        }
        selectedItemPosition = position;
        DBProduct product = products.get(position);

        if (product != null) {
            Intent addStampsIntent = new Intent(mContext, AddStampsActivity.class);
            addStampsIntent.putExtra(PRODUCT_ID, product.getId());
            addStampsIntent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mSelectedProgramId);
            addStampsIntent.putExtra(RecordDirectSalesActivity.CUSTOMER_ID, mCustomerId);
            addStampsIntent.putExtra(PRODUCT_ID, product.getId());
            startActivity(addStampsIntent);
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

        if (sessionManager != null && productsRecyclerView != null && products.isEmpty()) {
            products = databaseHelper.listMerchantProducts(sessionManager.getMerchantId());
            ((ProductsGridViewAdapter) productsRecyclerView.getAdapter()).animateTo(products);

        }
    }

    @Override
    public void onCustomerSelected(DBCustomer user) {
        mCustomerId = user.getId();
        DBProduct product = products.get(selectedItemPosition);

        if (product != null) {
            Intent addStampsIntent = new Intent(mContext, AddStampsActivity.class);
            addStampsIntent.putExtra(PRODUCT_ID, product.getId());
            addStampsIntent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mSelectedProgramId);
            addStampsIntent.putExtra(RecordDirectSalesActivity.CUSTOMER_ID, mCustomerId);
            addStampsIntent.putExtra(PRODUCT_ID, product.getId());
            startActivity(addStampsIntent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CustomerAutoCompleteDialog.ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                customerAutoCompleteDialog.onActivityResult(requestCode, resultCode, data);
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
