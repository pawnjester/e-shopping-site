package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.fragments.CustomerDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.Merchant;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.CustomerDetailActivityEventBus;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;

/**
 * An activity representing a single Customer detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link CustomerListActivity}.
 */
public class CustomerDetailActivity extends RxAppCompatActivity {
    private static final String TAG = CustomerDetailActivity.class.getSimpleName();
    private static final int REQUEST_CHOOSE_PROGRAM = 145;
    private int customerId;
    private ReactiveEntityStore<Persistable> mDataStore;
    private CustomerEntity mItem;
    private Context mContext;
    private MerchantEntity merchantEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);
        Toolbar toolbar = findViewById(R.id.customer_detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        SessionManager sessionManager = new SessionManager(this);
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(this);
        merchantEntity = mDatabaseManager.getMerchant(sessionManager.getMerchantId());
        customerId = getIntent().getIntExtra(CustomerDetailFragment.ARG_ITEM_ID, 0);
        mItem = mDatabaseManager.getCustomerById(customerId);

        FloatingActionButton fab = findViewById(R.id.customer_detail_fab);
        if (fab != null) {
            fab.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_create_white_48px));
            fab.setOnClickListener(view -> {
                if (mItem != null) {
                    Intent intent = new Intent(CustomerDetailActivity.this, EditCustomerDetailsActivity.class);
                    intent.putExtra(Constants.CUSTOMER_ID, customerId);
                    startActivity(intent);
                }
            });
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(CustomerDetailFragment.ARG_ITEM_ID, customerId);
            CustomerDetailFragment fragment = new CustomerDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.customer_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, CustomerListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        CustomerDetailActivityEventBus
                .getInstance()
                .getFragmentEventObservable().subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer integer) {
                if (integer == CustomerDetailActivityEventBus.ACTION_START_SALE) {
                    startSale();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void startSale() {
        mDataStore.count(LoyaltyProgramEntity.class)
                .get()
                .single()
                .toObservable()
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        if (integer == 0) {
                            new AlertDialog.Builder(mContext)
                                    .setTitle("No Loyalty Program Found!")
                                    .setMessage("To record a sale, you would have to start a loyalty program.")
                                    .setPositiveButton(mContext.getString(R.string.start_loyalty_program_btn_label), (dialog, which) -> {
                                        dialog.dismiss();
                                        startLoyaltyProgram();
                                    })
                                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss()).show();
                        } else if (integer == 1) {
                            LoyaltyProgramEntity loyaltyProgramEntity = merchantEntity.getLoyaltyPrograms().get(0);
                            initiateSalesProcess(loyaltyProgramEntity);
                        } else {
                            chooseProgram();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void initiateSalesProcess(@NonNull LoyaltyProgramEntity loyaltyProgramEntity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
        if (isPosTurnedOn) {
            if (loyaltyProgramEntity.getProgramType().equals(getString(R.string.simple_points))) {
                startPointsSaleWithPos(loyaltyProgramEntity.getId());
            } else if (loyaltyProgramEntity.getProgramType().equals(getString(R.string.stamps_program))) {
                startStampsSaleWithPos(loyaltyProgramEntity.getId());
            }
        } else {
            startSaleWithoutPos(loyaltyProgramEntity.getId());
        }
    }

    private void chooseProgram() {
        Intent intent = new Intent(this, ChooseProgramActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_PROGRAM);
    }

    private void startPointsSaleWithPos(int programId) {
        Intent intent = new Intent(this, PointsSaleWithPosActivity.class);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startStampsSaleWithPos(int programId) {
        Intent intent = new Intent(this, StampsSaleWithPosActivity.class);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startSaleWithoutPos(int programId) {
        Intent intent = new Intent(this, SaleWithoutPosActivity.class);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startLoyaltyProgram() {
        Intent intent = new Intent(mContext, LoyaltyProgramListActivity.class);
        intent.putExtra(Constants.CREATE_LOYALTY_PROGRAM, true);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CHOOSE_PROGRAM) {
                int programId = data.getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
                LoyaltyProgramEntity loyaltyProgramEntity = mDataStore.findByKey(LoyaltyProgramEntity.class, programId).blockingGet();
                initiateSalesProcess(loyaltyProgramEntity);
            }
        }
    }
}
