package co.loystar.loystarbusiness.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.GraphCoordinates;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;


public class ChartFragment extends Fragment implements OnChartValueSelectedListener {
    private BarChart barChart;
    private String merchantCurrencySymbol;
    private SessionManager mSessionManager;
    private DatabaseManager mDatabaseManager;

    public ChartFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chart, container, false);
        mSessionManager =  new SessionManager(getActivity());
        mDatabaseManager = DatabaseManager.getInstance(getActivity());
        merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(getActivity()).getCurrency(mSessionManager.getCurrency()).getSymbol();

        barChart = rootView.findViewById(R.id.chart);
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
        p.setColor(ContextCompat.getColor(getActivity(), R.color.black_overlay));

        barChart.setExtraTopOffset(chartPadding);
        barChart.setExtraBottomOffset(chartPadding);
        addGraphDataset();

        return rootView;
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    private void addGraphDataset() {
        ReactiveEntityStore<Persistable> mDataStore = DatabaseManager.getDataStore(getActivity());
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(mSessionManager.getMerchantId()))
                .get()
                .firstOrNull();
        Selection<ReactiveResult<SalesTransactionEntity>> resultSelection = mDataStore.select(SalesTransactionEntity.class);
        resultSelection.where(SalesTransactionEntity.MERCHANT.eq(merchantEntity));
        resultSelection.get().observableResult().subscribe(new Observer<ReactiveResult<SalesTransactionEntity>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ReactiveResult<SalesTransactionEntity> salesTransactionEntities) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        List<SalesTransactionEntity> salesTransactionEntities = resultSelection.get().toList();
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
            barDataSet.setColor(ContextCompat.getColor(getActivity(), R.color.colorAccentLight));

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

    @Override
    public void onPause() {
        super.onPause();
        try {
            getActivity().unregisterReceiver(salesTransactionsSyncFinishedReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(salesTransactionsSyncFinishedReceiver, new IntentFilter(Constants.SALES_TRANSACTIONS_SYNC_FINISHED));
    }

    private BroadcastReceiver salesTransactionsSyncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            addGraphDataset();
        }
    };
}
