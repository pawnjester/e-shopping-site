package co.loystar.loystarbusiness.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.SalesOrderItemBinding;
import co.loystar.loystarbusiness.fragments.SalesOrderDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.OrderPrintOptionsFetcher;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.OrderItemEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesOrder;
import co.loystar.loystarbusiness.models.entities.SalesOrderEntity;
import co.loystar.loystarbusiness.models.pojos.OrderPrintOption;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.TimeUtils;
import co.loystar.loystarbusiness.utils.ui.MyAlertDialog;
import co.loystar.loystarbusiness.utils.ui.OrderPrintOptionsDialogFragment;
import co.loystar.loystarbusiness.utils.ui.PrintTextFormatter;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;
import timber.log.Timber;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.support.v4.app.NavUtils.navigateUpFromSameTask;

public class SalesOrderListActivity extends RxAppCompatActivity
    implements OrderPrintOptionsDialogFragment.OnPrintOptionSelectedListener {

    private boolean mTwoPane;
    private ExecutorService executor;
    private final String KEY_RECYCLER_STATE = "recycler_state";
    private Bundle mBundleRecyclerViewState;
    private SessionManager mSessionManager;
    private ReactiveEntityStore<Persistable> mDataStore;
    private Context mContext;
    private FragmentManager fragmentManager;
    private SalesOrderListAdapter mAdapter;
    private MyAlertDialog myAlertDialog;
    private SalesOrderEntity mSelectedOrderEntity;

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

    /*Views*/
    @BindView(R.id.sales_order_list_rv)
    EmptyRecyclerView mRecyclerView;

    @BindView(R.id.salesOrderListContainer)
    View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salesorder_list);

        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.activity_sales_order_list_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        fragmentManager = getSupportFragmentManager();
        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        myAlertDialog = new MyAlertDialog();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAdapter = new SalesOrderListAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        if (findViewById(R.id.sales_order_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        setupRecyclerView();
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(Constants.SALES_ORDER_ID)){
            int orderId = intent.getIntExtra(Constants.SALES_ORDER_ID, 0);
            if (mTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID, orderId);
                SalesOrderDetailFragment salesOrderDetailFragment = new SalesOrderDetailFragment();
                salesOrderDetailFragment.setArguments(arguments);
                fragmentManager.beginTransaction()
                    .replace(R.id.sales_order_detail_container, salesOrderDetailFragment)
                    .commit();
            } else {
                Intent detailIntent = new Intent(mContext, SalesOrderDetailActivity.class);
                detailIntent.putExtra(SalesOrderDetailFragment.ARG_ITEM_ID, orderId);
                startActivity(detailIntent);
            }
        }
        else {
            if (mTwoPane) {
                Result<SalesOrderEntity> result = mAdapter.performQuery();
                if (result.first() != null) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID, result.first().getId());
                    SalesOrderDetailFragment salesOrderDetailFragment = new SalesOrderDetailFragment();
                    salesOrderDetailFragment.setArguments(arguments);
                    fragmentManager.beginTransaction()
                        .replace(R.id.sales_order_detail_container, salesOrderDetailFragment)
                        .commit();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        TextView emptyView = findViewById(R.id.no_orders_empty_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(
            getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
            getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setEmptyView(emptyView);
    }

    @Override
    public void onPrintOptionSelected(OrderPrintOption orderPrintOption) {
        if (orderPrintOption.id.equals(getString(R.string.customer_cashier_kitchen))) {
            ArrayList<OrderPrintOption> allPrintOptions = OrderPrintOptionsFetcher.getOrderPrintOptions(mContext);
            ArrayList<OrderPrintOption> printOptions = new ArrayList<>();
            for (OrderPrintOption printOption: allPrintOptions) {
                if (!printOption.id.equals(getString(R.string.customer_cashier_kitchen))) {
                    printOptions.add(printOption);
                }
            }
            for (OrderPrintOption printOption: printOptions) {
                Completable.complete()
                    .delay(2, TimeUnit.SECONDS)
                    .doOnComplete(() -> printViaBT(printOption))
                    .subscribe();
            }
        } else {
            printViaBT(orderPrintOption);
        }
    }

    private class SalesOrderListAdapter extends QueryRecyclerAdapter<SalesOrderEntity, BindingHolder<SalesOrderItemBinding>> {

       @Override
       public Result<SalesOrderEntity> performQuery() {
           MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
               .where(MerchantEntity.ID.eq(mSessionManager.getMerchantId()))
               .get()
               .firstOrNull();

           if (merchantEntity == null) {
               return null;
           }
           return mDataStore
               .select(SalesOrderEntity.class)
               .where(SalesOrderEntity.MERCHANT.eq(merchantEntity))
               .orderBy(SalesOrderEntity.CREATED_AT.desc())
               .get();
       }

       @Override
       public void onBindViewHolder(SalesOrderEntity item, BindingHolder<SalesOrderItemBinding> holder, int position) {
            holder.binding.setSalesOrder(item);

            if (item.getStatus().equals(getString(R.string.pending))) {
                holder.binding.printOrderReceipt.setVisibility(View.GONE);
                holder.binding.salesOrderActionsWrapper.setVisibility(View.VISIBLE);
                holder.binding.statusText.setText(getString(R.string.status_pending));
                holder.binding.statusText.setBackgroundColor(ContextCompat.getColor(mContext, R.color.orange));
            } else if (item.getStatus().equals(getString(R.string.completed))) {
                holder.binding.printOrderReceipt.setVisibility(View.VISIBLE);
                holder.binding.statusText.setText(getString(R.string.status_completed));
                holder.binding.statusText.setBackgroundColor(ContextCompat.getColor(mContext, R.color.green));
            } else if (item.getStatus().equals(getString(R.string.rejected))){
                holder.binding.printOrderReceipt.setVisibility(View.GONE);
                holder.binding.statusText.setText(getString(R.string.status_rejected));
                holder.binding.statusText.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
            }

            holder.binding.timestampText.setText(TimeUtils.getTimeAgo(item.getCreatedAt().getTime(), mContext));


            CustomerEntity customerEntity = item.getCustomer();
            String customerName = customerEntity.getFirstName() + " " + customerEntity.getLastName();
            holder.binding.customerName.setText(customerName);

           List<OrderItemEntity> orderItemEntities = item.getOrderItems();
           StringBuilder stringBuilder  = new StringBuilder();
           int count = 0;
           for (OrderItemEntity orderItemEntity: orderItemEntities) {
               count++;
               String productName = orderItemEntity.getProduct().getName();
               stringBuilder
                   .append(productName)
                   .append(" (")
                   .append(orderItemEntity.getQuantity())
                   .append(")");
               if (count < orderItemEntities.size()) {
                   stringBuilder.append(", ");
               }
           }
           holder.binding.orderDescription.setText(stringBuilder.toString());
           holder.binding.printOrderReceipt.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_print));

           holder.binding.getRoot().setLayoutParams(new FrameLayout.LayoutParams(
               ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
           );
       }

       @Override
       public BindingHolder<SalesOrderItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
           LayoutInflater inflater = LayoutInflater.from(parent.getContext());
           SalesOrderItemBinding binding = SalesOrderItemBinding.inflate(inflater);
           binding.getRoot().setTag(binding);

           binding.processBtn.setOnClickListener(view -> {
               if (mTwoPane) {
                   Bundle arguments = new Bundle();
                   arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID, binding.getSalesOrder().getId());
                   SalesOrderDetailFragment salesOrderDetailFragment = new SalesOrderDetailFragment();
                   salesOrderDetailFragment.setArguments(arguments);
                   fragmentManager.beginTransaction()
                       .replace(R.id.sales_order_detail_container, salesOrderDetailFragment)
                       .commit();
               } else {
                   Intent intent = new Intent(mContext, SalesOrderDetailActivity.class);
                   intent.putExtra(SalesOrderDetailFragment.ARG_ITEM_ID, binding.getSalesOrder().getId());
                   startActivity(intent);
               }
           });

           binding.printOrderReceipt.setOnClickListener(view -> {
                mSelectedOrderEntity = mDataStore.findByKey(SalesOrderEntity.class, binding.getSalesOrder().getId()).blockingGet();
                if (mSelectedOrderEntity != null) {
                    OrderPrintOptionsDialogFragment bottomSheetDialogFragment = OrderPrintOptionsDialogFragment.newInstance();
                    bottomSheetDialogFragment.setListener(SalesOrderListActivity.this);
                    if (!bottomSheetDialogFragment.isAdded()) {
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
                }
           });
           binding.itemStatusBloc.setOnClickListener(view -> processOrder(binding.getSalesOrder()));
           binding.itemDescriptionBloc.setOnClickListener(view -> processOrder(binding.getSalesOrder()));

           binding.rejectBtn.setOnClickListener(view -> {
               myAlertDialog.setTitle("Are you sure?");
               myAlertDialog.setPositiveButton(getString(R.string.confirm_reject), (dialogInterface, i) -> {
                   switch (i) {
                       case BUTTON_NEGATIVE:
                           dialogInterface.dismiss();
                           break;
                       case BUTTON_POSITIVE:
                           dialogInterface.dismiss();
                           SalesOrderEntity salesOrderEntity = mDataStore.findByKey(SalesOrderEntity.class, binding.getSalesOrder().getId()).blockingGet();
                           salesOrderEntity.setStatus(getString(R.string.rejected));
                           salesOrderEntity.setUpdateRequired(true);
                           mDataStore.update(salesOrderEntity).subscribe(orderEntity -> {
                               SyncAdapter.performSync(mContext, mSessionManager.getEmail());
                               mAdapter.queryAsync();
                           });
                           break;
                   }
               });
               myAlertDialog.setNegativeButtonText(getString(android.R.string.no));
               myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);
           });

           return new BindingHolder<>(binding);
       }

       private void processOrder(SalesOrder salesOrder) {
           if (mTwoPane) {
               Bundle arguments = new Bundle();
               arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID, salesOrder.getId());
               SalesOrderDetailFragment salesOrderDetailFragment = new SalesOrderDetailFragment();
               salesOrderDetailFragment.setArguments(arguments);
               fragmentManager.beginTransaction()
                   .replace(R.id.sales_order_detail_container, salesOrderDetailFragment)
                   .commit();
           } else {
               Intent intent = new Intent(mContext, SalesOrderDetailActivity.class);
               intent.putExtra(SalesOrderDetailFragment.ARG_ITEM_ID, salesOrder.getId());
               startActivity(intent);
           }
       }
   }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        mAdapter.close();
        try {
            closeBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*save RecyclerView state*/
        mBundleRecyclerViewState = new Bundle();
        Parcelable listState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        mBundleRecyclerViewState.putParcelable(KEY_RECYCLER_STATE, listState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*restore RecyclerView state*/
        if (mBundleRecyclerViewState != null) {
            Parcelable listState = mBundleRecyclerViewState.getParcelable(KEY_RECYCLER_STATE);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(listState);
        }
        mAdapter.queryAsync();
    }

    void printViaBT(OrderPrintOption printOption) {

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
                    if (device.getName().equals("Wari P1 BT")) {
                        mmDevice = device;
                        break;
                    }
                }
            }

            if (mmDevice == null) {
                showSnackbar(R.string.no_printer_devises_available);
            } else {
                openBT(printOption);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // tries to open a connection to the bluetooth printer device
    void openBT(OrderPrintOption printOption) {
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
                    sendData(printOption);
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

    private void sendData(OrderPrintOption printOption) throws IOException {
        Observable.fromCallable(() -> {
            try {
                PrintTextFormatter formatter = new PrintTextFormatter();
                String td = "%.2f";
                double totalCharge = Double.valueOf(String.format(Locale.UK, td, mSelectedOrderEntity.getTotal()));
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

                List<OrderItemEntity> orderItemEntities = mSelectedOrderEntity.getOrderItems();

                for (OrderItemEntity orderItem: orderItemEntities) {
                    ProductEntity productEntity = orderItem.getProduct();
                    if (productEntity != null) {
                        double tc = orderItem.getTotalPrice();
                        int tcv = Double.valueOf(String.format(Locale.UK, td, tc)).intValue();

                        BILL.append("\n").append(productEntity.getName());
                        writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
                        BILL = new StringBuilder();

                        BILL.append("\n").append(orderItem.getQuantity())
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

                BILL.append("\n").append("TOTAL").append("               ").append(totalCharge).append("\n");
                writeWithFormat(BILL.toString().getBytes(), formatter.bold(), formatter.leftAlign());
                BILL = new StringBuilder();

                if (printOption != null) {
                    if (printOption.id.equals(getString(R.string.customer_only))) {
                        BILL.append("\nThank you for your patronage.").append("\n\nPOWERED BY LOYSTAR");
                        writeWithFormat(BILL.toString().getBytes(), formatter.get(), formatter.leftAlign());
                    }
                }

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

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @MainThread
    private void showSnackbar(String message) {
        Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG).show();
    }
}
