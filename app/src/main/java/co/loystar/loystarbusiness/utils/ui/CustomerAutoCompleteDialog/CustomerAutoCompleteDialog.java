package co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;

import java.io.Serializable;
import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.AddNewCustomerActivity;
import co.loystar.loystarbusiness.activities.RecordDirectSalesActivity;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;

import static android.app.Activity.RESULT_OK;

/**
 * Created by laudbruce-tagoe on 3/26/17.
 */

public class CustomerAutoCompleteDialog extends DialogFragment {

    private SelectedCustomerListener mSelectedCustomerListener;
    private DBCustomer mSelectedCustomer;
    private static final String DIALOG_TITLE = "dialogTitle";
    private static final String USERS = "users";
    public static final int ADD_NEW_CUSTOMER_REQUEST = 150;
    private FragmentActivity activity;
    private CustomerAutoCompleteDialogAdapter autoCompleteDialogAdapter;
    public static final String TAG = CustomerAutoCompleteDialog.class.getSimpleName();

    public static CustomerAutoCompleteDialog newInstance(String dialogTitle, ArrayList<DBCustomer> customers) {
        CustomerAutoCompleteDialog customerAutoCompleteDialog = new CustomerAutoCompleteDialog();
        Bundle args = new Bundle();
        args.putString(DIALOG_TITLE, dialogTitle);
        args.putSerializable(USERS, customers);
        customerAutoCompleteDialog.setArguments(args);
        return customerAutoCompleteDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        // Prevent crash on orientation change by retrieving mSelectedCustomerListener instance
        if (null != savedInstanceState) {
            mSelectedCustomerListener = (SelectedCustomerListener) savedInstanceState.getSerializable("customer");
        }

        View rootView = inflater.inflate(R.layout.customer_autocomplete_layout, null);

        final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) rootView.findViewById(R.id.customerAutocomplete);
        autoCompleteTextView.setThreshold(1);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setView(rootView);

        Bundle args = getArguments();
        if (args != null) {
            String dialogTitle = args.getString(DIALOG_TITLE);
            @SuppressWarnings("unchecked")
            ArrayList<DBCustomer> customers = (ArrayList<DBCustomer>) args.getSerializable(USERS);
            autoCompleteDialogAdapter = new CustomerAutoCompleteDialogAdapter(getActivity(), customers);
            autoCompleteTextView.setAdapter(autoCompleteDialogAdapter);

            autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    mSelectedCustomer = (DBCustomer) adapterView.getItemAtPosition(i);
                    autoCompleteDialogAdapter.getFilter().filter("");
                    getDialog().dismiss();
                    mSelectedCustomerListener.onCustomerSelected(mSelectedCustomer);
                }
            });

            alertDialog.setTitle(dialogTitle);
        }

        alertDialog.setPositiveButton("Add new customer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                Intent addCustomerIntent = new Intent(activity, AddNewCustomerActivity.class);
                if (!autoCompleteTextView.getText().toString().isEmpty()) {
                    String txt = autoCompleteTextView.getText().toString();
                    if (TextUtilsHelper.isInteger(txt)) {
                        addCustomerIntent.putExtra(RecordDirectSalesActivity.CUSTOMER_PHONE_NUMBER, txt);
                    }
                    else {
                        addCustomerIntent.putExtra(AddNewCustomerActivity.CUSTOMER_NAME, txt);
                    }
                }
                getActivity().startActivityForResult(addCustomerIntent, ADD_NEW_CUSTOMER_REQUEST);
            }
        });

        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                autoCompleteDialogAdapter.getFilter().filter("");

            }
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity = getActivity();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("customer", mSelectedCustomerListener);
        super.onSaveInstanceState(outState);
    }

    public void SetSelectedCustomerListener(SelectedCustomerListener selectedCustomerListener) {
        mSelectedCustomerListener = selectedCustomerListener;
    }

    public interface SelectedCustomerListener<T> extends Serializable {
        void onCustomerSelected(DBCustomer user);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_NEW_CUSTOMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data.hasExtra(AddNewCustomerActivity.CUSTOMER_ID)) {
                    DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
                    mSelectedCustomer = databaseHelper.getCustomerById(data.getLongExtra(AddNewCustomerActivity.CUSTOMER_ID, 0));
                    mSelectedCustomerListener.onCustomerSelected(mSelectedCustomer);
                }
            }
        }
    }

}
