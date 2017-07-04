package co.loystar.loystarbusiness.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.RecordDirectSalesActivity;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog.CustomerAutoCompleteDialogAdapter;

/**
 * Created by ordgen on 7/4/17.
 */


/**Bundle extras
 * Long mCustomerId => preselected customer Id (optional)
 * */
public class OrderBelongsToFragment extends Fragment {
    private OnOrderBelongsToFragmentInteractionListener mListener;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private Long mCustomerId;
    public OrderBelongsToFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_order_belong_to, container, false);

        SessionManager sessionManager = new SessionManager(getActivity());
        mCustomerId = getArguments().getLong(RecordDirectSalesActivity.CUSTOMER_ID, 0L);

        final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) rootView.findViewById(
                R.id.fragment_order_belongs_to_customer_autocomplete);
        autoCompleteTextView.setThreshold(3);
        CustomerAutoCompleteDialogAdapter autoCompleteDialogAdapter = new CustomerAutoCompleteDialogAdapter(getActivity(),
                databaseHelper.listMerchantCustomers(sessionManager.getMerchantId()));
        autoCompleteTextView.setAdapter(autoCompleteDialogAdapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                DBCustomer customer = (DBCustomer) adapterView.getItemAtPosition(i);
                if (customer != null) {
                    mCustomerId = customer.getId();
                    autoCompleteTextView.setText(customer.getFirst_name());
                }
            }
        });

        DBCustomer customer = databaseHelper.getCustomerById(mCustomerId);
        if (customer != null) {
            autoCompleteTextView.setText(customer.getFirst_name());
        }

        BrandButtonNormal submitBtn = (BrandButtonNormal) rootView.findViewById(R.id.fragment_order_belong_to_next_btn);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mCustomerId == 0) {
                    autoCompleteTextView.setError(getString(R.string.error_select_customer));
                    autoCompleteTextView.requestFocus();
                    return;
                }
                mListener.onOrderBelongsFragmentInteraction(mCustomerId);
            }
        });

        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnOrderBelongsToFragmentInteractionListener) {
            mListener = (OnOrderBelongsToFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnOrderBelongsToFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnOrderBelongsToFragmentInteractionListener {
        void onOrderBelongsFragmentInteraction(Long  customerId);
    }
}
