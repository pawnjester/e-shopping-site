package co.loystar.loystarbusiness.activities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.DealAdapter;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.pojos.LoyaltyDeal;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.PrintTextFormatter;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonTransparent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SaleWithPosConfirmationActivity extends BaseActivity {
    private Context mContext;
    private SessionManager mSessionManager;
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Integer> mOrderSummaryItems = new HashMap<>();
    private ArrayList<LoyaltyDeal> loyaltyDeals = new ArrayList<>();
    private DatabaseManager mDatabaseManager;
    private CustomerEntity mCustomer;


    private View mLayout;

    /*bluetooth print*/
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    @BindView(R.id.btn_continue)
    BrandButtonNormal continueBtn;

    @BindView(R.id.printReceipt)
    BrandButtonTransparent printReceiptBtn;

    @BindView(R.id.deals_recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.no_customer_wrapper)
    View noCustomerWrapper;

    @BindView(R.id.deals_wrapper)
    View dealsWrapper;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_with_pos_confirmation);

        mLayout = findViewById(R.id.activity_sale_with_pos_confirmation_wrapper);
        mContext = this;
        mSessionManager = new SessionManager(this);
        mDatabaseManager = DatabaseManager.getInstance(this);

        int mCustomerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        mOrderSummaryItems = (HashMap<Integer, Integer>) getIntent().getSerializableExtra(Constants.ORDER_SUMMARY_ITEMS);
        mCustomer = mDatabaseManager.getCustomerById(mCustomerId);

        if (mCustomer == null) {
            noCustomerWrapper.setVisibility(View.VISIBLE);
            dealsWrapper.setVisibility(View.GONE);
        } else {
            noCustomerWrapper.setVisibility(View.GONE);
            dealsWrapper.setVisibility(View.VISIBLE);

            List<LoyaltyProgramEntity> programs = mDatabaseManager.getMerchantLoyaltyPrograms(mSessionManager.getMerchantId());
            for (LoyaltyProgramEntity programEntity: programs) {
                int total_user_points = mDatabaseManager.getTotalCustomerPointsForProgram(programEntity.getId(), mCustomerId);
                int total_user_stamps = mDatabaseManager.getTotalCustomerStampsForProgram(programEntity.getId(), mCustomerId);
                loyaltyDeals.add(new LoyaltyDeal(
                    programEntity.getThreshold(),
                    programEntity.getReward(),
                    programEntity.getProgramType(),
                    total_user_points,
                    total_user_stamps)
                );
            }

            DealAdapter mAdapter = new DealAdapter(getApplicationContext(), loyaltyDeals, mCustomer, deal -> {
                shareLoyaltyMessage(deal);
            });
            mRecyclerView.setHasFixedSize(true);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
            );
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean bluetoothPrintEnabled = sharedPreferences.getBoolean(getString(R.string.pref_enable_bluetooth_print_key), false);
        if (bluetoothPrintEnabled) {
            printReceiptBtn.setVisibility(View.VISIBLE);
        }

        if (!AccountGeneral.isAccountActive(this)) {
            Drawable drawable = ContextCompat.getDrawable(mContext, android.R.drawable.ic_dialog_alert);
            int color = ContextCompat.getColor(mContext, R.color.white);
            assert drawable != null;
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(mContext, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(mContext);
            }
            builder.setTitle("Your Account Is Inactive!")
                .setMessage("Kindly note functionalities on Loystar will be limited until you re-subscribe. Thank you for your patronage")
                .setPositiveButton(getString(R.string.pay_subscription), (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                .setIcon(drawable)
                .show();
        }

        RxView.clicks(continueBtn).subscribe(o -> {
            Intent intent = new Intent(mContext, SaleWithPosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

            RxView.clicks(printReceiptBtn).subscribe(o -> printViaBT());
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                navigateUpTo(new Intent(mContext, MerchantBackOfficeActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareLoyaltyMessage(LoyaltyDeal deal) {
        String txt = "";
        if (deal.getProgram_type().equals(getString(R.string.simple_points))) {
            if (deal.getTotal_user_points() >= deal.getThreshold()) {
                txt = "Dear " + mCustomer.getLastName()
                        +  " " + mCustomer.getFirstName()
                        + ", Congratulations! You have earned "
                        + deal.getReward() +
                        " from " + mSessionManager.getBusinessName() +
                        ". Redeem your reward with the code sent via SMS on your next purchase. Thank you";
            } else {
                int difference = 0;
                String type = "";
                int typeOfReward = 0;
                if (deal.getProgram_type().equals(getString(R.string.simple_points))) {
                    difference = deal.getThreshold() - deal.getTotal_user_points();
                    typeOfReward = deal.getTotal_user_points();
                    if (deal.getTotal_user_points() == 1) {
                        type = "point";
                    } else {
                        type = "points";
                    }
                } else if (deal.getProgram_type().equals(getString(R.string.stamps_program))) {
                    difference = deal.getThreshold() - deal.getTotal_user_stamps();
                    typeOfReward = deal.getTotal_user_stamps();
                    if (deal.getTotal_user_stamps() == 1) {
                        type = "stamp";
                    } else {
                        type = "stamps";
                    }
                }
                txt = "Hello " +  mCustomer.getLastName()
                        +  " " + mCustomer.getFirstName()
                        + ", you now have " + typeOfReward + " " + type + " from "
                        + mSessionManager.getBusinessName() + ". Earn " + difference + " more to redeem "
                        + deal.getReward() + ". Thank you.";
            }
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            String toNumber = mCustomer.getPhoneNumber().substring(1);
            String encodedString = URLEncoder.encode(txt, "UTF-8");
            String message = "http://api.whatsapp.com/send?phone="
                    + toNumber + "&text=" + encodedString;
            intent.setData(Uri.parse(message));
            startActivity(intent);
        } catch(Exception e) {
            e.printStackTrace();
        }
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

        }
    };

    private BroadcastReceiver syncStartedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

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

    void printViaBT() {

        if (!AccountGeneral.isAccountActive(this)) {
            Toast.makeText(mContext, getString(R.string.resubcribe_mzg), Toast.LENGTH_LONG).show();
        }
        else {

            String td = "%.2f";
            double totalCharge = 0;
            String textToPrint =
                    "<MEDIUM2><BOLD><CENTER>"+ mSessionManager.getBusinessName()+" <BR>" + // business name
                            "<SMALL><BOLD><CENTER>"+ mSessionManager.getAddressLine1()+" <BR>" + // AddressLine1
                            "<SMALL><BOLD><CENTER>"+ mSessionManager.getAddressLine2()+" <BR>" + // AddressLine2
                            "<SMALL><CENTER>"+mSessionManager.getContactNumber()+"<BR>" + // contact number
                            "<SMALL><CENTER>"+TextUtilsHelper.getFormattedDateTimeString(Calendar.getInstance())+"<BR>\n"; //time stamp
            textToPrint+="<LEFT>Item               ";
            textToPrint+=" <RIGHT>Subtotal<BR>\n";

            for (Map.Entry<Integer, Integer> orderItem: mOrderSummaryItems.entrySet()) {
                ProductEntity productEntity = mDatabaseManager.getProductById(orderItem.getKey());
                if (productEntity != null) {
                    double tc = productEntity.getPrice() * orderItem.getValue();
                    totalCharge += tc;
                    Double tcv = Double.valueOf(String.format(Locale.UK, td, tc));
                    textToPrint+= "<LEFT>"+productEntity.getName()+" ("+orderItem.getValue()+"x"+productEntity.getPrice()+")          ";
                    textToPrint+="<RIGHT>"+tcv+"<BR>";
                }
            }

            textToPrint+="<RIGHT><MEDIUM1>Total: "+totalCharge+"<BR><BR>";
            textToPrint+="<CENTER><BOLD>Thank you for your patronage :)<BR>";
            textToPrint+="<CENTER><BOLD><BR>";
            textToPrint+="<SMALL><CENTER>POWERED BY LOYSTAR<BR>";
            textToPrint+="<SMALL><CENTER>www.loystar.co<BR>";
            textToPrint+="<BR>";
            textToPrint+="<SMALL><CENTER>------------------------<BR>";



            try {

                Intent intent = new Intent("pe.diegoveloper.printing");
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, textToPrint);
                startActivity(intent);

            } catch (ActivityNotFoundException ex) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=pe.diegoveloper.printerserverapp"));
                startActivity(intent);
            }
        }
    }

    // tries to open a connection to the bluetooth printer device
    void openBT() {
        Observable.fromCallable(() -> {
            try {
                mmDevice.fetchUuidsWithSdp();
                ParcelUuid[] parcelUuid = mmDevice.getUuids();
                if (parcelUuid != null) {
                    UUID uuid = parcelUuid[0].getUuid();
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                    mmSocket.connect();
                    mmOutputStream = mmSocket.getOutputStream();
                    mmInputStream = mmSocket.getInputStream();
                }
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException ignored) {
                }

                throw Exceptions.propagate(e);
            }
            return true;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .doOnSubscribe(disposable -> showSnackbar(R.string.opening_printer_connection))
                .subscribe(t -> {
                    if (mmOutputStream == null) {
                        Toast.makeText(mContext, getString(R.string.error_printer_connection), Toast.LENGTH_LONG).show();
                    } else {
                        beginListenForData();
                        sendData();
                    }
                }, throwable -> Toast.makeText(mContext, getString(R.string.error_printer_connection), Toast.LENGTH_LONG).show());

    }

    /*
* after opening a connection to bluetooth printer device,
* we have to listen and check if a data were sent to be printed.
*/
    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // this is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();

                        if (bytesAvailable > 0) {

                            byte[] packetBytes = new byte[bytesAvailable];
                            //noinspection ResultOfMethodCallIgnored
                            mmInputStream.read(packetBytes);

                            for (int i = 0; i < bytesAvailable; i++) {

                                byte b = packetBytes[i];
                                if (b == delimiter) {

                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(
                                        readBuffer, 0,
                                        encodedBytes, 0,
                                        encodedBytes.length
                                    );

                                    // specify US-ASCII encoding
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    // tell the user data were sent to bluetooth printer device
                                    handler.post(() -> {});

                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }

                    } catch (IOException ex) {
                        stopWorker = true;
                    }

                }
            });

            workerThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // this will send text data to be printed by the bluetooth printer
    void sendData() throws IOException{
        Observable.fromCallable(() -> {
            try {
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

                BILL.append("\n").append("-------------------------------");
                writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
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
                            .append(productEntity.getPrice())
                            .append("          ").append(tcv);
                        writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
                        BILL = new StringBuilder();
                    }
                }

                BILL.append("\n").append("-------------------------------");
                writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
                BILL = new StringBuilder();

                totalCharge = Double.valueOf(String.format(Locale.UK, td, totalCharge));
                BILL.append("\n").append("TOTAL").append("               ").append(totalCharge).append("\n");
                writeWithFormat(BILL.toString().getBytes(), formatter.bold(), formatter.leftAlign());
                BILL = new StringBuilder();

                BILL.append("\nThank you for your patronage.").append("\n\nPOWERED BY LOYSTAR");
                writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());

                mmOutputStream.write(0x0D);
                mmOutputStream.write(0x0D);
                mmOutputStream.write(0x0D);
                return true;
            } catch (IOException e) {
                try {
                    closeBT();
                } catch (IOException ignored){}
                throw Exceptions.propagate(e);
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToLifecycle())
            .doOnError(throwable -> showSnackbar(throwable.getMessage()))
            .subscribe(o -> {
                try {
                    closeBT();
                } catch (IOException ignored){}
            }, throwable -> Toast.makeText(mContext, getString(R.string.error_printer_connection), Toast.LENGTH_LONG).show());
    }


    // close the connection to bluetooth printer.
    void closeBT() throws IOException {
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
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
            Timber.e(e);
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
