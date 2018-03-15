package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.DateTime;

import java.sql.Timestamp;

import butterknife.BindView;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;

public class AddPointsActivity extends BaseActivity {

    @BindView(R.id.currencyEditText)
    CurrencyEditText mCurrencyEditText;

    @BindView(R.id.addPoints)
    BrandButtonNormal addPointsBtn;

    @BindView(R.id.total_points)
    TextView totalPointsView;

    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;
    private CustomerEntity mCustomer;
    private MerchantEntity merchantEntity;
    private int mProgramId;
    private int totalCustomerPoints = 0;
    private ReactiveEntityStore<Persistable> mDataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_points);

        mDatabaseManager = DatabaseManager.getInstance(this);
        mSessionManager = new SessionManager(this);
        mDataStore = DatabaseManager.getDataStore(this);

        merchantEntity = mDatabaseManager.getMerchant(mSessionManager.getMerchantId());

        mProgramId = getIntent().getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
        int amountSpent = getIntent().getIntExtra(Constants.CASH_SPENT, 0);
        int mCustomerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);

        mCustomer = mDatabaseManager.getCustomerById(mCustomerId);
        totalCustomerPoints = mDatabaseManager.getTotalCustomerPointsForProgram(mProgramId, mCustomerId);

        RxView.clicks(addPointsBtn).subscribe(o -> addPoints());
        totalPointsView.setText(getString(R.string.total_points, String.valueOf(totalCustomerPoints)));
        mCurrencyEditText.setText(String.valueOf(amountSpent));
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (mCustomer != null) {
                String title = "Add Points " + "(" + TextUtilsHelper.capitalize(mCustomer.getFirstName()) + ")";
                actionBar.setTitle(title);
            }
        }
    }

    private void addPoints() {
        if (mCurrencyEditText.getText().toString().isEmpty()) {
            mCurrencyEditText.setError(getString(R.string.error_amount_required));
            mCurrencyEditText.requestFocus();
            return;
        }

        Integer amountSpent = Integer.valueOf(mCurrencyEditText.getFormattedValue(mCurrencyEditText.getRawValue()));
        Integer lastSaleId = mDatabaseManager.getLastSaleRecordId();

        SaleEntity newSaleEntity = new SaleEntity();
        if (lastSaleId == null) {
            newSaleEntity.setId(1);
        } else {
            newSaleEntity.setId(lastSaleId + 1);
        }
        newSaleEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
        newSaleEntity.setMerchant(merchantEntity);
        newSaleEntity.setPayedWithCard(false);
        newSaleEntity.setPayedWithCash(true);
        newSaleEntity.setPayedWithMobile(false);
        newSaleEntity.setTotal(amountSpent);
        newSaleEntity.setSynced(false);
        newSaleEntity.setCustomer(mCustomer);

        mDataStore.upsert(newSaleEntity).subscribe(saleEntity -> {
            SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
            Integer lastTransactionId = mDatabaseManager.getLastTransactionRecordId();

            if (lastTransactionId == null) {
                transactionEntity.setId(1);
            } else {
                transactionEntity.setId(lastTransactionId + 1);
            }
            transactionEntity.setSynced(false);
            transactionEntity.setSendSms(true);
            transactionEntity.setAmount(amountSpent);
            transactionEntity.setMerchantLoyaltyProgramId(mProgramId);
            transactionEntity.setPoints(amountSpent);
            transactionEntity.setStamps(0);
            transactionEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
            transactionEntity.setProgramType(getString(R.string.simple_points));
            if (mCustomer != null) {
                transactionEntity.setUserId(mCustomer.getUserId());
            }

            transactionEntity.setSale(saleEntity);
            transactionEntity.setMerchant(merchantEntity);
            transactionEntity.setCustomer(mCustomer);

            mDataStore.upsert(transactionEntity).subscribe(/*no-op*/);
            SyncAdapter.performSync(this, mSessionManager.getEmail());

            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }

            int newTotalPoints = totalCustomerPoints + amountSpent;
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.CASH_SPENT, amountSpent);
            bundle.putInt(Constants.TOTAL_CUSTOMER_POINTS, newTotalPoints);

            Intent intent = new Intent();
            intent.putExtras(bundle);

            setResult(RESULT_OK, intent);
            finish();
        });
    }
}
