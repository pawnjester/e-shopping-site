package co.loystar.loystarbusiness.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by ordgen on 3/22/18.
 */

public class CustomerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LOADER = 1;

    private LoadMoreListener mLoadMoreListener;

    public interface LoadMoreListener {
        void onLoadMore();
    }

    private OnCustomerItemClickListener customerItemClickListener;
    private boolean mEnableLoadMore = false;
    private ArrayList<CustomerEntity> mCustomerEntities;

    public CustomerListAdapter(ArrayList<CustomerEntity> entities) {
        mCustomerEntities = entities;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.customer_name)
        TextView customerName;

        @BindView(R.id.customer_phone_number)
        TextView customerNumber;

        public ViewHolder(View itemView) {
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

    class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface OnCustomerItemClickListener {
        void onItemClick(View v, int position);
        void onLongItemClick(View v, int position);
    }

    public void setOnCustomerItemClickListener(OnCustomerItemClickListener customerItemClickListener) {
        this.customerItemClickListener = customerItemClickListener;
    }

    private void setupClickableViews(final View view, final ViewHolder viewHolder) {
        view.setOnClickListener(v -> {
            if (customerItemClickListener != null) {
                customerItemClickListener.onItemClick(view, viewHolder.getAdapterPosition());
            }
        });

        view.setOnLongClickListener(view1 -> {
            if (customerItemClickListener != null) {
                customerItemClickListener.onLongItemClick(view, viewHolder.getAdapterPosition());
            }
            return true;
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_LOADER: {
                return new LoadingViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loading_row, parent, false));
            }
            default: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.customer_item, parent, false);
                ViewHolder viewHolder = new ViewHolder(view);
                setupClickableViews(view, viewHolder);
                return viewHolder;
            }
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            ((ViewHolder)holder).bindView(mCustomerEntities.get(position));
        } else if (holder instanceof LoadingViewHolder) {
            if (mLoadMoreListener != null) {
                mLoadMoreListener.onLoadMore();
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadMorePosition(position)) {
            return VIEW_TYPE_LOADER;
        }
        return super.getItemViewType(position);
    }

    private boolean isLoadMorePosition(int position) {
        return mEnableLoadMore && position == mCustomerEntities.size();
    }

    public void setLoadMoreListener(LoadMoreListener listener) {
        mLoadMoreListener = listener;
    }

    @Override
    public int getItemCount() {
        return mCustomerEntities.size() + (mEnableLoadMore ? 1 : 0);
    }

    @Override
    public long getItemId(int position) {
        return mCustomerEntities.get(position).getId();
    }

    public void enableLoadMore(boolean enable) {
        int loadMoreIndex = mCustomerEntities.size();

        if (0 == loadMoreIndex) {
            return;
        }

        if (mEnableLoadMore == enable) {
            return;
        }

        mEnableLoadMore = enable;

        Observable.fromCallable(() -> enable).
            subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(isEnable -> {
                if (isEnable) {
                    notifyItemInserted(loadMoreIndex);
                } else {
                    notifyItemRemoved(loadMoreIndex);
                }
        });
    }

    public ArrayList<CustomerEntity> getCustomerEntities() {
        return mCustomerEntities;
    }

    public void setCustomerEntities(ArrayList<CustomerEntity> mCustomerEntities) {
        Observable.fromCallable(() -> true)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(aBoolean -> {
                this.mCustomerEntities.clear();
                notifyDataSetChanged();
                this.mCustomerEntities = mCustomerEntities;
                notifyDataSetChanged();
            });
    }
}
