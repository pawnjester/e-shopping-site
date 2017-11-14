package co.loystar.loystarbusiness.fragments;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.CustomerDetailActivity;
import co.loystar.loystarbusiness.activities.CustomerListActivity;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;

/**
 * A fragment representing a single Customer detail screen.
 * This fragment is either contained in a {@link CustomerListActivity}
 * in two-pane mode (on tablets) or a {@link CustomerDetailActivity}
 * on handsets.
 */
public class CustomerDetailFragment extends Fragment {
    private static final String TAG = CustomerDetailFragment.class.getSimpleName();
    public static final String ARG_ITEM_ID = "item_id";
    private CustomerEntity mItem;
    private boolean mTwoPane;

    public CustomerDetailFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            DatabaseManager databaseManager = DatabaseManager.getInstance(getActivity());
            mItem = databaseManager.getCustomerById(getArguments().getInt(ARG_ITEM_ID, 0));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.customer_toolbar_layout);
            if (appBarLayout != null && mItem != null) {
                String fullCustomerName = mItem.getFirstName() + " " + mItem.getLastName();
                appBarLayout.setTitle(fullCustomerName);
            }
            if (activity.findViewById(R.id.customer_detail_container) != null) {
                mTwoPane = true;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.customer_detail, container, false);
        return rootView;
    }
}
