package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;


public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    private ArrayList<InvoiceEntity> mInvoices;
    private OnItemClickListener mlistener;

    private Context mContext;

    public InvoiceAdapter(Context context,
                          ArrayList<InvoiceEntity> invoiceEntities,
                          OnItemClickListener listener) {
        mContext = context;
        mInvoices = invoiceEntities;
        mlistener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(InvoiceEntity invoice);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.invoice_list_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceAdapter.ViewHolder holder, int i) {
        InvoiceEntity invoice = mInvoices.get(i);
        holder.bind(invoice, mlistener);
    }

    @Override
    public int getItemCount() {
        return mInvoices.size();
    }

    public class ViewHolder extends  RecyclerView.ViewHolder {

        private TextView mInvoiceId;
        private TextView mCustomer;
        private TextView mAmount;
        private TextView mStatus;
        private CardView mCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mInvoiceId = itemView.findViewById(R.id.invoice_id);
            mCustomer = itemView.findViewById(R.id.customer);
            mAmount = itemView.findViewById(R.id.amount);
            mStatus = itemView.findViewById(R.id.status);
            mCard = itemView.findViewById(R.id.invoice_card);
        }

        public void bind(final InvoiceEntity entity, final OnItemClickListener listener) {
            String lastName;
            if (entity.getCustomer().getLastName() == null) {
                lastName = "";
            } else {
                lastName = entity.getCustomer().getLastName();
            }
            String name = entity.getCustomer().getFirstName() + " " + lastName;
            mAmount.setText(entity.getPaidAmount());
            mInvoiceId.setText(entity.getNumber());
            mStatus.setText(entity.getStatus());
            mCustomer.setText(name);
            mCard.setOnClickListener(view -> {
                listener.onItemClick(entity);
            });
        }
    }
}