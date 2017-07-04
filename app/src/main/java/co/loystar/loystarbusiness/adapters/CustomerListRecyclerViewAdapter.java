package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerViewOnItemClickListener;

/**
 * Created by laudbruce-tagoe on 5/25/17.
 */

public class CustomerListRecyclerViewAdapter extends RecyclerView.Adapter<CustomerListRecyclerViewAdapter.ViewHolder> {
    private ArrayList<DBCustomer> mCustomers;
    private Context mContext;
    private RecyclerViewOnItemClickListener itemClickListener;

    public CustomerListRecyclerViewAdapter(Context context, ArrayList<DBCustomer> mCustomers, RecyclerViewOnItemClickListener itemClickListener) {
        this.mCustomers = mCustomers;
        this.mContext = context;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public CustomerListRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.customer_list_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CustomerListRecyclerViewAdapter.ViewHolder holder, int position) {
        holder.bindItem(mCustomers.get(position));
    }

    @Override
    public int getItemCount() {
        return mCustomers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private View mView;
        private TextView mIdView;
        private TextView mContentView;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            mView.setOnClickListener(this);
            mIdView = (TextView) mView.findViewById(R.id.customer_list_title);
            mContentView = (TextView) mView.findViewById(R.id.customer_list_title_subtitle);
        }

        void bindItem(DBCustomer customer) {
            String fullCustomerName = customer.getFirst_name() + " " + customer.getLast_name();
            mIdView.setText(fullCustomerName);
            mContentView.setText(customer.getPhone_number());
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) {
                itemClickListener.onItemClicked(getAdapterPosition());
            }
        }
    }
}
