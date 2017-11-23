package co.loystar.loystarbusiness.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.LoyaltyProgramListActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;

public class HomeEmptyStateFragment extends Fragment {

    private String stateType;

    public HomeEmptyStateFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        SessionManager sessionManager = new SessionManager(getActivity());
        View rootView = inflater.inflate(R.layout.fragment_home_empty_state, container, false);
        ImageView stateWelcomeImageView = rootView.findViewById(R.id.welcomeImage);
        TextView stateWelcomeTextView = rootView.findViewById(R.id.welcomeText);
        TextView stateDescriptionTextView = rootView.findViewById(R.id.stateDescriptionText);
        BrandButtonNormal stateActionBtn = rootView.findViewById(R.id.stateActionBtn);
        stateType = getArguments().getString(Constants.STATE_TYPE, "");

        if (stateType.equals(Constants.NO_LOYALTY_PROGRAM)) {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(getActivity(), R.drawable.ic_sunrise));
            stateWelcomeTextView.setText(getString(R.string.welcome_text, sessionManager.getFirstName()));
            stateDescriptionTextView.setText(getString(R.string.start_loyalty_program_empty_state));
            stateActionBtn.setText(getString(R.string.start_loyalty_program_btn_label));
        } else if (stateType.equals(Constants.NO_SALES_TRANSACTIONS)) {
            stateWelcomeImageView.setImageDrawable(AppCompatResources.getDrawable(getActivity(), R.drawable.ic_firstsale));
            stateWelcomeTextView.setText(getString(R.string.hello_text, sessionManager.getFirstName()));
            stateDescriptionTextView.setText(getString(R.string.start_sale_empty_state));
            stateActionBtn.setText(getString(R.string.start_sale_btn_label));
        }

        stateActionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stateType.equals(Constants.NO_LOYALTY_PROGRAM)) {
                    Intent intent = new Intent(getActivity(), LoyaltyProgramListActivity.class);
                    intent.putExtra(Constants.CREATE_LOYALTY_PROGRAM, true);
                    getActivity().startActivity(intent);
                } else if (stateType.equals(Constants.NO_SALES_TRANSACTIONS)) {

                }
            }
        });
        return rootView;
    }

}
