package co.loystar.loystarbusiness.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.RecordDirectSalesActivity;
import co.loystar.loystarbusiness.events.AddPointsOnFinishEvent;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TimeUtils;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;

/**
 * Created by ordgen on 7/4/17.
 */

/*Required Intent Arguments*/
/**
 * mProgramId => loyalty program id
 * mCustomerId => customer id
 * mCustomerAmountSpent => customer amount spent
 * mTotalCustomerPoints = > total customer points
 * */

public class AddPointsFragment extends Fragment {
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private CurrencyEditText amountSpentView;
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private Long mCustomerId;
    private Long mProgramId;

    private static final String TAG = AddPointsFragment.class.getCanonicalName();


    public AddPointsFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_add_points, container, false);

        Bundle extras = getArguments();
        mProgramId = extras.getLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID);
        mCustomerId = extras.getLong(RecordDirectSalesActivity.CUSTOMER_ID);
        String mCustomerAmountSpent = extras.getString(RecordDirectSalesActivity.CUSTOMER_AMOUNT_SPENT, "");
        String mTotalCustomerPoints = extras.getString(RecordDirectSalesActivity.TOTAL_CUSTOMER_POINTS);
        final DBCustomer customer = databaseHelper.getCustomerById(mCustomerId);

        android.support.v7.app.ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            if (customer != null) {
                actionBar.setTitle(customer.getFirst_name().replace("\"", "").substring(0, 1).toUpperCase() + customer.getFirst_name().replace("\"", "").substring(1));
            }
        }

        String pointsProgramWorthTemplate = "%s Points";
        ((TextView) rootView.findViewById(R.id.fragment_add_points_total_points)).setText(String.format(pointsProgramWorthTemplate, mTotalCustomerPoints));

        amountSpentView = (CurrencyEditText) rootView.findViewById(R.id.fragment_add_points_amount_spent);
        Button addBtn = (Button) rootView.findViewById(R.id.fragment_add_points_add_points);

        amountSpentView.setText(mCustomerAmountSpent);
        amountSpentView.requestFocus();

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (amountSpentView.getText().toString().isEmpty()) {
                    amountSpentView.setError(getString(R.string.error_amount_required));
                    amountSpentView.requestFocus();
                    return;
                }
                if (amountSpentView.getRawValue() == 0) {
                    amountSpentView.setError(getString(R.string.error_amount_required_zero));
                    amountSpentView.requestFocus();
                    return;
                }

                if (customer != null) {
                    Integer amt = Integer.valueOf(amountSpentView.getFormattedValue(amountSpentView.getRawValue()));
                    DBTransaction dbTransaction = new DBTransaction();
                    dbTransaction.setAmount(amt);
                    dbTransaction.setPoints(amt);
                    dbTransaction.setUser_id(customer.getUser_id());
                    dbTransaction.setLocal_db_created_at(TimeUtils.getCurrentDateAndTime());
                    dbTransaction.setMerchant_id(sessionManager.getMerchantId());
                    dbTransaction.setMerchant_loyalty_program_id(mProgramId);
                    dbTransaction.setSynced(false);

                    databaseHelper.insertTransaction(dbTransaction);
                    int userTotalPoints = 0;
                    userTotalPoints = databaseHelper.getTotalUserPointsForProgram(customer.getUser_id(), mProgramId, sessionManager.getMerchantId());

                    Bundle bundle = new Bundle();
                    bundle.putString(RecordDirectSalesActivity.TOTAL_CUSTOMER_POINTS, String.valueOf(userTotalPoints));

                    View view = getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }

                    /*Answers.getInstance().logCustom(new CustomEvent("Points Transaction  Recorded").
                            putCustomAttribute("Program Type", getString(R.string.simple_points)).
                            putCustomAttribute("Merchant", sessionManager.getMerchantEmail()));*/

                    BusProvider.getInstance().post(new AddPointsOnFinishEvent.OnFinish(bundle));
                }

            }
        });

        return rootView;
    }

}
