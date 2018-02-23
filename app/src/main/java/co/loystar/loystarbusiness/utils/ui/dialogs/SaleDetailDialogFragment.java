package co.loystar.loystarbusiness.utils.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.SaleDetailDialogAdapter;
import co.loystar.loystarbusiness.models.pojos.OrderSummaryItem;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;

/**
 * Created by ordgen on 2/22/18.
 */

public class SaleDetailDialogFragment extends AppCompatDialogFragment {
    public static final String TAG = SaleDetailDialogFragment.class.getSimpleName();

    @BindView(R.id.sale_detail_dialog_rv)
    RecyclerView recyclerView;

    @BindView(R.id.printReceipt)
    ImageButton printReceiptBtn;

    private SaleDetailDialogPrintListener mListener;

    public static SaleDetailDialogFragment newInstance(ArrayList<OrderSummaryItem> orderSummaryItems) {
        SaleDetailDialogFragment saleDetailDialogFragment = new SaleDetailDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.ORDER_SUMMARY_ITEMS, orderSummaryItems);
        saleDetailDialogFragment.setArguments(args);
        return saleDetailDialogFragment;
    }

    public SaleDetailDialogFragment() {}

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (getActivity() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View rootView = inflater.inflate(R.layout.sale_detail_dialog, null);

        ButterKnife.bind(this, rootView);

        if (getArguments() != null) {
            ArrayList<OrderSummaryItem> mOrderSummaryItems = (ArrayList<OrderSummaryItem>) getArguments().getSerializable(Constants.ORDER_SUMMARY_ITEMS);
            SaleDetailDialogAdapter mAdapter = new SaleDetailDialogAdapter(mOrderSummaryItems);

            recyclerView.setHasFixedSize(true);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_small),
                getResources().getDimensionPixelOffset(R.dimen.item_space_small))
            );
            recyclerView.setAdapter(mAdapter);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean bluetoothPrintEnabled = sharedPreferences.getBoolean(getString(R.string.pref_enable_bluetooth_print_key), false);

            printReceiptBtn.setVisibility(bluetoothPrintEnabled ? View.VISIBLE :View.GONE);
            printReceiptBtn.setImageDrawable(AppCompatResources.getDrawable(getActivity(), R.drawable.ic_print));

            printReceiptBtn.setOnClickListener(view -> {
                if (mListener != null) {
                    mListener.onClickPrint();
                }
            });
        }

        builder.setView(rootView);
        builder.setPositiveButton(android.R.string.no, (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        return builder.create();
    }

    public interface SaleDetailDialogPrintListener {
        void onClickPrint();
    }

    public void setListener(SaleDetailDialogPrintListener mListener) {
        this.mListener = mListener;
    }
}
