package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.dialogs.CustomerAutoCompleteDialogAdapter;

public class SaleWithoutPosActivity extends BaseActivity {

    private static final int ADD_NEW_CUSTOMER_REQUEST = 122;
    private static final int ADD_POINTS_REQUEST = 123;
    private static final int ADD_STAMPS_REQUEST = 124;

    @BindView(R.id.record_direct_sales_amount_spent)
    CurrencyEditText mCurrencyEditText;

    @BindView(R.id.record_direct_sales_customer_autocomplete)
    AutoCompleteTextView mAutoCompleteTextView;

    @BindView(R.id.sale_without_pos_continue_btn)
    BrandButtonNormal submitBtn;

    private int mLoyaltyProgramId;
    private int mCashSpent;
    private Context mContext;
    private LoyaltyProgramEntity mLoyaltyProgram;
    private boolean bluetoothPrintEnabled;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;
    private CustomerEntity mSelectedCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_without_pos);

        mContext  = this;
        mDatabaseManager = DatabaseManager.getInstance(this);
        mSessionManager = new SessionManager(this);

        mLoyaltyProgramId = getIntent().getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
        mLoyaltyProgram = mDatabaseManager.getLoyaltyProgramById(mLoyaltyProgramId);
        int mCustomerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        mSelectedCustomer = mDatabaseManager.getCustomerById(mCustomerId);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        bluetoothPrintEnabled = sharedPreferences.getBoolean(getString(R.string.pref_enable_bluetooth_print_key), false);

        RxView.clicks(submitBtn).subscribe(o -> {
            if (TextUtils.isEmpty(mAutoCompleteTextView.getText().toString())) {
                mAutoCompleteTextView.setError(getString(R.string.error_select_customer));
                mAutoCompleteTextView.requestFocus();
                return;
            }

            mCashSpent = (int) mCurrencyEditText.getRawValue();

            if (mSelectedCustomer == null) {
                Intent addCustomerIntent = new Intent(mContext, AddNewCustomerActivity.class);
                String txt = mAutoCompleteTextView.getText().toString();
                if (TextUtilsHelper.isInteger(txt)) {
                    addCustomerIntent.putExtra(Constants.PHONE_NUMBER, txt);
                }
                else {
                    addCustomerIntent.putExtra(Constants.CUSTOMER_NAME, txt);
                }
                startActivityForResult(addCustomerIntent, ADD_NEW_CUSTOMER_REQUEST);
            } else {
                AddPointsOrStamps();
            }
        });
        setupAutoCompleteTextView();
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void AddPointsOrStamps() {
        if (mLoyaltyProgram.getProgramType().equals(getString(R.string.simple_points))) {
            Intent addPointsIntent = new Intent(mContext, AddPointsActivity.class);
            addPointsIntent.putExtra(Constants.LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
            addPointsIntent.putExtra(Constants.CASH_SPENT, mCashSpent);
            addPointsIntent.putExtra(Constants.CUSTOMER_ID, mSelectedCustomer.getId());
            startActivityForResult(addPointsIntent, ADD_POINTS_REQUEST);
        } else if (mLoyaltyProgram.getProgramType().equals(getString(R.string.stamps_program))) {
            Intent addStampsIntent = new Intent(mContext, AddStampsActivity.class);
            addStampsIntent.putExtra(Constants.LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
            addStampsIntent.putExtra(Constants.CASH_SPENT, mCashSpent);
            addStampsIntent.putExtra(Constants.CUSTOMER_ID, mSelectedCustomer.getId());
            startActivityForResult(addStampsIntent, ADD_STAMPS_REQUEST);
        }
    }

    private void setupAutoCompleteTextView() {
        List<CustomerEntity> mCustomers = mDatabaseManager.getMerchantCustomers(mSessionManager.getMerchantId());
        mAutoCompleteTextView.setThreshold(1);
        ArrayList<CustomerEntity> customerEntities = new ArrayList<>();
        customerEntities.addAll(mCustomers);
        CustomerAutoCompleteDialogAdapter autoCompleteDialogAdapter = new CustomerAutoCompleteDialogAdapter(this, customerEntities);
        mAutoCompleteTextView.setAdapter(autoCompleteDialogAdapter);

        if (mSelectedCustomer != null) {
            mAutoCompleteTextView.setText(mSelectedCustomer.getFirstName());
        }

        RxAutoCompleteTextView.itemClickEvents(mAutoCompleteTextView).subscribe(adapterViewItemClickEvent -> {
            mSelectedCustomer = (CustomerEntity) adapterViewItemClickEvent.view().getItemAtPosition(adapterViewItemClickEvent.position());
            mAutoCompleteTextView.setText(mSelectedCustomer.getFirstName());
        });

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
               int  mCustomerId = data.getIntExtra(Constants.CUSTOMER_ID, 0);
                mSelectedCustomer = mDatabaseManager.getCustomerById(mCustomerId);
                if (mSelectedCustomer != null) {
                    AddPointsOrStamps();
                }
            }
        } else if (requestCode == ADD_POINTS_REQUEST) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = new Bundle();
                bundle.putBoolean(Constants.SHOW_CONTINUE_BUTTON, true);
                bundle.putBoolean(Constants.PRINT_RECEIPT, bluetoothPrintEnabled);
                bundle.putInt(Constants.CASH_SPENT, data.getIntExtra(Constants.CASH_SPENT, 0));
                bundle.putInt(Constants.TOTAL_CUSTOMER_POINTS, data.getIntExtra(Constants.TOTAL_CUSTOMER_POINTS, 0));
                bundle.putInt(Constants.LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
                bundle.putInt(Constants.CUSTOMER_ID, data.getIntExtra(Constants.CUSTOMER_ID, 0));

                Intent intent = new Intent(mContext, SaleWithoutPosConfirmationActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        } else if (requestCode == ADD_STAMPS_REQUEST) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = new Bundle();
                bundle.putBoolean(Constants.SHOW_CONTINUE_BUTTON, true);
                bundle.putBoolean(Constants.PRINT_RECEIPT, bluetoothPrintEnabled);
                bundle.putInt(Constants.TOTAL_CUSTOMER_STAMPS, data.getIntExtra(Constants.TOTAL_CUSTOMER_STAMPS, 0));
                bundle.putInt(Constants.LOYALTY_PROGRAM_ID, mLoyaltyProgramId);
                bundle.putInt(Constants.CUSTOMER_ID, data.getIntExtra(Constants.CUSTOMER_ID, 0));
                bundle.putInt(Constants.CASH_SPENT, mCashSpent);

                Intent intent = new Intent(mContext, SaleWithoutPosConfirmationActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }
    }
}
