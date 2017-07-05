package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.MerchantLoyaltyProgramsAdapter;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.LoyaltyProgramsOverflowItemClickEvent;
import co.loystar.loystarbusiness.fragments.LoyaltyProgramsDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.support.v4.app.NavUtils.navigateUpFromSameTask;

/**
 * Created by ordgen on 7/4/17.
 */

@RuntimePermissions
public class LoyaltyProgramsListActivity extends AppCompatActivity {
    private boolean mTwoPane;
    private ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private Context mContext;
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private View mLayout;
    private EmptyRecyclerView mRecyclerView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty_programs_list_activity);
        mLayout = findViewById(R.id.activity_loyalty_programs_list_activity_container);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        mContext = this;
        loyaltyPrograms = databaseHelper.listMerchantPrograms(sessionManager.getMerchantId());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoyaltyProgramsListActivity.this, CreateLoyaltyProgramListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                startActivity(intent);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mRecyclerView = (EmptyRecyclerView) findViewById(R.id.loyaltyprogram_list);
        assert mRecyclerView != null;
        setupRecyclerView(mRecyclerView);

        if (findViewById(R.id.loyaltyprogram_detail_container) != null) {
            mTwoPane = true;

            if (!loyaltyPrograms.isEmpty()) {
                Bundle arguments = new Bundle();
                arguments.putLong(LoyaltyProgramsDetailFragment.ARG_ITEM_ID, loyaltyPrograms.get(0).getId());
                LoyaltyProgramsDetailFragment fragment = new LoyaltyProgramsDetailFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.loyaltyprogram_detail_container, fragment)
                        .commit();
            }
        }

        boolean programUpdatedIntent = getIntent().getBooleanExtra("programUpdated", false);
        if (programUpdatedIntent) {
            Snackbar.make(mLayout, getString(R.string.program_update_success), Snackbar.LENGTH_LONG).show();
        }

        boolean programCreatedIntent = getIntent().getBooleanExtra("programCreated", false);
        if (programCreatedIntent) {
            Snackbar.make(mLayout, getString(R.string.program_created_success), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        MerchantLoyaltyProgramsAdapter adapter = new MerchantLoyaltyProgramsAdapter(loyaltyPrograms, mContext);

        View emptyView = findViewById(R.id.activity_loyalty_programs_list_activity_empty_container);
        BrandButtonNormal addBtn = (BrandButtonNormal) findViewById(R.id.activity_loyalty_programs_list_activity_add_program);
        if (addBtn != null) {
            addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, CreateLoyaltyProgramListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                    startActivity(intent);
                }
            });
        }

        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setEmptyView(emptyView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Subscribe
    public void OnLoyaltyProgramsAdapterItemClickListener(LoyaltyProgramsOverflowItemClickEvent.OnItemClicked onItemClicked) {

        DBMerchantLoyaltyProgram loyaltyProgram = loyaltyPrograms.get(onItemClicked.getAdapterPosition());
        String action = onItemClicked.getAction();
        if (action.equals("edit")) {
            if (mTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putLong(LoyaltyProgramsDetailFragment.ARG_ITEM_ID, loyaltyProgram.getId());
                LoyaltyProgramsDetailFragment fragment = new LoyaltyProgramsDetailFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.loyaltyprogram_detail_container, fragment)
                        .commit();
            } else {;
                Intent intent = new Intent(mContext, LoyaltyProgramsDetailActivity.class);
                intent.putExtra(LoyaltyProgramsDetailFragment.ARG_ITEM_ID, loyaltyProgram.getId());
                startActivity(intent);
            }
        }
        else if (action.equals("delete")) {
            loyaltyProgram.setDeleted(true);
            databaseHelper.updateProgram(loyaltyProgram);
            loyaltyPrograms.remove(loyaltyProgram);
            mRecyclerView.getAdapter().notifyItemRemoved(onItemClicked.getAdapterPosition());
            mRecyclerView.getAdapter().notifyItemRangeChanged(onItemClicked.getAdapterPosition(), loyaltyPrograms.size());
            Snackbar.make(mLayout, getString(R.string.program_deleted_notice), Snackbar.LENGTH_LONG).show();

            LoyaltyProgramsListActivityPermissionsDispatcher.syncDataWithCheck(LoyaltyProgramsListActivity.this);
        }
    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
    public void syncData() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(LoyaltyProgramsListActivity.this);
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
        Toast.makeText(mContext, R.string.permission_accounts_denied, Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.GET_ACCOUNTS)
    void showNeverAskForGetAccounts() {
        Toast.makeText(mContext, R.string.permission_accounts_neverask, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LoyaltyProgramsListActivityPermissionsDispatcher.onRequestPermissionsResult(LoyaltyProgramsListActivity.this, requestCode, grantResults);
    }

    @Override
    public void onBackPressed() {
        navigateUpTo(new Intent(this, SettingsActivity.class));
    }
}
