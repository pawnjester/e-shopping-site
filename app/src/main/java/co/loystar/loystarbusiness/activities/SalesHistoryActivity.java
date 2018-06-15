package co.loystar.loystarbusiness.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.darwindeveloper.onecalendar.clases.Day;
import com.darwindeveloper.onecalendar.views.OneCalendarView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.fragments.CardSalesHistoryFragment;
import co.loystar.loystarbusiness.fragments.CashSalesHistoryFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.pojos.OrderSummaryItem;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.EventBus.SaleHistoryPrintEventBus;
import co.loystar.loystarbusiness.utils.EventBus.SalesDetailFragmentEventBus;
import co.loystar.loystarbusiness.utils.Foreground;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.PrintTextFormatter;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import timber.log.Timber;

public class SalesHistoryActivity extends BaseActivity {

    private static final int REQUEST_CHOOSE_PROGRAM = 110;

    @BindView(R.id.saleDateCalendarSelect)
    OneCalendarView calendarView;

    @BindView(R.id.noSalesView)
    View noSalesView;

    @BindView(R.id.calendarBloc)
    View calendarBloc;

    @BindView(R.id.stateImage)
    ImageView stateImage;

    @BindView(R.id.stateIntroText)
    TextView stateIntroText;

    @BindView(R.id.stateDescriptionText)
    TextView stateDescriptionText;

    @BindView(R.id.stateActionBtn)
    BrandButtonNormal stateActionBtn;

    @BindView(R.id.sales_detail_bs_toolbar)
    Toolbar salesDetailToolbar;

    @BindView(R.id.sales_date)
    TextView salesDateView;

    @BindView(R.id.total_sales)
    TextView totalSalesView;

    @BindView(R.id.tabs)
    TabLayout tabLayout;

    @BindView(R.id.activity_sales_history_vp)
    ViewPager mViewPager;

    @BindView(R.id.sales_history_container)
    View mLayout;

    private Context mContext;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SessionManager mSessionManager;
    private MerchantEntity merchantEntity;
    private Date selectedDate;
    private ArrayList<OrderSummaryItem> orderSummaryItems = new ArrayList<>();

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

