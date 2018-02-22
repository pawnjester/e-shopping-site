package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.databinding.SalesHistoryItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.models.pojos.OrderSummaryItem;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.dialogs.SaleDetailDialogFragment;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

/**
 * Created by ordgen on 2/22/18.
 */

public class CashSalesHistoryAdapter extends
    QueryRecyclerAdapter<SaleEntity, BindingHolder<SalesHistoryItemBinding>>
    implements SaleDetailDialogFragment.SaleDetailDialogPrintListener {

    private Date saleDate;
    private MerchantEntity merchantEntity;
    private Context mContext;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SessionManager mSessionManager;
    private CashSalesHistoryAdapterPrintListener mListener;
    private ArrayList<OrderSummaryItem> mOrderSummaryItems = new ArrayList<>();

    public CashSalesHistoryAdapter(
        Context context,
        MerchantEntity merchantEntity,
        Date saleDate
    ) {
        super(SaleEntity.$TYPE);

        mContext = context;
        this.merchantEntity = merchantEntity;
        this.saleDate = saleDate;
        mSessionManager = new SessionManager(mContext);
        mDataStore = DatabaseManager.getDataStore(context);
    }

    @Override
    public Result<SaleEntity> performQuery() {
        Calendar startDayCal = Calendar.getInstance();
        startDayCal.setTime(saleDate);

        Calendar nextDayCal = Calendar.getInstance();
        nextDayCal.setTime(saleDate);
        nextDayCal.add(Calendar.DAY_OF_MONTH, 1);

        Selection<ReactiveResult<SaleEntity>> resultSelection = mDataStore.select(SaleEntity.class);
        resultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
        resultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));
        resultSelection.where(SaleEntity.PAYED_WITH_CASH.eq(true));
        return resultSelection.orderBy(SaleEntity.CREATED_AT.desc()).get();
    }

    @Override
    public void onBindViewHolder(SaleEntity item, BindingHolder<SalesHistoryItemBinding> holder, int position) {
        holder.binding.setSale(item);
        holder.binding.getRoot().setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        );

        String customerName;
        if (item.getCustomer() == null) {
            customerName = mContext.getString(R.string.guest_customer);
            holder.binding.customerNameLabel.setText(mContext.getString(R.string.guest_customer));
        } else {
            CustomerEntity customerEntity = item.getCustomer();
            String lastName = customerEntity.getLastName();

            if (TextUtils.isEmpty(lastName)) {
                lastName = "";
            } else {
                lastName = " " + TextUtilsHelper.capitalize(lastName);
            }

            customerName = TextUtilsHelper.capitalize(customerEntity.getFirstName()) + lastName;
        }
        holder.binding.customerNameLabel.setText(customerName);

        holder.binding.itemWrapper.setOnClickListener(view -> {
            ArrayList<OrderSummaryItem> orderSummaryItems = new ArrayList<>();
            for (SalesTransactionEntity transactionEntity: item.getTransactions()) {
                ProductEntity productEntity = mDataStore.findByKey(ProductEntity.class, transactionEntity.getProductId()).blockingGet();
                if (productEntity != null) {
                    Double price = productEntity.getPrice();
                    int count = Double.valueOf(transactionEntity.getAmount()).intValue() / price.intValue();
                    orderSummaryItems.add(new OrderSummaryItem(productEntity.getName(), count, price, transactionEntity.getAmount()));
                }
            }
            if (!orderSummaryItems.isEmpty()) {
                mOrderSummaryItems = orderSummaryItems;
                SaleDetailDialogFragment dialogFragment = SaleDetailDialogFragment.newInstance(orderSummaryItems);
                dialogFragment.setListener(CashSalesHistoryAdapter.this);
                dialogFragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), SaleDetailDialogFragment.TAG);
            }
        });

        String merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(mContext).getCurrency(mSessionManager.getCurrency()).getSymbol();
        holder.binding.totalSales.setText(mContext.getString(R.string.total_sale_value, merchantCurrencySymbol, String.valueOf(item.getTotal())));
    }

    @Override
    public BindingHolder<SalesHistoryItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        SalesHistoryItemBinding binding = SalesHistoryItemBinding.inflate(inflater);
        binding.getRoot().setTag(binding);
        return new BindingHolder<>(binding);
    }

    @Override
    public void onClickPrint() {
        if(mListener != null) {
            mListener.onPrintClick(mOrderSummaryItems);
        }
    }

    public interface CashSalesHistoryAdapterPrintListener {
        void onPrintClick(ArrayList<OrderSummaryItem> orderSummaryItems);
    }

    public void setListener(CashSalesHistoryAdapterPrintListener mListener) {
        this.mListener = mListener;
    }
}
