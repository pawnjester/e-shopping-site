package co.loystar.loystarbusiness.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.darwindeveloper.onecalendar.clases.Day;
import com.darwindeveloper.onecalendar.views.OneCalendarView;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import timber.log.Timber;

public class CashSalesHistoryFragment extends Fragment {

    @BindView(R.id.saleDateCalendarSelect)
    OneCalendarView calendarView;

    public CashSalesHistoryFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cash_sales_history, container, false);
        ButterKnife.bind(this, rootView);

        calendarView.setOneCalendarClickListener(new OneCalendarView.OneCalendarClickListener() {
            @Override
            public void dateOnClick(Day day, int position) {
                Timber.e("DATE: %s", day);
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

        return rootView;
    }
}
