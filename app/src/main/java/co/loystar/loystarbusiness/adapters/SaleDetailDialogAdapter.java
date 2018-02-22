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
import co.loystar.loystarbusiness.models.pojos.OrderSummaryItem;
import timber.log.Timber;

/**
 * Created by ordgen on 2/22/18.
 */

public class SaleDetailDialogAdapter extends RecyclerView.Adapter<SaleDetailDialogAdapter.ViewHolder> {
    private ArrayList<OrderSummaryItem> mOrderSummaryItems;

    public SaleDetailDialogAdapter(ArrayList<OrderSummaryItem> orderSummaryItems) {
        mOrderSummaryItems = orderSummaryItems;
    }

    @Override
    public SaleDetailDialogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sale_detail_dialog_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SaleDetailDialogAdapter.ViewHolder holder, int position) {
        holder.bind(mOrderSummaryItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mOrderSummaryItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.productName)
        TextView productNameView;

        @BindView(R.id.total)
        TextView totalView;

        @BindView(R.id.description)
        TextView descriptionView;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        void bind(OrderSummaryItem orderSummaryItem) {
            productNameView.setText(orderSummaryItem.getName());
            totalView.setText(String.valueOf(orderSummaryItem.getTotal()));

            String descriptionText = orderSummaryItem.getCount() + " x " + orderSummaryItem.getPrice();
            descriptionView.setText(descriptionText);
        }
    }
}
