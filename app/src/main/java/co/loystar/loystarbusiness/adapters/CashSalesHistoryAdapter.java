package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.databinding.SalesHistoryItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import timber.log.Timber;

/**
 * Created by ordgen on 2/22/18.
 */

public class CashSalesHistoryAdapter extends QueryRecyclerAdapter<SaleEntity, BindingHolder<SalesHistoryItemBinding>> {

    private Date saleDate;
    private MerchantEntity merchantEntity;
    private Context mContext;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SessionManager mSessionManager;

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

        StringBuilder sb = new StringBuilder("");
        int i = 0;
        boolean hasProductId = false;
        for (SalesTransactionEntity transactionEntity: item.getTransactions()) {
            hasProductId = transactionEntity.getProductId() > 0;
            ProductEntity productEntity = mDataStore.findByKey(ProductEntity.class, transactionEntity.getProductId()).blockingGet();
            if (productEntity != null) {
                Double price = productEntity.getPrice();
                int count = transactionEntity.getAmount() / price.intValue();
                sb.append(productEntity.getName()).append(" (").append(count).append(")");
            }
            if (i + 1 < item.getTransactions().size() && !TextUtils.isEmpty(sb)) {
                sb.append(", ");
            }
            i++;
        }

        if (TextUtils.isEmpty(sb)) {
            if (hasProductId) {
                // this would occur if product has since been deleted
                holder.binding.productsBought.setText(mContext.getString(R.string.product_not_found));
            } else {
                // this would occur if sale is a non-pos sale
                holder.binding.productsBought.setText(mContext.getString(R.string.cash));
            }
        } else {
            holder.binding.productsBought.setText(sb.toString());
        }

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
}
