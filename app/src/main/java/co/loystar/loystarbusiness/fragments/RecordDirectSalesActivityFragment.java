package co.loystar.loystarbusiness.fragments;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.RecordDirectSalesActivity;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.RecordDirectSalesActivityFragmentEvent;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog.CustomerAutoCompleteDialogAdapter;

/**
 * Created by ordgen on 7/4/17.
 */

/** Bundle Extras
 * Long mCustomerId => customer Id (optional)
 * */

public class RecordDirectSalesActivityFragment extends Fragment {
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private Long mCustomerId;
    private ArrayList<DBCustomer> mCustomers;
    private SessionManager sessionManager;

    public RecordDirectSalesActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_record_direct_sales, container, false);

        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        Bundle extras = getArguments();
        mCustomerId = extras.getLong(RecordDirectSalesActivity.CUSTOMER_ID, 0L);
        sessionManager = new SessionManager(getActivity());

        final CurrencyEditText amountSpentView = (CurrencyEditText) rootView.findViewById(R.id.record_direct_sales_amount_spent);
        BrandButtonNormal submitBtn = (BrandButtonNormal) rootView.findViewById(R.id.fragment_next_btn);

        mCustomers = databaseHelper.listMerchantCustomers(sessionManager.getMerchantId());
        final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) rootView.findViewById(
                R.id.record_direct_sales_customer_autocomplete);
        autoCompleteTextView.setThreshold(1);
        CustomerAutoCompleteDialogAdapter autoCompleteDialogAdapter = new CustomerAutoCompleteDialogAdapter(getActivity(),
                mCustomers);
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

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCustomerId == 0) {
                    autoCompleteTextView.setError(getString(R.string.error_select_customer));
                    autoCompleteTextView.requestFocus();
                    return;
                }
                if (amountSpentView.getText().toString().isEmpty()) {
                    amountSpentView.setError(getString(R.string.error_amount_required));
                    amountSpentView.requestFocus();
                    return;
                }
                if (amountSpentView.getRawValue() == 0 || amountSpentView.getText().toString().equals("0")) {
                    amountSpentView.setError(getString(R.string.error_amount_required_zero));
                    amountSpentView.requestFocus();
                    return;
                }

                Bundle extras = new Bundle();
                extras.putLong(RecordDirectSalesActivity.CUSTOMER_ID, mCustomerId);
                extras.putString(RecordDirectSalesActivity.CUSTOMER_AMOUNT_SPENT, amountSpentView.getFormattedValue(amountSpentView.getRawValue()));
                BusProvider.getInstance().post(new RecordDirectSalesActivityFragmentEvent.OnFinish(extras));
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCustomers = databaseHelper.listMerchantCustomers(sessionManager.getMerchantId());
    }
}
