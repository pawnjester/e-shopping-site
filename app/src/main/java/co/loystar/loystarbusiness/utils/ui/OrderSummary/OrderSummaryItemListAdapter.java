package co.loystar.loystarbusiness.utils.ui.OrderSummary;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.GeneralUtils;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.IncrementDecrementButton;
import co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput.CurrenciesFetcher;

/**
 * Created by laudbruce-tagoe on 3/25/17.
 */

public class OrderSummaryItemListAdapter extends RecyclerView.Adapter<OrderSummaryItemListAdapter.ViewHolder> {

    private ArrayList<OrderSummaryItem> items;
    private Context context;
    private SessionManager sessionManager;
    private OrderSummaryItemUpdateListener orderSummaryItemUpdateListener;
    public OrderSummaryItemListAdapter(Context context, ArrayList<OrderSummaryItem> items,
                                       OrderSummaryItemUpdateListener orderSummaryItemUpdateListener) {
        this.items = items;
        this.context = context;
        this.orderSummaryItemUpdateListener = orderSummaryItemUpdateListener;
        sessionManager = new SessionManager(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_summary_items_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindItem(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView itemName;
        private TextView totalItemPrice;
        private IncrementDecrementButton incrementDecrementButton;
        private int count;
        private double totalAmountCollectable;

        public ViewHolder(View itemView) {
            super(itemView);

            itemName = (TextView) itemView.findViewById(R.id.item_name);
            totalItemPrice = (TextView) itemView.findViewById(R.id.totalItemPrice);
            incrementDecrementButton = (IncrementDecrementButton) itemView.findViewById(R.id.actionsBtn);
        }

        void bindItem(final OrderSummaryItem item) {
            final String currencySymbol = CurrenciesFetcher.getCurrencies(context).getCurrency(sessionManager.getMerchantCurrency()).getSymbol();

            count = item.getItemCount();

            final String sTemplate = "%s %.2f";
            final String pTemplate = "%.2f";

            totalAmountCollectable = count * item.getItemPrice();
            itemName.setText(item.getItemName());
            String priceText = String.format(Locale.UK, pTemplate, totalAmountCollectable);
            totalItemPrice.setText(String.format(Locale.UK, sTemplate, currencySymbol, totalAmountCollectable));

            if (priceText.length() > 7 && !GeneralUtils.isXLargeTablet(context) && !GeneralUtils.isSmallTablet(context)) {
                totalItemPrice.setTextSize(12);
            }

            incrementDecrementButton.setNumber(String.valueOf(item.getItemCount()));

            incrementDecrementButton.setOnValueChangeListener(new IncrementDecrementButton.OnValueChangeListener() {
                @Override
                public void onValueChange(IncrementDecrementButton view, int oldValue, int newValue) {
                    orderSummaryItemUpdateListener.getOrderSummaryItemUpdate(item.getItemId(), newValue);

                }
            });
        }
    }

    private static boolean isNegative(double d) {
        return Double.doubleToRawLongBits(d) < 0;
    }

    public static double getDoubleValue(double amt) {
        double val = 0.00;
        if (!isNegative(amt)) {
            val = amt;
        }
        return val;
    }

    public interface OrderSummaryItemUpdateListener {
        void getOrderSummaryItemUpdate(Long itemId, int newCount);
    }

    public void clear() {
        int size = items.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                items.remove(0);
            }

            this.notifyItemRangeRemoved(0, size);
        }
    }

}
