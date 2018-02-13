package co.loystar.loystarbusiness.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.onesignal.OneSignal;
import com.roughike.bottombar.BottomBar;
import com.uxcam.UXCam;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.Foreground;
import co.loystar.loystarbusiness.utils.GraphCoordinates;
import co.loystar.loystarbusiness.utils.NotificationUtils;
import co.loystar.loystarbusiness.utils.fcm.SendFirebaseRegistrationToken;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import io.smooch.core.Smooch;
import io.smooch.core.User;
import timber.log.Timber;


public class MerchantBackOfficeActivity extends AppCompatActivity
    implements OnChartValueSelectedListener {

    private static final int REQUEST_CHOOSE_PROGRAM = 110;
    private SessionManager mSessionManager;
    private Context mContext;
    private ReactiveEntityStore<Persistable> mDataStore;
    private BottomBar bottomNavigationBar;
    private String merchantCurrencySymbol;
    private BarChart barChart;
    private ImageView stateWelcomeImageView;
    private TextView stateWelcomeTextView;
    private TextView stateDescriptionTextView;
    private BrandButtonNormal stateActionBtn;
    private MerchantEntity merchantEntity;
    private MixpanelAPI mixpanelAPI;

    @BindView(R.id.merchant_back_office_layout)
    View mainLayout;

    @BindView(R.id.merchant_back_office_order_received_layout)
    View orderReceivedLayout;

    @BindView(R.id.orderReceivedImg)
    ImageView orderReceivedImgView;

    @BindView(R.id.viewBtn)
    Button viewOrderBtn;

    @BindView(R.id.backToHomeBtn)
    Button backToHomeBtn;

    @BindView(R.id.merchant_back_office_wrapper)
    View mLayout;

    @BindView(R.id.chartLayout)
    View chartLayout;

    @BindView(R.id.emptyStateLayout)
    View emptyStateLayout;

    @BindView(R.id.startSaleBtn)
    BrandButtonNormal startSaleBtn;

    @BindView(R.id.viewHistoryBtn)
    Button viewHistoryBtn;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Constants.PUSH_NOTIFICATION)) {
                if (intent.hasExtra(Constants.NOTIFICATION_TYPE)) {
                    if (intent.getStringExtra(Constants.NOTIFICATION_TYPE).equals(Constants.ORDER_RECEIVED_NOTIFICATION)) {

                        NotificationUtils notificationUtils = new NotificationUtils(MerchantBackOfficeActivity.this);
                        notificationUtils.playNotificationSound();

                        mainLayout.setVisibility(View.GONE);
                        orderReceivedLayout.setVisibility(View.VISIBLE);

                        Animation animation = new AlphaAnimation(1, 0);
                        animation.setDuration(1000);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.setRepeatCount(Animation.INFINITE);
                        animation.setRepeatMode(Animation.REVERSE);
                        orderReceivedImgView.startAnimation(animation);

                        int orderId = intent.getIntExtra(Constants.NOTIFICATION_ORDER_ID, 0);
                        viewOrderBtn.setOnClickListener(view -> {
                            Intent orderListIntent = new Intent(mContext, SalesOrderListActivity.class);
                            orderListIntent.putExtra(Constants.SALES_ORDER_ID, orderId);
                            startActivity(orderListIntent);
                        });
                    }
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_back_office);
        Toolbar toolbar = findViewById(R.id.activity_merchant_back_office_toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
            }
        }

        stateWelcomeImageView = emptyStateLayout.findViewById(R.id.stateImage);
        stateWelcomeTextView = emptyStateLayout.findViewById(R.id.stateIntroText);
        stateDescriptionTextView = emptyStateLayout.findViewById(R.id.stateDescriptionText);
        stateActionBtn = emptyStateLayout.findViewById(R.id.stateActionBtn);
        orderReceivedImgView.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_order));

        backToHomeBtn.setOnClickListener(view -> {
            mainLayout.setVisibility(View.VISIBLE);
            orderReceivedLayout.setVisibility(View.GONE);
        });

        mContext = this;
        mSessionManager = new SessionManager(this);
        mDataStore = DatabaseManager.getDataStore(this);
        merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(this).getCurrency(mSessionManager.getCurrency()).getSymbol();
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mSessionManager.getBusinessName().substring(0, 1).toUpperCase() + mSessionManager.getBusinessName().substring(1));
        }

        if (getIntent().getBooleanExtra(Constants.IS_NEW_LOGIN, false)) {
            SendFirebaseRegistrationToken sendFirebaseRegistrationToken = new SendFirebaseRegistrationToken(mContext);
            sendFirebaseRegistrationToken.sendRegistrationToServer();
        }

        if (getIntent().hasExtra(Constants.NOTIFICATION_MESSAGE)) {
            if (getIntent().getStringExtra(Constants.NOTIFICATION_TYPE).equals(Constants.ORDER_RECEIVED_NOTIFICATION)) {

                NotificationUtils notificationUtils = new NotificationUtils(MerchantBackOfficeActivity.this);
                notificationUtils.playNotificationSound();

                int orderId = getIntent().getIntExtra(Constants.NOTIFICATION_ORDER_ID, 0);
                mainLayout.setVisibility(View.GONE);
                orderReceivedLayout.setVisibility(View.VISIBLE);

                Animation animation = new AlphaAnimation(1, 0);
                animation.setDuration(1000);
                animation.setInterpolator(new LinearInterpolator());
                animation.setRepeatCount(Animation.INFINITE);
                animation.setRepeatMode(Animation.REVERSE);
                orderReceivedImgView.startAnimation(animation);

                viewOrderBtn.setOnClickListener(view -> {
                    Intent orderListIntent = new Intent(mContext, SalesOrderListActivity.class);
                    orderListIntent.putExtra(Constants.SALES_ORDER_ID, orderId);
                    startActivity(orderListIntent);
                });
            }
        }

        startSaleBtn.setOnClickListener(view -> startSale());
        viewHistoryBtn.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, SalesHistoryActivity.class);
            startActivity(intent);
        });

        setupView();
        setupGraph();
        setupBottomNavigation();
        initializePlugins();
    }

    private void initializePlugins() {
        if (!BuildConfig.DEBUG) {
            List<String> debugEmails = new ArrayList<>(Arrays.asList("loystarapp@gmail.com", "niinyarko1@gmail.com", "boxxy@gmail.com"));
            if (!debugEmails.contains(mSessionManager.getEmail())) {
                /* FirebaseAnalytics */
                FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
                mFirebaseAnalytics.setUserProperty("BusinessName", mSessionManager.getBusinessName());
                mFirebaseAnalytics.setUserProperty("ContactNumber", mSessionManager.getContactNumber());
                mFirebaseAnalytics.setUserProperty("BusinessType", mSessionManager.getBusinessType());
                mFirebaseAnalytics.setUserProperty("FirstName", mSessionManager.getFirstName());
                mFirebaseAnalytics.setUserProperty("LastName", mSessionManager.getLastName());

                /* Mixpanel */
                mixpanelAPI = MixpanelAPI.getInstance(mContext, BuildConfig.MIXPANEL_TOKEN);
                mixpanelAPI.identify(mSessionManager.getEmail());
                // create a user profile
                mixpanelAPI.getPeople().identify(mixpanelAPI.getDistinctId());
                mixpanelAPI.getPeople().identify(mSessionManager.getEmail());
                mixpanelAPI.getPeople().set("BusinessType", mSessionManager.getBusinessType());
                mixpanelAPI.getPeople().set("ContactNumber", mSessionManager.getContactNumber());
                mixpanelAPI.getPeople().set("BusinessName", mSessionManager.getBusinessName());
                mixpanelAPI.getPeople().set("phone", mSessionManager.getContactNumber());
                mixpanelAPI.getPeople().set("name", mSessionManager.getBusinessName());
                mixpanelAPI.getPeople().showNotificationIfAvailable(this);

                JSONObject props = new JSONObject();
                try {
                    /* these properties will be included with each event sent*/
                    props.put("BusinessType", mSessionManager.getBusinessType());
                    props.put("ContactNumber", mSessionManager.getContactNumber());
                    props.put("BusinessName", mSessionManager.getBusinessName());
                    props.put("Email", mSessionManager.getEmail());

                    mixpanelAPI.registerSuperProperties(props);
                } catch (JSONException e) {
                    FirebaseCrash.report(e);
                    e.printStackTrace();
                }

                /* Smooch */
                Smooch.login(mSessionManager.getEmail(), "jwt", null);
                User.getCurrentUser().setEmail(mSessionManager.getEmail());
                User.getCurrentUser().setFirstName(mSessionManager.getFirstName());

                final Map<String, Object> customProperties = new HashMap<>();
                customProperties.put("BusinessName", mSessionManager.getBusinessName());
                customProperties.put("ContactNumber", mSessionManager.getContactNumber());
                customProperties.put("BusinessType", mSessionManager.getBusinessType());

                boolean isProgramCreated = mDataStore.count(LoyaltyProgramEntity.class)
                        .where(LoyaltyProgramEntity.OWNER.eq(merchantEntity)).get().single().blockingGet() > 0;
                customProperties.put("createdLoyaltyProgram", isProgramCreated);
                User.getCurrentUser().addProperties(customProperties);

                /* OneSignal */
                OneSignal.sendTag("user", mSessionManager.getEmail());
                OneSignal.syncHashedEmail(mSessionManager.getEmail());

                /* UXCam */
                UXCam.startWithKey(BuildConfig.UXCAM_TOKEN);
                UXCam.tagUsersName(mSessionManager.getEmail());
            }
        }
    }

    private void setupView() {
        mDataStore.count(LoyaltyProgramEntity.class)
                .where(LoyaltyProgramEntity.OWNER.eq(merchantEntity)).get().single()
                .mergeWith(
                        mDataStore.count(SalesTransactionEntity.class)
                                .where(SalesTransactionEntity.MERCHANT.eq(merchantEntity))
                                .get()
                                .single()
                )
                .toList()
                .subscribe(integers -> {
                    int totalLoyaltyPrograms = integers.get(0);
                    int totalSales = integers.get(1);
                    if (totalLoyaltyPrograms == 0 || totalSales == 0) {
                        emptyStateLayout.setVisibility(View.VISIBLE);
                        chartLayout.setVisibility(View.GONE);
                        if (totalLoyaltyPrograms == 0) {
                            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_sunrise));
                            stateWelcomeTextView.setText(getString(R.string.welcome_text, mSessionManager.getFirstName()));
                            stateDescriptionTextView.setText(getString(R.string.start_loyalty_program_empty_state));
                            stateActionBtn.setText(getString(R.string.start_loyalty_program_btn_label));
                            stateActionBtn.setOnClickListener(view -> startLoyaltyProgram());
                        } else {
                            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_firstsale));
                            stateWelcomeTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
                            stateDescriptionTextView.setText(getString(R.string.start_sale_empty_state));
                            stateActionBtn.setText(getString(R.string.start_sale_btn_label));
                            stateActionBtn.setOnClickListener(view -> startSale());
                        }
                    } else {
                        chartLayout.setVisibility(View.VISIBLE);
                        emptyStateLayout.setVisibility(View.GONE);
                    }
                });
    }

    private void setupGraph() {
        barChart = findViewById(R.id.chart);
        barChart.setDrawValueAboveBar(true);
        barChart.setDescription(null);
        barChart.setNoDataText("No Sale Recorded");

        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);
        barChart.setOnChartValueSelectedListener(this);


        Paint p = barChart.getPaint(Chart.PAINT_INFO);
        int emptyStateTextSize = getResources().getDimensionPixelSize(R.dimen.empty_state_text_title);
        int chartPadding = getResources().getDimensionPixelSize(R.dimen.chart_padding);
        p.setTextSize(emptyStateTextSize);
        p.setTypeface(App.getInstance().getTypeface());
        p.setColor(ContextCompat.getColor(mContext, R.color.black_overlay));

        barChart.setExtraTopOffset(chartPadding);
        barChart.setExtraBottomOffset(chartPadding);
        addGraphDataset();
    }

    private void addGraphDataset() {
        mDataStore.select(SaleEntity.class).where(
                SaleEntity.MERCHANT.eq(merchantEntity))
                .get()
                .observableResult().subscribe(entities -> {
            if (!entities.toList().isEmpty()) {
                ArrayList<GraphCoordinates> graphCoordinates = getGraphValues(entities.toList());
                ArrayList<GraphCoordinates> cashGraphValues = getCashOnlyGraphValues(graphCoordinates);
                ArrayList<GraphCoordinates> cardGraphValues = getCardOnlyGraphValues(graphCoordinates);

                Collections.sort(cashGraphValues, new GraphCoordinatesComparator());
                Collections.sort(cardGraphValues, new GraphCoordinatesComparator());

                String[] xVals = new String[cashGraphValues.size()];
                ArrayList<BarEntry> yVals = new ArrayList<>();
                DateFormat outFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                Calendar todayCalendar = Calendar.getInstance();
                Calendar cal = Calendar.getInstance();
                try {
                    String todayDate = TextUtilsHelper.getFormattedDateString(todayCalendar);
                    Date todayDateWithoutTimeStamp = outFormatter.parse(todayDate);
                    for (int i = 0; i < cashGraphValues.size(); i++) {
                        GraphCoordinates cashGc = cashGraphValues.get(i);
                        GraphCoordinates cardGc = cardGraphValues.get(i);
                        cal.setTime(cashGc.getX());
                        String dateString = TextUtilsHelper.getFormattedDateString(cal);
                        Date salesCreatedAt = outFormatter.parse(dateString);
                        if (salesCreatedAt.equals(todayDateWithoutTimeStamp)) {
                            dateString = "today";
                        }
                        xVals[i] = dateString;
                        BarEntry entry = new BarEntry(i, new float[]{cashGc.getY(), cardGc.getY()});
                        entry.setData(cashGc.getX());
                        yVals.add(entry);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                XAxis xAxis = barChart.getXAxis();
                xAxis.setGranularity(1f);
                xAxis.setGranularityEnabled(true);
                xAxis.setDrawGridLines(false);
                xAxis.setValueFormatter(new IndexAxisValueFormatter(xVals));

                YAxis leftAxis = barChart.getAxisLeft();
                leftAxis.setValueFormatter(new MyAxisValueFormatter());
                leftAxis.setTypeface(App.getInstance().getTypeface());

                YAxis rightAxis = barChart.getAxisRight();
                rightAxis.setEnabled(false);

                BarDataSet barDataSet = new BarDataSet(yVals, "");
                barDataSet.setValueTypeface(App.getInstance().getTypeface());
                barDataSet.setValueTextSize(14);
                barDataSet.setDrawValues(false);
                barDataSet.setValueFormatter(new MyAxisValueFormatter());
                barDataSet.setColors(Arrays.asList(
                    ContextCompat.getColor(mContext, R.color.colorPrimaryDark),
                    ContextCompat.getColor(mContext, R.color.colorAccentLight)));
                barDataSet.setStackLabels(new String[]{"Cash Sales", "Card Sales"});

                ArrayList<IBarDataSet> dataSets = new ArrayList<>();
                dataSets.add(barDataSet);

                BarData data = new BarData(dataSets);
                data.setValueTextSize(10f);
                data.setValueTypeface(App.getInstance().getTypeface());
                data.setBarWidth(0.9f);
                data.notifyDataChanged();

                barChart.setData(data);
                barChart.notifyDataSetChanged();
                barChart.invalidate();
            }
        });
    }

    private ArrayList<GraphCoordinates> getCardOnlyGraphValues(ArrayList<GraphCoordinates> graphCoordinates) {
        ArrayList<GraphCoordinates> cardOnlyGraphCoordinates = new ArrayList<>();

        for (GraphCoordinates gc: graphCoordinates) {
            Calendar startDayCal = Calendar.getInstance();
            startDayCal.setTime(gc.getX());

            Calendar nextDayCal = Calendar.getInstance();
            nextDayCal.setTime(gc.getX());
            nextDayCal.add(Calendar.DAY_OF_MONTH, 1);

            Selection<ReactiveResult<Tuple>> resultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
            resultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
            resultSelection.where(SaleEntity.PAYED_WITH_CARD.eq(true));
            resultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

            Tuple tuple = resultSelection.get().firstOrNull();
            if (tuple == null || tuple.get(0) == null) {
                cardOnlyGraphCoordinates.add(new GraphCoordinates(gc.getX(), 0));
            } else {
                Double total = tuple.get(0);
                cardOnlyGraphCoordinates.add(new GraphCoordinates(gc.getX(), total.intValue()));
            }
        }
        return cardOnlyGraphCoordinates;
    }

    private ArrayList<GraphCoordinates> getCashOnlyGraphValues(ArrayList<GraphCoordinates> graphCoordinates) {
        ArrayList<GraphCoordinates> cashOnlyGraphCoordinates = new ArrayList<>();
        for (GraphCoordinates gc: graphCoordinates) {
            Calendar startDayCal = Calendar.getInstance();
            startDayCal.setTime(gc.getX());

            Calendar nextDayCal = Calendar.getInstance();
            nextDayCal.setTime(gc.getX());
            nextDayCal.add(Calendar.DAY_OF_MONTH, 1);


            Selection<ReactiveResult<Tuple>> resultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
            resultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
            resultSelection.where(SaleEntity.PAYED_WITH_CASH.eq(true));
            resultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

            Tuple tuple = resultSelection.get().firstOrNull();
            if (tuple == null || tuple.get(0) == null) {
                cashOnlyGraphCoordinates.add(new GraphCoordinates(gc.getX(), 0));
            } else {
                Double total = tuple.get(0);
                cashOnlyGraphCoordinates.add(new GraphCoordinates(gc.getX(), total.intValue()));
            }
        }
        return cashOnlyGraphCoordinates;
    }

    private ArrayList<GraphCoordinates> getGraphValues(List<SaleEntity> entities) {
        HashMap<Date, Integer> dateToAmount = new HashMap<>();
        ArrayList<GraphCoordinates> graphCoordinates = new ArrayList<>();
        Calendar todayCalendar = Calendar.getInstance();
        String todayDateString = TextUtilsHelper.getFormattedDateString(todayCalendar);
        DateFormat outFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        try {
            Date todayDateWithoutTimeStamp = outFormatter.parse(todayDateString);
            ArrayList<Date> transactionDatesFor2day = new ArrayList<>();
            for (SaleEntity saleEntity: entities) {
                Date createdAt = saleEntity.getCreatedAt();
                Integer amount = (int) saleEntity.getTotal();

                Calendar cal = Calendar.getInstance();
                cal.setTime(createdAt);
                String formattedDate = TextUtilsHelper.getFormattedDateString(cal);
                Date createdAtWithoutTime = outFormatter.parse(formattedDate);

                if (todayDateWithoutTimeStamp.equals(createdAtWithoutTime)) {
                    transactionDatesFor2day.add(createdAtWithoutTime);
                }

                if (dateToAmount.get(createdAtWithoutTime) != null) {
                    amount += dateToAmount.get(createdAtWithoutTime);
                }
                dateToAmount.put(createdAtWithoutTime, amount);
            }

            if (!dateToAmount.isEmpty()) {
                if (transactionDatesFor2day.isEmpty()) {
                    dateToAmount.put(todayDateWithoutTimeStamp, 0);
                }

                ArrayList<GraphCoordinates> allSalesRecords = new ArrayList<>();

                for (Map.Entry<Date, Integer> entry : dateToAmount.entrySet()) {
                    allSalesRecords.add(new GraphCoordinates(entry.getKey(), entry.getValue()));
                }

                Collections.sort(allSalesRecords, new GraphCoordinatesComparator());
                Collections.reverse(allSalesRecords);

                if (allSalesRecords.size() > 3) {
                    for (int i=0; i < 3; i++) {
                        GraphCoordinates record = allSalesRecords.get(i);
                        graphCoordinates.add(record);
                    }
                }
                else {
                    graphCoordinates.addAll(allSalesRecords);
                }

                Collections.sort(graphCoordinates, new GraphCoordinatesComparator());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return graphCoordinates;
    }

    private void setupBottomNavigation() {
        bottomNavigationBar = findViewById(R.id.bottom_navigation_bar);
        bottomNavigationBar.setOnTabSelectListener(tabId -> {
            switch (tabId) {
                case R.id.customers:
                    Intent intent = new Intent(mContext, CustomerListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    break;
                case R.id.orders:
                    Intent ordersIntent = new Intent(this, SalesOrderListActivity.class);
                    ordersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(ordersIntent);
                    break;
            }
        });
        bottomNavigationBar.setOnTabReselectListener(tabId -> {
            if (tabId == R.id.home) {
                if (barChart != null) {
                    barChart.highlightValues(null);
                    barChart.fitScreen();
                }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.merchant_back_office, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        ComponentName componentName = new ComponentName(mContext, SearchableActivity.class);
        if (searchView != null && searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));
            searchView.setIconifiedByDefault(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.sync_now_item:
                SyncAdapter.performSync(mContext, mSessionManager.getEmail());
                return true;
            case R.id.action_settings:
                Intent settings_intent = new Intent(mContext, SettingsActivity.class);
                startActivity(settings_intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
            unregisterReceiver(salesTransactionsSyncFinishedReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(syncFinishedReceiver, new IntentFilter(Constants.SYNC_FINISHED));
        registerReceiver(syncStartedReceiver, new IntentFilter(Constants.SYNC_STARTED));
        registerReceiver(salesTransactionsSyncFinishedReceiver, new IntentFilter(Constants.SALES_TRANSACTIONS_SYNC_FINISHED));

        if (bottomNavigationBar != null){
            bottomNavigationBar.selectTabWithId(R.id.home);
        }

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
            new IntentFilter(Constants.PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());
    }

    private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            showSnackbar(R.string.records_updated_notice);
        }
    };

    private BroadcastReceiver syncStartedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            showSnackbar(R.string.records_to_be_updated_notice);
        }
    };

    private BroadcastReceiver salesTransactionsSyncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            setupView();
            addGraphDataset();
        }
    };

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        if (e.getY() > 0) {
            if (h.getStackIndex() == 0) {
                // cash sale selected
                Intent intent = new Intent(mContext, SalesHistoryActivity.class);
                Bundle args = new Bundle();
                args.putString(Constants.TYPE_OF_SALE, Constants.CASH_SALE);
                args.putSerializable(Constants.SALE_DATE, (Date) e.getData());
                intent.putExtras(args);
                startActivity(intent);
            } else if (h.getStackIndex() == 1) {
                // card sale selected
                Intent intent = new Intent(mContext, SalesHistoryActivity.class);
                Bundle args = new Bundle();
                args.putString(Constants.TYPE_OF_SALE, Constants.CARD_SALE);
                args.putSerializable(Constants.SALE_DATE, (Date) e.getData());
                intent.putExtras(args);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onNothingSelected() {

    }

    private class GraphCoordinatesComparator implements Comparator<GraphCoordinates> {
        @Override
        public int compare(GraphCoordinates o1, GraphCoordinates o2) {
            return o1.getX().compareTo(o2.getX());
        }
    }

    private class MyAxisValueFormatter implements IAxisValueFormatter, IValueFormatter {

        private String mFormat;

        private MyAxisValueFormatter() {
            mFormat = "%s %s";
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return String.format(mFormat, merchantCurrencySymbol, value);
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return String.format(mFormat, merchantCurrencySymbol, Math.round(value));
        }
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

    @Override
    protected void onDestroy() {
        if (mixpanelAPI != null) {
            mixpanelAPI.flush();
        }
        super.onDestroy();
    }
}
