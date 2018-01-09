package co.loystar.loystarbusiness.utils.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.OrderPrintOptionsFetcher;
import co.loystar.loystarbusiness.models.pojos.OrderPrintOption;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;

/**
 * Created by ordgen on 1/7/18.
 */

public class OrderPrintOptionsDialogFragment extends AppCompatDialogFragment {

    private OnPrintOptionSelectedListener mListener;

    public static OrderPrintOptionsDialogFragment newInstance() {
        return new OrderPrintOptionsDialogFragment();
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

        builder.setTitle(getString(R.string.print_for));

        ArrayList<OrderPrintOption> printOptions = OrderPrintOptionsFetcher.getOrderPrintOptions(getActivity());
        List<CharSequence> entries = new ArrayList<>();
        for (OrderPrintOption printOption: printOptions) {
            entries.add(printOption.getTitle());
        }
        PrintOptionsAdapter printOptionsAdapter = new PrintOptionsAdapter(getActivity(), R.layout.checked_view, entries);
        builder.setSingleChoiceItems(printOptionsAdapter, -1, (dialogInterface, i) -> {
            OrderPrintOption printOption = printOptions.get(i);
            if (mListener != null) {
                mListener.onPrintOptionSelected(printOption);
                dialogInterface.dismiss();
            }
        });

        return builder.create();
    }

    private class PrintOptionsAdapter extends ArrayAdapter<CharSequence> {
        private List<CharSequence> mEntries;
        private Context mContext;

        PrintOptionsAdapter(@NonNull Context context, @LayoutRes int resource, List<CharSequence> entries) {
            super(context, resource);
            mEntries = entries;
            mContext = context;
        }

        ViewHolder holder;

        class ViewHolder {
            CheckedTextView checkedTextView;
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Nullable
        @Override
        public String getItem(int position) {
            return mEntries.get(position).toString();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.checked_view, parent, false);;

                holder = new ViewHolder();
                holder.checkedTextView = convertView.findViewById(R.id.checked_list_item);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.checkedTextView.setText(mEntries.get(position));
            return convertView;
        }
    }

    public interface OnPrintOptionSelectedListener {
        void onPrintOptionSelected(OrderPrintOption orderPrintOption);
    }

    public void setListener(OnPrintOptionSelectedListener listener) {
        mListener = listener;
    }
}
