package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;

/**
 * Created by ordgen on 3/22/18.
 */

public class CustomerListAdapter extends LoadMoreRecyclerViewAdapter<CustomerEntity> {
    private static final int TYPE_CUSTOMER = 1;

    public CustomerListAdapter(
        @NonNull Context context,
        ItemClickListener itemClickListener,
        @NonNull RetryLoadMoreListener retryLoadMoreListener) {
        super(context, itemClickListener, retryLoadMoreListener);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_CUSTOMER) {
            View view = mInflater.inflate(R.layout.customer_item, parent, false);
            CustomerViewHolder cv = new CustomerViewHolder(view);
            setupClickableViews(view, cv);
            return cv;
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CustomerViewHolder) {
            ((CustomerViewHolder) holder).bindView(mDataList.get(position));
        }
        super.onBindViewHolder(holder, position);
    }

    @Override
    protected int getCustomItemViewType(int position) {
        return TYPE_CUSTOMER;
    }

    class CustomerViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.customer_name)
        TextView customerName;

        @BindView(R.id.customer_phone_number)
        TextView customerNumber;

        CustomerViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindView(CustomerEntity customerEntity) {
            String lastName;
            if (customerEntity.getLastName() == null) {
                lastName = "";
            } else {
                lastName = customerEntity.getLastName();
            }
            String name = customerEntity.getFirstName() + " " + lastName;
            customerName.setText(name);

            customerNumber.setText(customerEntity.getPhoneNumber());
        }
    }

    private void setupClickableViews(final View view, final RecyclerView.ViewHolder viewHolder) {
        view.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(view, viewHolder.getAdapterPosition());
            }
        });

        view.setOnLongClickListener(view1 -> {
            if (mItemClickListener != null) {
                mItemClickListener.onLongItemClick(view, viewHolder.getAdapterPosition());
            }
            return true;
        });
    }
}
