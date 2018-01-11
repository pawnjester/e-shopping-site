package co.loystar.loystarbusiness.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.SalesOrderListActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.OrderItemEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesOrderEntity;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.MyAlertDialog;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.OrderItemDividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;
import timber.log.Timber;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;


public class SalesOrderDetailFragment extends Fragment{
    public static final String ARG_ITEM_ID = "item_id";
    private SalesOrderEntity mItem;
    private ArrayList<OrderItemEntity> orderItems = new ArrayList<>();
    private SessionManager mSessionManager;
    private View processOrderViewWrapper;
    private MyAlertDialog myAlertDialog;
    private ReactiveEntityStore<Persistable> mDataStore;

    public SalesOrderDetailFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() == null) {
            return;
        }

        mSessionManager = new SessionManager(getActivity());
        mDataStore = DatabaseManager.getDataStore(getActivity());
        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {

            DatabaseManager databaseManager = DatabaseManager.getInstance(getActivity());
            mItem = databaseManager.getSalesOrderById(getArguments().getInt(ARG_ITEM_ID, 0));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.sales_order_detail, container, false);
        if (getActivity() == null) {
            return rootView;
        }
        processOrderViewWrapper = rootView.findViewById(R.id.process_order_action_buttons_wrapper);
        if (mItem != null) {
            orderItems.addAll(mItem.getOrderItems());
            if (mItem.getStatus().equals(getString(R.string.pending))) {
                processOrderViewWrapper.setVisibility(View.VISIBLE);
            }
        }

        myAlertDialog = new MyAlertDialog();
        OrderItemsAdapter mAdapter = new OrderItemsAdapter(orderItems);
        RecyclerView mRecyclerView = rootView.findViewById(R.id.order_items_rv);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(
            new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.addItemDecoration(new OrderItemDividerItemDecoration(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

        RxView.clicks(rootView.findViewById(R.id.reject_order_btn)).subscribe(o -> {
            myAlertDialog.setTitle("Are you sure?");
            myAlertDialog.setPositiveButton(getString(R.string.confirm_reject), (dialogInterface, i) -> {
                switch (i) {
                    case BUTTON_NEGATIVE:
                        dialogInterface.dismiss();
                        break;
                    case BUTTON_POSITIVE:
                        dialogInterface.dismiss();
                        mItem.setStatus(getString(R.string.rejected));
                        mItem.setUpdateRequired(true);
                        mDataStore.upsert(mItem).subscribe(orderEntity -> {
                            processOrderViewWrapper.setVisibility(View.GONE);
                            SyncAdapter.performSync(getActivity(), mSessionManager.getEmail());

                            Intent intent = new Intent(getActivity(), SalesOrderListActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        });
                        break;
                }
            });
            myAlertDialog.setNegativeButtonText(getString(android.R.string.no));
            if (getActivity() != null && !myAlertDialog.isAdded()) {
                myAlertDialog.show(getActivity().getSupportFragmentManager(), MyAlertDialog.TAG);
            }
        });

        RxView.clicks(rootView.findViewById(R.id.process_order_btn)).subscribe(o -> {
            myAlertDialog.setTitle("Are you sure?");
            myAlertDialog.setPositiveButton(getString(R.string.yes), (dialogInterface, i) -> {
                switch (i) {
                    case BUTTON_NEGATIVE:
                        dialogInterface.dismiss();
                        break;
                    case BUTTON_POSITIVE:
                        dialogInterface.dismiss();
                        mItem.setStatus(getString(R.string.completed));
                        mItem.setUpdateRequired(true);
                        mDataStore.upsert(mItem).subscribe(orderEntity -> {
                            processOrderViewWrapper.setVisibility(View.GONE);
                            SyncAdapter.performSync(getActivity(), mSessionManager.getEmail());

                            Intent intent = new Intent(getActivity(), SalesOrderListActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        });
                        break;
                }
            });
            myAlertDialog.setNegativeButtonText(getString(android.R.string.no));
            if (getActivity() != null && !myAlertDialog.isAdded()) {
                myAlertDialog.show(getActivity().getSupportFragmentManager(), MyAlertDialog.TAG);
            }
        });

        return rootView;
    }

    private class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {
        private ArrayList<OrderItemEntity> mOrderItems;

        OrderItemsAdapter(ArrayList<OrderItemEntity> orderItems) {
            mOrderItems = orderItems;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            OrderItemEntity orderItemEntity = mOrderItems.get(position);
            String merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(getActivity()).getCurrency(mSessionManager.getCurrency()).getSymbol();
            holder.productCost.setText(getString(R.string.product_price, merchantCurrencySymbol, String.valueOf(orderItemEntity.getUnitPrice())));

            ProductEntity productEntity = orderItemEntity.getProduct();
            String nameText = productEntity.getName() + " (" + orderItemEntity.getQuantity() + ")";
            holder.productName.setText(nameText);
            if (getActivity() != null) {
                Glide.with(getActivity()).
                    load(productEntity.getPicture())
                    .into(holder.productImage);
            }
        }

        @Override
        public int getItemCount() {
            return mOrderItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private ImageView productImage;
            private TextView productName;
            private TextView productCost;

            public ViewHolder(View itemView) {
                super(itemView);
                productImage = itemView.findViewById(R.id.product_image);
                productName = itemView.findViewById(R.id.product_name);
                productCost = itemView.findViewById(R.id.product_cost);
            }
        }
    }
}
