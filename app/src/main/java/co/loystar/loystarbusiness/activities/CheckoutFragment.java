package co.loystar.loystarbusiness.activities;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.OrderItemDividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;


/**
 * A simple {@link Fragment} subclass.
 */
public class CheckoutFragment extends Fragment {

    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    private ReactiveEntityStore<Persistable> mDataStore;
    private Context context;
    private EmptyRecyclerView mOrdersRecyclerView;
    private OrderSummaryAdapter mOrderSummaryAdapter;
    private String merchantCurrencySymbol;
    private ExecutorService executor;



    public CheckoutFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_checkout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getActivity();
        mDataStore = DatabaseManager.getDataStore(context);
        mSessionManager = new SessionManager(context);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class,
                mSessionManager.getMerchantId()).blockingGet();
        merchantCurrencySymbol = CurrenciesFetcher
                .getCurrencies(context)
                .getCurrency(mSessionManager.getCurrency())
                .getSymbol();

        mOrderSummaryAdapter = new OrderSummaryAdapter();
        executor = Executors.newSingleThreadExecutor();

    }

    private void setUpOrdersRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = getActivity().findViewById(R.id.empty_cart);
        mOrdersRecyclerView = recyclerView;
        mOrdersRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(context, 4);
        mOrdersRecyclerView.setLayoutManager(mLayoutManager);
        mOrdersRecyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mOrdersRecyclerView.addItemDecoration(new OrderItemDividerItemDecoration(context));
//        mOrdersRecyclerView.setAdapter(orderSummaryAdapter);
        mOrdersRecyclerView.setEmptyView(emptyView);
    }

    private class OrderSummaryAdapter extends
            QueryRecyclerAdapter<ProductEntity, BindingHolder<PosProductItemBinding>> implements Filterable {

        @Override
        public Filter getFilter() {
            return null;
        }

        @Override
        public Result<ProductEntity> performQuery() {
            return null;
        }

        @Override
        public void onBindViewHolder(ProductEntity item, BindingHolder<PosProductItemBinding> holder, int position) {

        }

        @NonNull
        @Override
        public BindingHolder<PosProductItemBinding> onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return null;
        }
    }
}
