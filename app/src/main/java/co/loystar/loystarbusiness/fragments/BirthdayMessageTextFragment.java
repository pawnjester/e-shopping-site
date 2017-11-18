package co.loystar.loystarbusiness.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.loystar.loystarbusiness.R;

public class BirthdayMessageTextFragment extends Fragment {


    public BirthdayMessageTextFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_birthday_message_text, container, false);
    }

}
