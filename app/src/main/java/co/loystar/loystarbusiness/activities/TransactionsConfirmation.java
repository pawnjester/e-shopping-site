package co.loystar.loystarbusiness.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonTransparent;

public class TransactionsConfirmation extends AppCompatActivity {

    private Context mContext;
    private CustomerEntity mCustomer;
    private LoyaltyProgramEntity mLoyaltyProgram;
    private SessionManager mSessionManager;
    private String receiptText = "";
    private int totalPoints;
    private int totalStamps;

    private View mLayout;
    private TextView programTypeTextView;
    private TextView customerNameView;
    private TextView customerLoyaltyWorthView;
    private BrandButtonNormal continueBtn;
    private BrandButtonTransparent printReceiptBtn;

    /*bluetooth print*/
    BluetoothSocket mBluetoothSocket;
    OutputStream mBtOutputStream;
    private byte FONT_TYPE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions_confirmation);
        Toolbar toolbar = findViewById(R.id.transactions_confirmation_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mSessionManager = new SessionManager(this);
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(this);

        int mCustomerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        int mLoyaltyProgramId = getIntent().getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
        boolean showContinueButton = getIntent().getBooleanExtra(Constants.SHOW_CONTINUE_BUTTON, false);
        boolean isPrintReceipt = getIntent().getBooleanExtra(Constants.PRINT_RECEIPT, false);
        receiptText = getIntent().getStringExtra(Constants.RECEIPT_TEXT);
        totalPoints = getIntent().getIntExtra(Constants.TOTAL_CUSTOMER_POINTS, 0);
        totalStamps = getIntent().getIntExtra(Constants.TOTAL_CUSTOMER_STAMPS, 0);

        mLayout = findViewById(R.id.transactions_confirmation_wrapper);
        programTypeTextView = findViewById(R.id.program_type_text);
        customerNameView = findViewById(R.id.customer_name_value);
        customerLoyaltyWorthView = findViewById(R.id.customer_loyalty_worth);
        continueBtn = findViewById(R.id.activity_transactions_confirmation_continue_btn);
        printReceiptBtn = findViewById(R.id.printReceipt);

        if (showContinueButton) {
            continueBtn.setVisibility(View.VISIBLE);
        }

        if (isPrintReceipt) {
            printReceiptBtn.setVisibility(View.VISIBLE);
        }

        mCustomer = mDatabaseManager.getCustomerById(mCustomerId);
        mLoyaltyProgram = mDatabaseManager.getLoyaltyProgramById(mLoyaltyProgramId);

        if (mLoyaltyProgram != null && mCustomer != null) {
            setupViews(mLoyaltyProgram, mCustomer);
        }
    }

    private void setupViews(@NonNull LoyaltyProgramEntity loyaltyProgramEntity, @NonNull CustomerEntity customerEntity) {
        boolean isPoints = loyaltyProgramEntity.getProgramType().equals(getString(R.string.simple_points));
        boolean isStamps = loyaltyProgramEntity.getProgramType().equals(getString(R.string.stamps_program));
        customerNameView.setText(TextUtilsHelper.capitalize(customerEntity.getFirstName() + " now has"));
        if (isPoints) {
            if (totalPoints == 1) {
                programTypeTextView.setText(getString(R.string.point));
            } else {
                programTypeTextView.setText(getString(R.string.points));
            }
            customerLoyaltyWorthView.setText(String.valueOf(totalPoints));
        } else if (isStamps) {
            if (totalStamps == 1) {
                programTypeTextView.setText(getString(R.string.stamp));
            } else {
                programTypeTextView.setText(getString(R.string.stamps));
            }
            customerLoyaltyWorthView.setText(String.valueOf(totalStamps));
        }

        RxView.clicks(continueBtn).subscribe(o -> {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
            if (isPosTurnedOn) {
                if (isPoints) {
                    Intent intent = new Intent(mContext, PointsSaleWithPosActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(Constants.LOYALTY_PROGRAM_ID, loyaltyProgramEntity.getId());
                    startActivity(intent);
                } else if (isStamps) {
                    Intent intent = new Intent(mContext, StampsSaleWithPosActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(Constants.LOYALTY_PROGRAM_ID, loyaltyProgramEntity.getId());
                    startActivity(intent);
                }
            } else {
                Intent intent = new Intent(mContext, SaleWithoutPosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Constants.LOYALTY_PROGRAM_ID, loyaltyProgramEntity.getId());
                startActivity(intent);
            }
        });

        RxView.clicks(printReceiptBtn).subscribe(o -> {
            connect();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(mContext, MerchantBackOfficeActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateUpTo(new Intent(mContext, MerchantBackOfficeActivity.class));
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(syncFinishedReceiver);
            unregisterReceiver(syncStartedReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(syncFinishedReceiver, new IntentFilter(Constants.SYNC_FINISHED));
        registerReceiver(syncStartedReceiver, new IntentFilter(Constants.SYNC_STARTED));
    }

    private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            showSnackbar(R.string.sms_sent_notice);
        }
    };

    private BroadcastReceiver syncStartedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            showSnackbar(R.string.sms_would_be_sent_notice);
        }
    };

    protected void connect() {
        if(mBluetoothSocket == null){
            Intent BTIntent = new Intent(TransactionsConfirmation.this, BluetoothDeviseListActivity.class);
            startActivityForResult(BTIntent, BluetoothDeviseListActivity.REQUEST_CONNECT_BT);
        }
        else{
            OutputStream outputStream = null;
            try {
                outputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBtOutputStream = outputStream;
            printBt();
        }
    }

    private void printBt() {
        try {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mBtOutputStream = mBluetoothSocket.getOutputStream();

            byte[] printFormat = { 0x1B, 0x21, FONT_TYPE };
            mBtOutputStream.write(printFormat);
            String BILL = "";

            BILL = "\n" + mSessionManager.getBusinessName() + "    "
                    + TextUtilsHelper.getFormattedDateTimeString(Calendar.getInstance()) +"\n";
            BILL = BILL
                    + "-----------------------------------------";
            BILL = BILL + "\n\n";
            BILL = BILL + "Total Qty:" + "      " + "2.0\n";
            BILL = BILL + "Total Value:" + "     "
                    + "17625.0\n";
            BILL = BILL
                    + "-----------------------------------------\n";
            mBtOutputStream.write(BILL.getBytes());
            mBtOutputStream.write(0x0D);
            mBtOutputStream.write(0x0D);
            mBtOutputStream.write(0x0D);
            mBtOutputStream.flush();
        } catch (IOException e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if(mBluetoothSocket!= null){
                mBtOutputStream.close();
                mBluetoothSocket.close();
                mBluetoothSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothDeviseListActivity.REQUEST_CONNECT_BT) {
            if (resultCode == RESULT_OK) {
                try {
                    mBluetoothSocket = BluetoothDeviseListActivity.getSocket();
                    if(mBluetoothSocket != null){
                        printBt();
                    }

                } catch (Exception e) {
                    Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }
}
