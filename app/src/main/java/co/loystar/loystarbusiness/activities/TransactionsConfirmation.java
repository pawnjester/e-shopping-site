package co.loystar.loystarbusiness.activities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.PrintTextFormatter;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonTransparent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TransactionsConfirmation extends RxAppCompatActivity {
    private static final String TAG = TransactionsConfirmation.class.getSimpleName();
    private Context mContext;
    private SessionManager mSessionManager;
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Integer> mOrderSummaryItems = new HashMap<>();
    private int totalPoints;
    private int totalStamps;
    private DatabaseManager mDatabaseManager;

    private View mLayout;
    private TextView programTypeTextView;
    private TextView customerLoyaltyWorthView;
    private BrandButtonNormal continueBtn;
    private BrandButtonTransparent printReceiptBtn;

    /*bluetooth print*/
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;

    @SuppressWarnings("unchecked")
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
        mDatabaseManager = DatabaseManager.getInstance(this);

        int mCustomerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        int mLoyaltyProgramId = getIntent().getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
        boolean showContinueButton = getIntent().getBooleanExtra(Constants.SHOW_CONTINUE_BUTTON, false);
        boolean isPrintReceipt = getIntent().getBooleanExtra(Constants.PRINT_RECEIPT, false);
        mOrderSummaryItems = (HashMap<Integer, Integer>) getIntent().getSerializableExtra(Constants.ORDER_SUMMARY_ITEMS);
        totalPoints = getIntent().getIntExtra(Constants.TOTAL_CUSTOMER_POINTS, 0);
        totalStamps = getIntent().getIntExtra(Constants.TOTAL_CUSTOMER_STAMPS, 0);

        mLayout = findViewById(R.id.transactions_confirmation_wrapper);
        programTypeTextView = findViewById(R.id.program_type_text);
        customerLoyaltyWorthView = findViewById(R.id.customer_loyalty_worth);
        continueBtn = findViewById(R.id.activity_transactions_confirmation_continue_btn);
        printReceiptBtn = findViewById(R.id.printReceipt);

        if (showContinueButton) {
            continueBtn.setVisibility(View.VISIBLE);
        }

        if (isPrintReceipt) {
            printReceiptBtn.setVisibility(View.VISIBLE);
        }

        CustomerEntity mCustomer = mDatabaseManager.getCustomerById(mCustomerId);
        LoyaltyProgramEntity mLoyaltyProgram = mDatabaseManager.getLoyaltyProgramById(mLoyaltyProgramId);

        if (mLoyaltyProgram != null && mCustomer != null) {
            setupViews(mLoyaltyProgram, mCustomer);
        }
    }

    private void setupViews(@NonNull LoyaltyProgramEntity loyaltyProgramEntity, @NonNull CustomerEntity customerEntity) {
        boolean isPoints = loyaltyProgramEntity.getProgramType().equals(getString(R.string.simple_points));
        boolean isStamps = loyaltyProgramEntity.getProgramType().equals(getString(R.string.stamps_program));
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

        RxView.clicks(printReceiptBtn).subscribe(o -> printViaBT(loyaltyProgramEntity, customerEntity));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            closeBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // this will print to a bluetooth printer device
    void printViaBT(@NonNull LoyaltyProgramEntity loyaltyProgramEntity, @NonNull CustomerEntity customerEntity) {
        Observable.fromCallable(() -> {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if(mBluetoothAdapter == null) {
                showSnackbar(R.string.no_bluetooth_adapter_available);
                return false;
            }

            if(!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
                return false;
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                // RPP300 is the name of the bluetooth printer device
                // we got this name from the list of paired devices
                if (device.getName().equals("Wari P1 BT")) {
                    mmDevice = device;
                    showSnackbar(R.string.bluetooth_device_found);
                    break;
                }
            }
            if (mmDevice  == null) {
                showSnackbar(R.string.no_paired_bluetooth_devises_available);
                return false;
            }

            return true;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .doOnError(throwable -> showSnackbar(throwable.getMessage()))
                .subscribe(o -> openBT(loyaltyProgramEntity, customerEntity));
    }

    // tries to open a connection to the bluetooth printer device
    void openBT(@NonNull LoyaltyProgramEntity loyaltyProgramEntity, @NonNull CustomerEntity customerEntity) {
        Observable.fromCallable(() -> {
            ParcelUuid[] parcelUuid = mmDevice.getUuids();
            if (parcelUuid != null) {
                UUID uuid = parcelUuid[0].getUuid();
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                mmSocket.connect();
                mmOutputStream = mmSocket.getOutputStream();
                return true;
            }
            return false;
        }).subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToLifecycle())
            .doOnSubscribe(disposable -> showSnackbar(R.string.openining_bluetooth_connection))
            .doOnError(throwable -> showSnackbar(throwable.getMessage()))
            .subscribe(o -> sendData(loyaltyProgramEntity, customerEntity));
    }

    // this will send text data to be printed by the bluetooth printer
    void sendData(@NonNull LoyaltyProgramEntity loyaltyProgramEntity, @NonNull CustomerEntity customerEntity) throws IOException {
        Observable.fromCallable(() -> {
            PrintTextFormatter formatter = new PrintTextFormatter();
            String td = "%.2f";
            double totalCharge = 0;
            StringBuilder BILL = new StringBuilder();

                /*print business name start*/
            BILL.append("\n").append(mSessionManager.getBusinessName());
            writeWithFormat(BILL.toString().getBytes(), formatter.bold(), formatter.centerAlign());
            BILL = new StringBuilder();
                /* print business name end*/

                /*print timestamp start*/
            BILL.append("\n").append(TextUtilsHelper.getFormattedDateTimeString(Calendar.getInstance()));
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.centerAlign());
            BILL = new StringBuilder();
                /*print timestamp end*/

            BILL.append("\n").append("-------------------------");
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
            BILL = new StringBuilder();

            BILL.append("\n\n");
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.get());
            BILL = new StringBuilder();

            for (Map.Entry<Integer, Integer> orderItem: mOrderSummaryItems.entrySet()) {
                ProductEntity productEntity = mDatabaseManager.getProductById(orderItem.getKey());
                if (productEntity != null) {
                    double tc = productEntity.getPrice() * orderItem.getValue();
                    totalCharge += tc;
                    int tcv = Double.valueOf(String.format(Locale.UK, td, tc)).intValue();

                    BILL.append("\n").append(productEntity.getName());
                    writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
                    BILL = new StringBuilder();

                    BILL.append("\n").append(orderItem.getValue())
                            .append(" ")
                            .append("x")
                            .append(" ")
                            .append(productEntity.getPrice());
                    writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
                    BILL = new StringBuilder();

                    BILL.append(tcv);
                    writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.rightAlign());
                    BILL = new StringBuilder();

                    BILL.append("\n\n");
                    writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.get());
                    BILL = new StringBuilder();
                }
            }

            BILL.append("\n").append("-------------------------");
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
            BILL = new StringBuilder();

            BILL.append("\n").append("TOTAL");
            writeWithFormat(BILL.toString().getBytes(), formatter.bold(), formatter.leftAlign());
            BILL = new StringBuilder();

            totalCharge = Double.valueOf(String.format(Locale.UK, td, totalCharge));
            BILL.append(totalCharge);
            writeWithFormat(BILL.toString().getBytes(), formatter.bold(), formatter.rightAlign());
            BILL = new StringBuilder();

            BILL.append("\n\n");
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.get());
            BILL = new StringBuilder();

            BILL.append("\n").append("-------------------------");
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
            BILL = new StringBuilder();

            String pTxt;
            if (totalPoints == 1) {
                pTxt = getString(R.string.point);
            } else {
                pTxt = getString(R.string.points);
            }
            int pointsDiff = loyaltyProgramEntity.getThreshold() - totalPoints;
            BILL.append("\n").append(customerEntity.getFirstName())
                    .append(" you now have ")
                    .append(totalPoints)
                    .append(" ")
                    .append(pTxt)
                    .append(", spend ")
                    .append(pointsDiff)
                    .append(" more to get your ")
                    .append(loyaltyProgramEntity.getReward());
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
            BILL = new StringBuilder();

            BILL.append("\nThank you for your patronage.");
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
            BILL = new StringBuilder();

            BILL.append("\n\n");
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.get());
            BILL = new StringBuilder();

            BILL.append("POWERED BY LOYSTAR");
            writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.centerAlign());
            mmOutputStream.flush();
            return true;
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToLifecycle())
            .doOnError(throwable -> showSnackbar(throwable.getMessage()))
            .subscribe(o -> closeBT());
    }

    // close the connection to bluetooth printer.
    void closeBT() throws IOException {
        try {
            mmOutputStream.close();
            mmSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to write with a given format
     *
     * @param buffer     the array of bytes to actually write
     * @param pFormat    The format byte array
     * @param pAlignment The alignment byte array
     */
    private void writeWithFormat(byte[] buffer, final byte[] pFormat, final byte[] pAlignment) {
        try {
            // Notify printer it should be printed with given alignment:
            mmOutputStream.write(pAlignment);
            // Notify printer it should be printed in the given format:
            mmOutputStream.write(pFormat);
            // Write the actual data:
            mmOutputStream.write(buffer, 0, buffer.length);

        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @MainThread
    private void showSnackbar(String message) {
        Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG).show();
    }
}
