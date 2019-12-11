package co.loystar.loystarbusiness.utils.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 2/6/18.
 */

public class PayOptionsDialog extends AppCompatDialogFragment {

    public static final String TAG = PayOptionsDialog.class.getSimpleName();
    @BindView(R.id.payWithCash)
    ImageButton payWithCashView;

    @BindView(R.id.payWithCard)
    ImageButton payWithCardView;

    @BindView(R.id.payWithInvoice)
    ImageButton payWithInvoice;

    private PayOptionsDialogClickListener mListener;

    public static PayOptionsDialog newInstance() {
        return new PayOptionsDialog();
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

        builder.setTitle(getString(R.string.pay_with));
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View rootView = inflater.inflate(R.layout.pay_options_dialog, null);

        ButterKnife.bind(this, rootView);
        payWithCashView.setImageDrawable(AppCompatResources.getDrawable(getActivity(), R.drawable.ic_cash));
        payWithCardView.setImageDrawable(AppCompatResources.getDrawable(getActivity(), R.drawable.ic_credit_card));
        payWithInvoice.setImageDrawable(AppCompatResources.getDrawable(getActivity(), R.drawable.ic_invoice));

        payWithCashView.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onPayWithCashClick();
                dismiss();
            }
        });

        payWithCardView.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onPayWithCardClick();
                dismiss();
            }
        });

        payWithInvoice.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onPayWithInvoice();;
                dismiss();;
            }
        });

        builder.setView(rootView);
        builder.setPositiveButton(android.R.string.no, (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        return builder.create();
    }

    public interface PayOptionsDialogClickListener {
        void onPayWithCashClick();
        void onPayWithCardClick();
        void onPayWithInvoice();
    }

    public void setListener(PayOptionsDialogClickListener mListener) {
        this.mListener = mListener;
    }
}
