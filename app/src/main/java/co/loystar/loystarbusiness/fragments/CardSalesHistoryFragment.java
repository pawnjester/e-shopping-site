package co.loystar.loystarbusiness.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.loystar.loystarbusiness.R;

public class CardSalesHistoryFragment extends Fragment {


    public CardSalesHistoryFragment() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_sales_history, container, false);
    }

}
