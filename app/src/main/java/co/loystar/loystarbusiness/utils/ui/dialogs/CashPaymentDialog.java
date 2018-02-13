package co.loystar.loystarbusiness.utils.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.ui.buttons.GreenButton;

/**
 * Created by ordgen on 2/6/18.
 */

public class CashPaymentDialog extends AppCompatDialogFragment {
    private static final String TOTAL_CHARGE = "totalCharge";
    public static final String TAG = CashPaymentDialog.class.getSimpleName();

    @BindView(R.id.totalCharge)
    TextView totalChargeView;

    @BindView(R.id.cash_collected_input)
    EditText cashCollectedInput;

    @BindView(R.id.cashChange)
    TextView cashChangeView;

    @BindView(R.id.completePayment)
    GreenButton completePaymentBtn;

    @BindView(R.id.includeCustomerDetail)
    CheckBox includeCustomerDetailCheckBox;

    private Double mTotalCharge;
    private boolean showCustomerDialog = true;
    private CashPaymentDialogOnCompleteListener mListener;

    public static CashPaymentDialog newInstance(double totalCharge) {
        CashPaymentDialog cashPaymentDialog = new CashPaymentDialog();
        Bundle args = new Bundle();
        args.putDouble(TOTAL_CHARGE, totalCharge);
        cashPaymentDialog.setArguments(args);
        return cashPaymentDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View rootView = inflater.inflate(R.layout.cash_payment_dialog, null);

        ButterKnife.bind(this, rootView);

        if (getArguments() != null) {
            mTotalCharge = getArguments().getDouble(TOTAL_CHARGE);
            totalChargeView.setText(String.valueOf(mTotalCharge));
        }

        RxTextView.textChangeEvents(cashCollectedInput).subscribe(textViewTextChangeEvent -> {
            CharSequence s = textViewTextChangeEvent.text();
            if (TextUtils.isEmpty(s)) {
                cashChangeView.setText(null);
            } else {
                int cashCollected = Integer.parseInt(String.valueOf(s));
                if (mTotalCharge != null) {
                    double c = cashCollected - mTotalCharge;
                    String template = "%.2f";
                    double change = Double.valueOf(String.format(Locale.UK, template, c));
                    cashChangeView.setText(String.valueOf(change));
                }
            }
        });

        completePaymentBtn.setOnClickListener(view -> {
            if (TextUtils.isEmpty(cashCollectedInput.getText().toString())) {
                Toast.makeText(getActivity(), getString(R.string.error_cash_collected_required), Toast.LENGTH_LONG).show();
                return;
            }
            if (mListener != null) {
                mListener.onCashPaymentDialogComplete(showCustomerDialog);
                dismiss();
            }
        });

        includeCustomerDetailCheckBox.setChecked(true);

        includeCustomerDetailCheckBox.setOnClickListener(view -> {
            CheckBox checkBox = (CheckBox) view;
            showCustomerDialog = checkBox.isChecked();
        });

        builder.setView(rootView);
        builder.setTitle(getString(R.string.cash_payment));
        builder.setPositiveButton(android.R.string.no, (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        return builder.create();
    }

    public interface CashPaymentDialogOnCompleteListener {
        void onCashPaymentDialogComplete(boolean showCustomerDialog);
    }

    public void setListener(CashPaymentDialogOnCompleteListener mListener) {
        this.mListener = mListener;
    }
}
