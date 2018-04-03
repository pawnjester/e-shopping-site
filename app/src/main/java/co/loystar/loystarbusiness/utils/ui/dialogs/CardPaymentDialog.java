package co.loystar.loystarbusiness.utils.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.ui.buttons.GreenButton;

/**
 * Created by ordgen on 2/8/18.
 */

public class CardPaymentDialog extends AppCompatDialogFragment {
    private static final String TOTAL_CHARGE = "totalCharge";
    public static final String TAG = CardPaymentDialog.class.getSimpleName();

    @BindView(R.id.totalCharge)
    TextView totalChargeView;

    @BindView(R.id.confirmCardPayment)
    GreenButton confirmCardPaymentBtn;

    @BindView(R.id.includeCustomerDetail)
    CheckBox includeCustomerDetailCheckBox;

    private boolean showCustomerDialog = true;
    private CardPaymentDialogOnCompleteListener mListener;

    public static CardPaymentDialog newInstance(double totalCharge) {
        CardPaymentDialog cashPaymentDialog = new CardPaymentDialog();
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
        @SuppressLint("InflateParams") View rootView = inflater.inflate(R.layout.card_payment_dialog, null);

        ButterKnife.bind(this, rootView);

        if (getArguments() != null) {
            Double mTotalCharge = getArguments().getDouble(TOTAL_CHARGE);
            totalChargeView.setText(String.valueOf(mTotalCharge));
        }

        confirmCardPaymentBtn.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onCardPaymentDialogComplete(showCustomerDialog);
                dismiss();
            }
        });

        includeCustomerDetailCheckBox.setChecked(false);

        includeCustomerDetailCheckBox.setOnClickListener(view -> {
            CheckBox checkBox = (CheckBox) view;
            showCustomerDialog = checkBox.isChecked();
        });

        builder.setView(rootView);
        builder.setTitle(getString(R.string.card_payment));
        builder.setPositiveButton(android.R.string.no, (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        return builder.create();
    }

    public interface CardPaymentDialogOnCompleteListener {
        void onCardPaymentDialogComplete(boolean showCustomerDialog);
    }

    public void setListener(CardPaymentDialogOnCompleteListener mListener) {
        this.mListener = mListener;
    }
}
