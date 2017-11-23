package co.loystar.loystarbusiness.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.GraphCoordinates;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;


public class MerchantBackOfficeActivity extends AppCompatActivity implements OnChartValueSelectedListener {
    private static final String TAG = MerchantBackOfficeActivity.class.getCanonicalName();
    private SessionManager mSessionManager;
    private Context mContext;
    private View mLayout;
    private ReactiveEntityStore<Persistable> mDataStore;
    private BottomBar bottomNavigationBar;
    private String merchantCurrencySymbol;
    private BarChart barChart;
    private View chartLayout;
    private View emptyStateLayout;
    private ImageView stateWelcomeImageView;
    private TextView stateWelcomeTextView;
    private TextView stateDescriptionTextView;
    private BrandButtonNormal stateActionBtn;
    private MerchantEntity merchantEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_back_office);
        Toolbar toolbar = findViewById(R.id.activity_merchant_back_office_toolbar);
        setSupportActionBar(toolbar);

        mLayout = findViewById(R.id.merchant_back_office_wrapper);
        chartLayout = findViewById(R.id.chartLayout);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        stateWelcomeImageView = emptyStateLayout.findViewById(R.id.welcomeImage);
        stateWelcomeTextView = emptyStateLayout.findViewById(R.id.welcomeText);
        stateDescriptionTextView = emptyStateLayout.findViewById(R.id.stateDescriptionText);
        stateActionBtn = emptyStateLayout.findViewById(R.id.stateActionBtn);

        mContext = this;
        mSessionManager = new SessionManager(this);
        mDataStore = DatabaseManager.getDataStore(this);
        merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(this).getCurrency(mSessionManager.getCurrency()).getSymbol();
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mSessionManager.getBusinessName().substring(0, 1).toUpperCase() + mSessionManager.getBusinessName().substring(1));
        }

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
                        } else {
                            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_firstsale));
                            stateWelcomeTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
                            stateDescriptionTextView.setText(getString(R.string.start_sale_empty_state));
                            stateActionBtn.setText(getString(R.string.start_sale_btn_label));
                        }
                    } else {
                        chartLayout.setVisibility(View.VISIBLE);
                        emptyStateLayout.setVisibility(View.GONE);
                    }
                });

        setupGraph();
        setupBottomNavigation();
    }

    private void setupGraph() {
        barChart = findViewById(R.id.chart);
        barChart.setDrawValueAboveBar(true);
        barChart.setDescription(null);
        barChart.setNoDataText("No Sales Recorded");

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
        mDataStore.select(SalesTransactionEntity.class).where(
                SalesTransactionEntity.MERCHANT.eq(merchantEntity))
                .get()
                .observableResult().subscribe(entities -> {
            List<SalesTransactionEntity> salesTransactionEntities = entities.toList();
            if (!salesTransactionEntities.isEmpty()) {
                ArrayList<GraphCoordinates> graphValues = getGraphValues(salesTransactionEntities);
                String[] xVals = new String[graphValues.size()];
                ArrayList<BarEntry> yVals = new ArrayList<>();
                DateFormat outFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                Calendar todayCalendar = Calendar.getInstance();
                Calendar cal = Calendar.getInstance();
                try {
                    String todayDate = TextUtilsHelper.getFormattedDateString(todayCalendar);
                    Date todayDateWithoutTimeStamp = outFormatter.parse(todayDate);
                    for (int i = 0; i < graphValues.size(); i++) {
                        GraphCoordinates gc = graphValues.get(i);
                        cal.setTime(gc.getX());
                        String dateString = TextUtilsHelper.getFormattedDateString(cal);
                        Date salesCreatedAt = outFormatter.parse(dateString);
                        if (salesCreatedAt.equals(todayDateWithoutTimeStamp)) {
                            dateString = "today";
                        }
                        xVals[i] = dateString;
                        yVals.add(new BarEntry(i, gc.getY()));
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

                BarDataSet barDataSet = new BarDataSet(yVals, "Total Sales");
                barDataSet.setValueTypeface(App.getInstance().getTypeface());
                barDataSet.setValueTextSize(14);
                barDataSet.setValueFormatter(new MyAxisValueFormatter());
                barDataSet.setColor(ContextCompat.getColor(mContext, R.color.colorAccentLight));

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

    private ArrayList<GraphCoordinates> getGraphValues(List<SalesTransactionEntity> entities) {
        HashMap<Date, Integer> dateToAmount = new HashMap<>();
        ArrayList<GraphCoordinates> graphCoordinates = new ArrayList<>();
        Calendar todayCalendar = Calendar.getInstance();
        String todayDateString = TextUtilsHelper.getFormattedDateString(todayCalendar);
        DateFormat outFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        try {
            Date todayDateWithoutTimeStamp = outFormatter.parse(todayDateString);
            ArrayList<Date> transactionDatesFor2day = new ArrayList<>();
            for (SalesTransactionEntity transactionEntity: entities) {
                Date createdAt = transactionEntity.getCreatedAt();
                Integer amount = transactionEntity.getAmount();

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

                Collections.sort(allSalesRecords, new pairObjectDateComparator());
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

                Collections.sort(graphCoordinates, new pairObjectDateComparator());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return graphCoordinates;
    }

    private void setupBottomNavigation() {
        bottomNavigationBar = findViewById(R.id.bottom_navigation_bar);
        bottomNavigationBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.record_sale:
                        /*boolean isPosTurnedOn = merchant.getTurn_on_point_of_sale() != null
                                && merchant.getTurn_on_point_of_sale();
                        loyaltyPrograms = databaseHelper.listMerchantPrograms(sessionManager.getMerchantId());

                        if (loyaltyPrograms.isEmpty()) {
                            Intent intent = new Intent(mContext, CreateLoyaltyProgramListActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                        else {
                            if (loyaltyPrograms.size() == 1) {
                                DBMerchantLoyaltyProgram loyaltyProgram = loyaltyPrograms.get(0);
                                if (isPosTurnedOn) {
                                    if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                                        Intent intent = new Intent(mContext, RecordPointsSalesWithPosActivity.class);
                                        intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                        startActivity(intent);
                                    }
                                    else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                                        Intent intent = new Intent(mContext, RecordStampsSalesWithPosActivity.class);
                                        intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                        startActivity(intent);
                                    }
                                }
                                else {
                                    Bundle data = new Bundle();
                                    data.putString(RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE, loyaltyProgram.getProgram_type());
                                    data.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                    Intent intent = new Intent(mContext, RecordDirectSalesActivity.class);
                                    intent.putExtras(data);
                                    startActivity(intent);
                                }
                            }
                            else {
                                if (isPosTurnedOn) {
                                    Intent chooseProgram = new Intent(MerchantBackOffice.this, SelectLoyaltyProgramForSales.class);
                                    startActivityForResult(chooseProgram, RECORD_SALES_WITH_POS_CHOOSE_PROGRAM);
                                }
                                else {
                                    Intent chooseProgram = new Intent(MerchantBackOffice.this, SelectLoyaltyProgramForSales.class);
                                    startActivityForResult(chooseProgram, RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM);
                                }
                            }
                        }*/
                        break;
                    case R.id.customers:
                        Intent intent = new Intent(mContext, CustomerListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.campaigns:
                        Intent loyaltyIntent = new Intent(mContext, LoyaltyProgramListActivity.class);
                        loyaltyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(loyaltyIntent);
                        break;
                }
            }
        });
        bottomNavigationBar.setOnTabReselectListener(new OnTabReselectListener() {
            @Override
            public void onTabReSelected(@IdRes int tabId) {
                if (tabId == R.id.home) {
                    if (barChart != null) {
                        barChart.highlightValues(null);
                        barChart.fitScreen();
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.merchant_back_office, menu);
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
            chartLayout.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            addGraphDataset();
        }
    };

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    private class pairObjectDateComparator implements Comparator<GraphCoordinates> {
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
}
