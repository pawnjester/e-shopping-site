package co.loystar.loystarbusiness.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.SalesHistoryActivity;
import co.loystar.loystarbusiness.adapters.CardSalesHistoryAdapter;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.EventBus.SalesDetailFragmentEventBus;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

/**
 * A simple {@link Fragment} subclass.
 */
public class CardSalesHistoryFragment extends Fragment implements SalesHistoryActivity.UpdateSelectedDateInterface{

    @BindView(R.id.total_card_sales)
    TextView totalCardSalesView;

    @BindView(R.id.card_sales_detail_rv)
    EmptyRecyclerView recyclerView;

    @BindView(R.id.stateImage)
    ImageView stateImage;

    @BindView(R.id.stateIntroText)
    TextView stateIntroText;

    @BindView(R.id.stateDescriptionText)
    TextView stateDescriptionText;

    @BindView(R.id.stateActionBtn)
    BrandButtonNormal stateActionBtn;

    private Date selectedDate;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SessionManager mSessionManager;
    private MerchantEntity merchantEntity;
    private CardSalesHistoryAdapter mAdapter;

    public static CardSalesHistoryFragment getInstance(Date selectedDate) {
        CardSalesHistoryFragment cardSalesHistoryFragment = new CardSalesHistoryFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.SALE_DATE, selectedDate);
        cardSalesHistoryFragment.setArguments(args);
        return cardSalesHistoryFragment;
    }

    public CardSalesHistoryFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() == null) {
            return;
        }

        mDataStore = DatabaseManager.getDataStore(getActivity());
        mSessionManager = new SessionManager(getActivity());
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        if (getArguments() != null) {
            selectedDate = (Date) getArguments().getSerializable(Constants.SALE_DATE);
        }

        mAdapter = new CardSalesHistoryAdapter(
            getActivity(),
            merchantEntity,
            selectedDate);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_card_sales_history, container, false);
        ButterKnife.bind(this, rootView);

        if (selectedDate != null) {
            setTotalSales();
        }

        stateActionBtn.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.FRAGMENT_EVENT_ID, SalesDetailFragmentEventBus.ACTION_START_SALE);
            SalesDetailFragmentEventBus
                .getInstance()
                .postFragmentAction(bundle);
        });
        return rootView;
    }

    private void setTotalSales() {
        Calendar startDayCal = Calendar.getInstance();
        startDayCal.setTime(selectedDate);

        Calendar nextDayCal = Calendar.getInstance();
        nextDayCal.setTime(selectedDate);
        nextDayCal.add(Calendar.DAY_OF_MONTH, 1);

        Selection<ReactiveResult<Tuple>> cardResultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
        cardResultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
        cardResultSelection.where(SaleEntity.PAYED_WITH_CARD.eq(true));
        cardResultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

        Tuple cardTuple = cardResultSelection.get().firstOrNull();
        double totalCardSalesForDateSelected;
        if (cardTuple == null || cardTuple.get(0) == null) {
            totalCardSalesForDateSelected = 0;
        } else {
            Double total = cardTuple.get(0);
            if (total > 0) {
                totalCardSalesForDateSelected = total;
            } else {
                totalCardSalesForDateSelected = 0;
            }
        }

        String merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(getActivity()).getCurrency(mSessionManager.getCurrency()).getSymbol();
        totalCardSalesView.setText(getString(R.string.total_sale_value, merchantCurrencySymbol, String.valueOf(totalCardSalesForDateSelected)));
        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        if (getActivity() == null) {
            return;
        }

        stateImage.setImageDrawable(AppCompatResources.getDrawable(getActivity(), R.drawable.ic_firstsale));
        stateIntroText.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateDescriptionText.setText(getString(R.string.start_sale_empty_state));
        stateActionBtn.setText(getString(R.string.start_sale_btn_label));

        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new SpacingItemDecoration(
            getResources().getDimensionPixelOffset(R.dimen.item_space_small),
            getResources().getDimensionPixelOffset(R.dimen.item_space_small))
        );
        recyclerView.setAdapter(mAdapter);

        mAdapter.queryAsync();
    }

    @Override
    public void update(Date date) {
        selectedDate = date;
        mAdapter = new CardSalesHistoryAdapter(
            getActivity(),
            merchantEntity,
            date);

        setTotalSales();
    }
}