    private BottomSheetBehavior bottomSheetBehavior;
    private double totalCardSalesForDateSelected;
    private double totalCashSalesForDateSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_history);

        mContext = this;
        ButterKnife.bind(this);
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        calendarView.setOneCalendarClickListener(new OneCalendarView.OneCalendarClickListener() {
            @Override
            public void dateOnClick(Day day, int position) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(day.getDate());
                calendar.set(Calendar.YEAR, 2018);

                selectedDate = calendar.getTime();
                setTotalSales();
            }

            @Override
            public void dateOnLongClick(Day day, int position) {

            }
        });

        calendarView.setOnCalendarChangeListener(new OneCalendarView.OnCalendarChangeListener() {
            @Override
            public void prevMonth() {

            }

            @Override
            public void nextMonth() {

            }
        });

        stateActionBtn.setOnClickListener(view -> startSale());

        mDataStore.count(SaleEntity.class).get().single().subscribe(integer -> {
            if (integer == 0) {
                showNoSalesView();
            }
        });

        setUpBottomSheet();
        Date preselectedSaleDate = null;
        if (getIntent().hasExtra(Constants.SALE_DATE)) {
            preselectedSaleDate = (Date) getIntent().getSerializableExtra(Constants.SALE_DATE);
        }

        if (preselectedSaleDate != null) {
            selectedDate = preselectedSaleDate;
            setTotalSales();
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setTotalSales() {
        Calendar startDayCal = Calendar.getInstance();
        startDayCal.setTime(selectedDate);

        Calendar nextDayCal = Calendar.getInstance();
        nextDayCal.setTime(selectedDate);
        nextDayCal.add(Calendar.DAY_OF_MONTH, 1);

        Selection<ReactiveResult<Tuple>> cashResultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
        cashResultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
        cashResultSelection.where(SaleEntity.PAYED_WITH_CASH.eq(true));
        cashResultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

        Tuple cashTuple = cashResultSelection.get().firstOrNull();
        if (cashTuple == null || cashTuple.get(0) == null) {
            totalCashSalesForDateSelected = 0;
        } else {
            Double total = cashTuple.get(0);
            if (total > 0) {
                totalCashSalesForDateSelected = total;
            } else {
                totalCashSalesForDateSelected = 0;
            }
        }

        Selection<ReactiveResult<Tuple>> cardResultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
        cardResultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
        cardResultSelection.where(SaleEntity.PAYED_WITH_CARD.eq(true));
        cardResultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

        Tuple cardTuple = cardResultSelection.get().firstOrNull();
        if (cardTuple == null || cardTuple.get(0) == null) {
            totalCardSalesForDateSelected = 0;
        } else {
            Double total = cardTuple.get(0);
            if (total > 0) {
                totalCardSalesForDateSelected = total;
            } else {
                totalCardSalesForDateSelected = 0;
            }
        }

        if (totalCardSalesForDateSelected == 0 && totalCashSalesForDateSelected == 0) {
            Toast.makeText(mContext, getString(R.string.no_sales_records), Toast.LENGTH_LONG).show();
        } else {
            showBottomSheet(true);
        }
    }

    private void setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.sale_detail_bottom_sheet_container));
        salesDetailToolbar.setNavigationOnClickListener(view -> showBottomSheet(false));
    }

    /**
     * Shows the no sales view and hides the calendar view.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showNoSalesView() {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        noSalesView.setVisibility(View.VISIBLE);
        stateImage.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_firstsale));
        stateIntroText.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateDescriptionText.setText(getString(R.string.start_sale_empty_state));
        stateActionBtn.setText(getString(R.string.start_sale_btn_label));

        noSalesView.animate()
            .setDuration(shortAnimTime)
            .alpha(1)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    calendarBloc.setVisibility(View.GONE);
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
                        if (Foreground.get().isForeground()) {
                            new AlertDialog.Builder(mContext)
                                .setTitle("No Loyalty Program Found!")
                                .setMessage("To record a sale, you would have to start a loyalty program.")
                                .setPositiveButton(mContext.getString(R.string.start_loyalty_program_btn_label), (dialog, which) -> {
                                    dialog.dismiss();
                                    startLoyaltyProgram();
                                })
                                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss()).show();
                        }
                    } else if (integer == 1) {
                        LoyaltyProgramEntity loyaltyProgramEntity = merchantEntity.getLoyaltyPrograms().get(0);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
                        if (isPosTurnedOn) {
                            startSaleWithPos();
                        } else {
                            startSaleWithoutPos(loyaltyProgramEntity.getId());
                        }
                    } else {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
                        if (isPosTurnedOn) {
                            startSaleWithPos();
                        } else {
                            chooseProgram();
                        }
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

    private void chooseProgram() {
        Intent intent = new Intent(this, ChooseProgramActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_PROGRAM);
    }

    private void startSaleWithPos() {
        Intent intent = new Intent(this, SaleWithPosActivity.class);
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

    private void showBottomSheet(boolean show) {
        if (show) {
            SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(mSectionsPagerAdapter);

            mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
            tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
            tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);

            salesDateView.setText(TextUtilsHelper.getFormattedDateString(calendar));
            double totalSales = totalCardSalesForDateSelected + totalCashSalesForDateSelected;
            String merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(mContext).getCurrency(mSessionManager.getCurrency()).getSymbol();
            totalSalesView.setText(getString(R.string.total_sale_value, merchantCurrencySymbol, String.valueOf(totalSales)));

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            mSectionsPagerAdapter.notifyDataSetChanged();
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            showBottomSheet(false);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = CashSalesHistoryFragment.getInstance(selectedDate);
                    break;
                case 1:
                    fragment = CardSalesHistoryFragment.getInstance(selectedDate);
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            if (object instanceof  CardSalesHistoryFragment) {
                ((CardSalesHistoryFragment) object).update(selectedDate);
            } else if (object instanceof CashSalesHistoryFragment) {
                ((CashSalesHistoryFragment) object).update(selectedDate);
            }
            return super.getItemPosition(object);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onResume() {
        super.onResume();

        SalesDetailFragmentEventBus
            .getInstance()
            .getFragmentEventObservable()
            .compose(bindToLifecycle())
            .subscribe(bundle -> {
                if (bundle.getInt(Constants.FRAGMENT_EVENT_ID, 0) == SalesDetailFragmentEventBus.ACTION_START_SALE) {
                    startSale();
                }
            });

        SaleHistoryPrintEventBus
            .getInstance()
            .getFragmentEventObservable()
            .compose(bindToLifecycle())
            .subscribe(bundle -> {
                if (bundle.getInt(Constants.FRAGMENT_EVENT_ID, 0) == SaleHistoryPrintEventBus.ACTION_START_PRINT) {
                    orderSummaryItems = (ArrayList<OrderSummaryItem>) bundle.getSerializable(Constants.ORDER_SUMMARY_ITEMS);
                    printViaBT();
                }
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CHOOSE_PROGRAM) {
                int programId = data.getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
                startSaleWithoutPos(programId);
            }

        }
    }

    public interface UpdateSelectedDateInterface {
        void update(Date date);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            closeBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    void printViaBT() {

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if(mBluetoothAdapter == null) {
                showSnackbar(R.string.no_bluetooth_adapter_available);
                return;
            }

            if(!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
                return;
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if(pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {

                    // RPP300 is the name of the bluetooth printer device
                    // we got this name from the list of paired devices
                    if (device.getName().equals("XL-1880")) {
                        mmDevice = device;
                        break;
                    }
                }
            }

            if (mmDevice == null) {
                showSnackbar(R.string.no_printer_devises_available);
            } else {
                openBT();
            }

        }catch(Exception e){
            e.printStackTrace();
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
                } catch (IOException ignored){}

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

                for (OrderSummaryItem orderItem: orderSummaryItems) {
                    totalCharge += orderItem.getTotal();

                    BILL.append("\n").append(orderItem.getName());
                    writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
                    BILL = new StringBuilder();

                    BILL.append("\n").append(orderItem.getCount())
                        .append(" ")
                        .append("x")
                        .append(" ")
                        .append(orderItem.getPrice())
                        .append("          ").append(orderItem.getTotal());
                    writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
                    BILL = new StringBuilder();
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
