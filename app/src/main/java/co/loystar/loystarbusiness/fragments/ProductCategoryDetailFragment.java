package co.loystar.loystarbusiness.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;

public class ProductCategoryDetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    private ProductCategoryEntity mItem;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;

    public ProductCategoryDetailFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabaseManager = DatabaseManager.getInstance(getActivity());
        mSessionManager = new SessionManager(getActivity());

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItem = mDatabaseManager.getProductCategoryById(getArguments().getInt(ARG_ITEM_ID));

            Activity activity = this.getActivity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.productcategory_detail, container, false);

        return rootView;
    }
}
