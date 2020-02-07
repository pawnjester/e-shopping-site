package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.utils.TimeUtils;


public class InvoiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<InvoiceEntity> mInvoices;
    private OnItemClickListener mlistener;


    private Context mContext;

    public InvoiceAdapter(Context context,
                          ArrayList<InvoiceEntity> invoiceEntities,
                          OnItemClickListener listener,
                          RecyclerView recyclerView) {
        mContext = context;
        mInvoices = invoiceEntities;
        mlistener = listener;
    }

    public void set(List<InvoiceEntity> dataList) {
        List<InvoiceEntity> clone = new ArrayList<>(dataList);
        mInvoices.clear();
        mInvoices.addAll(clone);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.invoice_list_item,
                parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int i) {

        if (holder instanceof ViewHolder) {
            InvoiceEntity invoice = mInvoices.get(i);
            ((ViewHolder) holder).bind(invoice, mlistener);
        }
    }

    @Override
    public int getItemCount() {
        return mInvoices == null ? 0 : mInvoices.size();
    }


    public interface OnItemClickListener {
        void onItemClick(InvoiceEntity invoice);
    }

    public class ViewHolder extends  RecyclerView.ViewHolder {

        private TextView mInvoiceId;
        private TextView mCustomer;
        private TextView mAmount;
        private TextView mStatus;
        private CardView mCard;
        private TextView mCreatedAt;
        private TextView mCreatedTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mInvoiceId = itemView.findViewById(R.id.invoice_id);
            mCustomer = itemView.findViewById(R.id.customer);
            mAmount = itemView.findViewById(R.id.amount);
            mStatus = itemView.findViewById(R.id.status);
            mCard = itemView.findViewById(R.id.invoice_card);
            mCreatedAt = itemView.findViewById(R.id.created_date_value);
            mCreatedTime = itemView.findViewById(R.id.created_time_value);
        }

        public void bind(final InvoiceEntity entity, final OnItemClickListener listener) {
            String lastName;
            String firstName;
            String amount;
            String number;
            String status;
            String createdAt;
            String createdTime;
            if (entity.getCustomer() == null) {
                lastName = "";
            } else {
                lastName = entity.getCustomer().getLastName();
            }
            if (entity.getCustomer() == null) {
                firstName = "";
            } else {
                firstName = entity.getCustomer().getFirstName();
            }
            String name = lastName + " " + firstName;
            if (entity.getAmount() == null) {
                amount = "0.0";
            } else {
                amount = entity.getAmount();
            }
            if (entity.getNumber() == null) {
                number = "";
            } else {
                number = entity.getNumber();
            }
            if (entity.getStatus() == null) {
                status = "";
            } else {
                status = entity.getStatus();
            }

            if (entity.getCreatedAt() == null) {
                createdAt = "";
                createdTime = "";
            } else {
                createdAt = TimeUtils.convertToDate(entity.getCreatedAt());
                createdTime = TimeUtils.convertToTime(entity.getCreatedAt());
            }
            mInvoiceId.setText(number);
            mStatus.setText(status);
            mCustomer.setText(name);
            mAmount.setText(amount);
            mCreatedAt.setText(createdAt);
            mCreatedTime.setText(createdTime);
            if (entity.getCustomer() != null) {
                mCard.setOnClickListener(view -> {
                    listener.onItemClick(entity);
                });
            }
        }
    }
}