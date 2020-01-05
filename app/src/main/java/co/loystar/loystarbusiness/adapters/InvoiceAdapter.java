package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;


public class InvoiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<InvoiceEntity> mInvoices;
    private OnItemClickListener mlistener;
    private OnLoadMoreListener mloadlistener;

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    boolean isLoading;
    int visibleThreshold = 5;
    int lastVisibleThreshold, totalItemCount;

    private Context mContext;

    public InvoiceAdapter(Context context,
                          ArrayList<InvoiceEntity> invoiceEntities,
                          OnItemClickListener listener,
                          OnLoadMoreListener loadMoreListener,
                          RecyclerView recyclerView) {
        mContext = context;
        mInvoices = invoiceEntities;
        mlistener = listener;
        mloadlistener = loadMoreListener;

        final LinearLayoutManager linearLayoutManager =
                (LinearLayoutManager) recyclerView.getLayoutManager();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleThreshold = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && totalItemCount <= (lastVisibleThreshold + visibleThreshold)) {
                    if (mloadlistener != null) {
                        mloadlistener.loadMore();
                    }
                        isLoading = true;
                }
            }
        });


    }

    public void set(List<InvoiceEntity> dataList) {
        List<InvoiceEntity> clone = new ArrayList<>(dataList);
        mInvoices.clear();
        mInvoices.addAll(clone);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mInvoices.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(mContext).inflate(
                    R.layout.invoice_list_item, viewGroup, false);
            return new ViewHolder(view);
        } else if(viewType == VIEW_TYPE_LOADING)  {
            View view = LayoutInflater.from(mContext).inflate(
                    R.layout.item_loading, viewGroup, false
            );
            return new LoadingViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int i) {

        if (holder instanceof ViewHolder) {
            InvoiceEntity invoice = mInvoices.get(i);
            ((ViewHolder) holder).bind(invoice, mlistener);
        } else if (holder instanceof  LoadingViewHolder) {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.mProgressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemCount() {
        return mInvoices == null ? 0 : mInvoices.size();
    }

    public void setLoading() {
        isLoading = false;
    }

    public interface OnItemClickListener {
        void onItemClick(InvoiceEntity invoice);
    }

    public interface OnLoadMoreListener {
        void loadMore();
    }

    public class LoadingViewHolder extends RecyclerView.ViewHolder {

        private ProgressBar mProgressBar;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            mProgressBar = itemView.findViewById(R.id.invoiceprogressbar);
        }

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
            String firstName;
            String amount;
            String number;
            String status;
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
            mInvoiceId.setText(number);
            mStatus.setText(status);
            mCustomer.setText(name);
            mAmount.setText(amount);
            if (entity.getCustomer() != null) {
                mCard.setOnClickListener(view -> {
                    listener.onItemClick(entity);
                });
            }
        }
    }
}