package co.loystar.loystarbusiness.utils.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.OrderPrintOptionsFetcher;
import co.loystar.loystarbusiness.models.pojos.OrderPrintOption;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import timber.log.Timber;

/**
 * Created by ordgen on 1/7/18.
 */

public class OrderPrintBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @BindView(R.id.order_print_rv)
    RecyclerView mRecyclerView;

    private PrintOptionsAdapter mAdapter;
    private OnPrintOptionSelectedListener mListener;

    public static OrderPrintBottomSheetDialogFragment newInstance() {
        return new OrderPrintBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.order_print_bottom_sheet_layout, container, false);
        if (getActivity() == null) {
            return view;
        }
        ButterKnife.bind(this, view);

        ArrayList<OrderPrintOption> printOptions = OrderPrintOptionsFetcher.getOrderPrintOptions(getActivity());
        mAdapter = new PrintOptionsAdapter(printOptions);
        setRecyclerView();
        return view;
    }

    private void setRecyclerView() {
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(
            getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
            getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                OrderPrintOption printOption = mAdapter.mOptions.get(position);
                if (mListener != null) {
                    mListener.onPrintOptionSelected(printOption);
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    class PrintOptionsAdapter extends RecyclerView.Adapter<PrintOptionsAdapter.ViewHolder> {

        ArrayList<OrderPrintOption> mOptions;

        PrintOptionsAdapter(ArrayList<OrderPrintOption> printOptions) {
            mOptions = printOptions;
        }

        @Override
        public PrintOptionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_print_option_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PrintOptionsAdapter.ViewHolder holder, int position) {
            holder.bind(mOptions.get(position));
        }

        @Override
        public int getItemCount() {
            return mOptions.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.optionTitle)
            TextView mTitle;

            public ViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            void bind(OrderPrintOption orderPrintOption) {
                mTitle.setText(orderPrintOption.getTitle());
            }
        }
    }

    public interface OnPrintOptionSelectedListener {
        void onPrintOptionSelected(OrderPrintOption orderPrintOption);
    }

    public void setListener(OnPrintOptionSelectedListener listener) {
        mListener = listener;
    }
}
