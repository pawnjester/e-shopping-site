package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.CustomerListActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.databinding.SalesHistoryItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import timber.log.Timber;

/**
 * Created by ordgen on 2/10/18.
 */

public class SalesHistoryAdapter extends QueryRecyclerAdapter<SaleEntity, BindingHolder<SalesHistoryItemBinding>> {

    private Date saleDate;
    private MerchantEntity merchantEntity;
    private String typeOfSale;
    private Context mContext;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SessionManager mSessionManager;

    public SalesHistoryAdapter(
        Context context,
        MerchantEntity merchantEntity,
        Date saleDate,
        String typeOfSale
    ) {
        super(SaleEntity.$TYPE);

        mContext = context;
        this.merchantEntity = merchantEntity;
        this.saleDate = saleDate;
        this.typeOfSale = typeOfSale;
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

        Selection<ReactiveResult<SaleEntity>> resultSelection;

        if (typeOfSale.equals(Constants.CASH_SALE)) {
            resultSelection = mDataStore.select(SaleEntity.class);
            resultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
            resultSelection.where(SaleEntity.PAYED_WITH_CASH.eq(true));
            resultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

            return resultSelection.orderBy(SaleEntity.CREATED_AT.desc()).get();
        } else if (typeOfSale.equals(Constants.CARD_SALE)) {
            resultSelection = mDataStore.select(SaleEntity.class);
            resultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
            resultSelection.where(SaleEntity.PAYED_WITH_CARD.eq(true));
            resultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

            return resultSelection.orderBy(SaleEntity.CREATED_AT.desc()).get();
        }
        return null;
    }

    @Override
    public void onBindViewHolder(SaleEntity item, BindingHolder<SalesHistoryItemBinding> holder, int position) {
      holder.binding.setSale(item);
        holder.binding.getRoot().setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        );

      if (item.getCustomer() == null) {
          holder.binding.customerNameLabel.setText(mContext.getString(R.string.guest_customer));
      } else {
          CustomerEntity customerEntity = item.getCustomer();
          String lastName = customerEntity.getLastName();

          if (TextUtils.isEmpty(lastName)) {
              lastName = "";
          } else {
              lastName = " " + TextUtilsHelper.capitalize(lastName);
          }

          String customerName = TextUtilsHelper.capitalize(customerEntity.getFirstName()) + lastName;
          holder.binding.customerNameLabel.setText(customerName);

          holder.binding.customerNameLabel.setOnClickListener(view -> {
              Intent customerDetailIntent = new Intent(mContext, CustomerListActivity.class);
              customerDetailIntent.putExtra(Constants.CUSTOMER_ID, customerEntity.getId());
              mContext.startActivity(customerDetailIntent);
          });
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
