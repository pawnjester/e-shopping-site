package co.loystar.loystarbusiness.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialogAdapter;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;

public class SaleWithoutPosFragment extends Fragment {
    private static final String TAG = SaleWithoutPosFragment.class.getSimpleName();
    private CustomerEntity mSelectedCustomer;
    private int mCustomerId;
    private CurrencyEditText mCurrencyEditText;
    private AutoCompleteTextView mAutoCompleteTextView;
    private OnSaleWithoutPosFragmentInteractionListener mListener;

    public SaleWithoutPosFragment() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sale_without_pos, container, false);
        SessionManager mSessionManager = new SessionManager(getActivity());
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(getActivity());
        if (getArguments() != null) {
            mCustomerId = getArguments().getInt(Constants.CUSTOMER_ID, 0);
        }
        List<CustomerEntity> mCustomers = mDatabaseManager.getMerchantCustomers(mSessionManager.getMerchantId());
        mSelectedCustomer = mDatabaseManager.getCustomerById(mCustomerId);

        mAutoCompleteTextView = rootView.findViewById(R.id.record_direct_sales_customer_autocomplete);
        mAutoCompleteTextView.setThreshold(1);
        ArrayList<CustomerEntity> customerEntities = new ArrayList<>();
        customerEntities.addAll(mCustomers);
        CustomerAutoCompleteDialogAdapter autoCompleteDialogAdapter = new CustomerAutoCompleteDialogAdapter(getContext(), customerEntities);
        mAutoCompleteTextView.setAdapter(autoCompleteDialogAdapter);

        if (mSelectedCustomer != null) {
            mAutoCompleteTextView.setText(mSelectedCustomer.getFirstName());
        }

        RxAutoCompleteTextView.itemClickEvents(mAutoCompleteTextView).subscribe(adapterViewItemClickEvent -> {
            mSelectedCustomer = (CustomerEntity) adapterViewItemClickEvent.view().getItemAtPosition(adapterViewItemClickEvent.position());
            mAutoCompleteTextView.setText(mSelectedCustomer.getFirstName());
        });

        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
        mCurrencyEditText = rootView.findViewById(R.id.record_direct_sales_amount_spent);
        BrandButtonNormal submitBtn = rootView.findViewById(R.id.sale_without_pos_continue_btn);

        RxView.clicks(submitBtn).subscribe(o -> {
            if (mSelectedCustomer == null) {
                mAutoCompleteTextView.setError(getString(R.string.error_select_customer));
                mAutoCompleteTextView.requestFocus();
                return;
            }
            if (mCurrencyEditText.getText().toString().isEmpty()) {
                mCurrencyEditText.setError(getString(R.string.error_amount_required));
                mCurrencyEditText.requestFocus();
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putInt(Constants.CUSTOMER_ID, mSelectedCustomer.getId());
            bundle.putString(
                    Constants.CASH_SPENT,
                    mCurrencyEditText.getFormattedValue(mCurrencyEditText.getRawValue()));
            mListener.onSaleWithoutPosFragmentInteraction(bundle);
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSaleWithoutPosFragmentInteractionListener) {
            mListener = (OnSaleWithoutPosFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSaleWithoutPosFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnSaleWithoutPosFragmentInteractionListener {
        void onSaleWithoutPosFragmentInteraction(Bundle data);
    }
}
