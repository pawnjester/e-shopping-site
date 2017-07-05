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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBProductCategory;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.Chips.ChipsAdapter;
import co.loystar.loystarbusiness.utils.ui.Chips.ChipsEntity;
import co.loystar.loystarbusiness.utils.ui.Chips.ChipsFactory;
import co.loystar.loystarbusiness.utils.ui.Chips.ChipsOnRemoveListener;
import co.loystar.loystarbusiness.utils.ui.Chips.IItemsFactory;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * Created by ordgen on 7/4/17.
 */

@RuntimePermissions
public class ProductCategoriesActivity extends AppCompatActivity implements ChipsOnRemoveListener, SearchView.OnQueryTextListener {

    /*constants*/
    private static final int REQUEST_PERMISSION_SETTING = 103;

    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private IItemsFactory itemsFactory = new ChipsFactory();
    private ArrayList<ChipsEntity> items;
    private SessionManager sessionManager;
    private Context mContext;
    private RecyclerView.Adapter mAdapter;
    private EmptyRecyclerView mRecyclerView;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_categories);
        mLayout = findViewById(R.id.product_categories_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        sessionManager = new SessionManager(mContext);

        View emptyView = findViewById(R.id.activity_product_categories_empty_container);
        BrandButtonNormal addBtn = (BrandButtonNormal) emptyView.findViewById(R.id.activity_product_categories_add_category);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, AddCategoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        mAdapter = createAdapter();
        ChipsLayoutManager layoutManager = ChipsLayoutManager.newBuilder(mContext).build();

        mRecyclerView = (EmptyRecyclerView) findViewById(R.id.product_categories_recycler_view);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(getResources().getDimensionPixelOffset(R.dimen.item_space),
                getResources().getDimensionPixelOffset(R.dimen.item_space)));
        mRecyclerView.setEmptyView(emptyView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_category_fab);
        fab.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_add_white_24px));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addCategoryIntent = new Intent(ProductCategoriesActivity.this, AddCategoryActivity.class);
                addCategoryIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(addCategoryIntent);
            }
        });

        boolean categoryCreatedIntent = getIntent().getBooleanExtra(getString(R.string.new_product_category_added), false);
        if (categoryCreatedIntent) {
            Snackbar.make(mLayout, getString(R.string.new_product_category_added), Snackbar.LENGTH_LONG).show();
            ProductCategoriesActivityPermissionsDispatcher.syncDataWithCheck(ProductCategoriesActivity.this);

        }
    }

    @SuppressWarnings("unchecked")
    private RecyclerView.Adapter createAdapter() {
        items = itemsFactory.getCategoryItems(mContext);
        return itemsFactory.createAdapter(items, this);
    }

    @Override
    public void onItemRemoved(final int position) {
        final DBProductCategory category = databaseHelper.getProductCategoryByName(items.get(position).getName(), sessionManager.getMerchantId());
        new AlertDialog.Builder(mContext)
                .setTitle("Are you sure?")
                .setMessage("All products associated with this category will be deleted as well.")
                .setPositiveButton(getString(R.string.confirm_delete_positive), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        category.setDeleted(true);
                        databaseHelper.updateProductCategory(category);
                        items.remove(position);
                        mAdapter.notifyItemRemoved(position);
                        mAdapter.notifyItemRangeChanged(position, items.size());
                        mAdapter.notifyDataSetChanged();
                        Snackbar.make(mLayout, getString(R.string.product_category_delete_success), Snackbar.LENGTH_LONG).show();

                        ProductCategoriesActivityPermissionsDispatcher.syncDataWithCheck(ProductCategoriesActivity.this);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            ((ChipsAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
        } else {
            ((ChipsAdapter) mRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        mRecyclerView.smoothScrollToPosition(0);
        return true;
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
    public void syncData() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(ProductCategoriesActivity.this);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        ProductCategoriesActivityPermissionsDispatcher.onRequestPermissionsResult(ProductCategoriesActivity.this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (resultCode == RESULT_OK) {
                ProductCategoriesActivityPermissionsDispatcher.syncDataWithCheck(ProductCategoriesActivity.this);
            }
        }
    }
}
