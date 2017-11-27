package co.loystar.loystarbusiness.utils.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;

import com.jakewharton.rxbinding2.widget.RxAutoCompleteTextView;
import com.trello.rxlifecycle2.components.support.RxAppCompatDialogFragment;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.AddNewCustomerActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.utils.Constants;

/**
 * Created by ordgen on 11/27/17.
 */

public class CustomerAutoCompleteDialog extends RxAppCompatDialogFragment {
    public final static String TAG = CustomerAutoCompleteDialog.class.getSimpleName();
    public static final int ADD_NEW_CUSTOMER_REQUEST = 150;
    private static final String DIALOG_TITLE = "dialogTitle";
    private SelectedCustomerListener mSelectedCustomerListener;
    private CustomerAutoCompleteDialogAdapter mAdapter;

    public static CustomerAutoCompleteDialog newInstance(String dialogTitle) {
        CustomerAutoCompleteDialog customerAutoCompleteDialog = new CustomerAutoCompleteDialog();
        Bundle args = new Bundle();
        args.putString(DIALOG_TITLE, dialogTitle);
        customerAutoCompleteDialog.setArguments(args);
        return customerAutoCompleteDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View rootView = inflater.inflate(R.layout.customer_autocomplete_dialog, null);

        final AutoCompleteTextView autoCompleteTextView = rootView.findViewById(R.id.customerAutocomplete);
        autoCompleteTextView.setThreshold(1);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setView(rootView);

        SessionManager mSessionManager = new SessionManager(getActivity());
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(getActivity());
        ArrayList<CustomerEntity> mCustomers = new ArrayList<>(mDatabaseManager.getMerchantCustomers(mSessionManager.getMerchantId()));
        mAdapter = new CustomerAutoCompleteDialogAdapter(getActivity(), mCustomers);
        autoCompleteTextView.setAdapter(mAdapter);

        RxAutoCompleteTextView.itemClickEvents(autoCompleteTextView)
                .subscribe(adapterViewItemClickEvent -> {
                    CustomerEntity customer = (CustomerEntity) adapterViewItemClickEvent.view().getItemAtPosition(adapterViewItemClickEvent.position());
                    mAdapter.getFilter().filter("");
                    getDialog().dismiss();
                    if (mSelectedCustomerListener != null) {
                        mSelectedCustomerListener.onCustomerSelected(customer);
                    }
                });

        String dialogTitle = getArguments().getString(DIALOG_TITLE, "");
        alertDialog.setTitle(dialogTitle);

        alertDialog.setPositiveButton("Add new customer", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            Intent addCustomerIntent = new Intent(getActivity(), AddNewCustomerActivity.class);
            if (!autoCompleteTextView.getText().toString().isEmpty()) {
                String txt = autoCompleteTextView.getText().toString();
                if (TextUtilsHelper.isInteger(txt)) {
                    addCustomerIntent.putExtra(Constants.PHONE_NUMBER, txt);
                }
                else {
                    addCustomerIntent.putExtra(Constants.CUSTOMER_NAME, txt);
                }
            }
            getActivity().startActivityForResult(addCustomerIntent, ADD_NEW_CUSTOMER_REQUEST);
        });

        alertDialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            mAdapter.getFilter().filter("");

        });

        Dialog dialog =  alertDialog.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    public interface SelectedCustomerListener {
        void onCustomerSelected(@NonNull CustomerEntity customerEntity);
    }

    public void setSelectedCustomerListener(SelectedCustomerListener mSelectedCustomerListener) {
        this.mSelectedCustomerListener = mSelectedCustomerListener;
    }
}
