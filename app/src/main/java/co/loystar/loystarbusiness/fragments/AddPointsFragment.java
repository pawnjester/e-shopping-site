package co.loystar.loystarbusiness.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.DateTime;

import java.sql.Timestamp;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;


public class AddPointsFragment extends Fragment {
    private DatabaseManager mDatabaseManager;
    private CurrencyEditText mCurrencyEditText;
    private SessionManager mSessionManager;
    private CustomerEntity mCustomer;
    private MerchantEntity merchantEntity;
    private int mProgramId;
    private int totalCustomerPoints = 0;

    private OnAddPointsFragmentInteractionListener mListener;

    public AddPointsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mSessionManager = new SessionManager(getActivity());
        int mCustomerId;
        int amountSpent = 0;
        View rootView = inflater.inflate(R.layout.fragment_add_points, container, false);

        if (getActivity() != null && getArguments() != null) {
            mDatabaseManager = DatabaseManager.getInstance(getActivity());
            merchantEntity = mDatabaseManager.getMerchant(mSessionManager.getMerchantId());

            mCustomerId = getArguments().getInt(Constants.CUSTOMER_ID, 0);
            mCustomer = mDatabaseManager.getCustomerById(mCustomerId);
            mProgramId = getArguments().getInt(Constants.LOYALTY_PROGRAM_ID, 0);
            amountSpent = getArguments().getInt(Constants.CASH_SPENT, 0);
            totalCustomerPoints = mDatabaseManager.getTotalCustomerPointsForProgram(mProgramId, mCustomerId);


            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null && mCustomer != null) {
                String title = "Add Points " + "(" + TextUtilsHelper.capitalize(mCustomer.getFirstName()) + ")";
                actionBar.setTitle(title);
            }
        }

        mCurrencyEditText = rootView.findViewById(R.id.currencyEditText);
        RxView.clicks(rootView.findViewById(R.id.addPoints)).subscribe(o -> addPoints());

        TextView totalPointsView = rootView.findViewById(R.id.total_points);
        totalPointsView.setText(getString(R.string.total_points, String.valueOf(totalCustomerPoints)));

        mCurrencyEditText.setText(String.valueOf(amountSpent));
        return rootView;
    }

    private void addPoints() {
        if (mCurrencyEditText.getText().toString().isEmpty()) {
            mCurrencyEditText.setError(getString(R.string.error_amount_required));
            mCurrencyEditText.requestFocus();
            return;
        }

        Integer amountSpent = Integer.valueOf(mCurrencyEditText.getFormattedValue(mCurrencyEditText.getRawValue()));
        SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
        Integer lastTransactionId = mDatabaseManager.getLastTransactionRecordId();

        if (lastTransactionId == null) {
            transactionEntity.setId(1);
        } else {
            transactionEntity.setId(lastTransactionId + 1);
        }
        transactionEntity.setSynced(false);
        transactionEntity.setSendSms(true);
        transactionEntity.setAmount(amountSpent);
        transactionEntity.setMerchantLoyaltyProgramId(mProgramId);
        transactionEntity.setPoints(amountSpent);
        transactionEntity.setStamps(0);
        transactionEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
        transactionEntity.setProgramType(getString(R.string.simple_points));
        if (mCustomer != null) {
            transactionEntity.setUserId(mCustomer.getUserId());
        }

        transactionEntity.setMerchant(merchantEntity);
        transactionEntity.setCustomer(mCustomer);

        mDatabaseManager.insertNewSalesTransaction(transactionEntity);
        SyncAdapter.performSync(getActivity(), mSessionManager.getEmail());

        int newTotalPoints = totalCustomerPoints + amountSpent;

        Bundle bundle = new Bundle();
        bundle.putInt(Constants.CASH_SPENT, amountSpent);
        bundle.putInt(Constants.TOTAL_CUSTOMER_POINTS, newTotalPoints);

        if (getActivity() != null) {
            View view = getActivity().getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
        mListener.onAddPointsFragmentInteraction(bundle);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAddPointsFragmentInteractionListener) {
            mListener = (OnAddPointsFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAddPointsFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnAddPointsFragmentInteractionListener {
        void onAddPointsFragmentInteraction(Bundle data);
    }
}
