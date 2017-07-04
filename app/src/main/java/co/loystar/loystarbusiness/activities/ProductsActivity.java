package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatDrawableManager;
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
import com.github.clans.fab.FloatingActionButton;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.ProductsGridViewAdapter;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerViewOnItemClickListener;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.REQUEST_PERMISSION_SETTING;

/**
 * Created by ordgen on 7/4/17.
 */

@RuntimePermissions
public class ProductsActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, RecyclerViewOnItemClickListener {
    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private EmptyRecyclerView mRecyclerView;
    private ArrayList<DBProduct> products;
    private Context mContext;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_view);
        mLayout = findViewById(R.id.products_view_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mContext = this;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(mContext);
        products = databaseHelper.listMerchantProducts(sessionManager.getMerchantId());

        boolean productCreatedIntent = getIntent().getBooleanExtra(getString(R.string.product_add_success), false);
        boolean productUpdatedIntent = getIntent().getBooleanExtra(getString(R.string.product_edit_success), false);
        boolean productDeletedIntent = getIntent().getBooleanExtra(getString(R.string.product_delete_success), false);

        if (productCreatedIntent) {
            Snackbar.make(mLayout, getString(R.string.product_add_success), Snackbar.LENGTH_LONG).show();
        }

        if (productUpdatedIntent) {
            Snackbar.make(mLayout, getString(R.string.product_edit_success), Snackbar.LENGTH_LONG).show();
        }

        if (productDeletedIntent) {
            Snackbar.make(mLayout, getString(R.string.product_delete_success), Snackbar.LENGTH_LONG).show();
            ProductsActivityPermissionsDispatcher.syncDataWithCheck(this);
        }



        FloatingActionButton addProduct = (FloatingActionButton) findViewById(R.id.add_products_fab);
        addProduct.setImageDrawable(AppCompatDrawableManager.get().getDrawable(mContext, R.drawable.ic_add_white_24px));
        addProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addProductIntent = new Intent(ProductsActivity.this, AddProductActivity.class);
                addProductIntent.putExtra(AddProductActivity.ACTIVITY_INITIATOR, ProductsActivity.class.getSimpleName());
                addProductIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(addProductIntent);
            }
        });

        View emptyView = findViewById(R.id.activity_products_view_empty_container);
        BrandButtonNormal addBtn = (BrandButtonNormal) emptyView.findViewById(R.id.activity_products_view_add_product);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addProductIntent = new Intent(ProductsActivity.this, AddProductActivity.class);
                addProductIntent.putExtra(AddProductActivity.ACTIVITY_INITIATOR, ProductsActivity.class.getSimpleName());
                addProductIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(addProductIntent);
            }
        });

        mRecyclerView = (EmptyRecyclerView) findViewById(R.id.products_recycler_view);
        RecyclerView.LayoutManager recyclerViewLayoutManager = new GridLayoutManager(mContext, 3);
        ProductsGridViewAdapter gridViewAdapter = new ProductsGridViewAdapter(mContext, products, this);
        mRecyclerView.setLayoutManager(recyclerViewLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(gridViewAdapter);
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.setEmptyView(emptyView);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
    public void syncData() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(ProductsActivity.this);
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
                        //take user to app settings
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
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            ((ProductsGridViewAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
        } else {
            ((ProductsGridViewAdapter) mRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        mRecyclerView.smoothScrollToPosition(0);
        return true;
    }

    @Override
    public void onItemClicked(int position) {
        Intent editProductIntent = new Intent(ProductsActivity.this, EditProductActivity.class);
        editProductIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        editProductIntent.putExtra(AddProductActivity.PRODUCT_ID, products.get(position).getId());
        startActivity(editProductIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ProductsActivityPermissionsDispatcher.onRequestPermissionsResult(ProductsActivity.this, requestCode, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionManager != null && mRecyclerView != null) {
            products = databaseHelper.listMerchantProducts(sessionManager.getMerchantId());

            ((ProductsGridViewAdapter) mRecyclerView.getAdapter()).animateTo(products);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (resultCode == RESULT_OK) {
                ProductsActivityPermissionsDispatcher.syncDataWithCheck(ProductsActivity.this);
            }
        }
    }
}
