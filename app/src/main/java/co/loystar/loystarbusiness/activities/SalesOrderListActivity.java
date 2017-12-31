package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.SalesOrderItemBinding;
import co.loystar.loystarbusiness.fragments.SalesOrderDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.OrderItemEntity;
import co.loystar.loystarbusiness.models.entities.SalesOrder;
import co.loystar.loystarbusiness.models.entities.SalesOrderEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.TimeUtils;
import co.loystar.loystarbusiness.utils.ui.MyAlertDialog;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.support.v4.app.NavUtils.navigateUpFromSameTask;

public class SalesOrderListActivity extends AppCompatActivity {

    private boolean mTwoPane;
    private ExecutorService executor;
    private final String KEY_RECYCLER_STATE = "recycler_state";
    private Bundle mBundleRecyclerViewState;
    private SessionManager mSessionManager;
    private ReactiveEntityStore<Persistable> mDataStore;
    private Context mContext;
    private FragmentManager fragmentManager;
    private SalesOrderListAdapter mAdapter;
    private MyAlertDialog myAlertDialog;

    /*Views*/
    private EmptyRecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salesorder_list);

        Toolbar toolbar = findViewById(R.id.activity_sales_order_list_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        fragmentManager = getSupportFragmentManager();
        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        myAlertDialog = new MyAlertDialog();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (findViewById(R.id.sales_order_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        if (getIntent().hasExtra(Constants.SALES_ORDER_ID)){
            int orderId = getIntent().getIntExtra(Constants.SALES_ORDER_ID, 0);
            if (mTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID, orderId);
                SalesOrderDetailFragment salesOrderDetailFragment = new SalesOrderDetailFragment();
                salesOrderDetailFragment.setArguments(arguments);
                fragmentManager.beginTransaction()
                    .replace(R.id.sales_order_detail_container, salesOrderDetailFragment)
                    .commit();
            } else {
                Intent intent = new Intent(mContext, SalesOrderDetailActivity.class);
                intent.putExtra(SalesOrderDetailFragment.ARG_ITEM_ID, orderId);
                startActivity(intent);
            }
        }
        else {
            if (mTwoPane) {
                Result<SalesOrderEntity> salesOrderEntities = mAdapter.performQuery();
                if (!salesOrderEntities.toList().isEmpty()) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID, salesOrderEntities.first().getId());
                    SalesOrderDetailFragment salesOrderDetailFragment = new SalesOrderDetailFragment();
                    salesOrderDetailFragment.setArguments(arguments);
                    fragmentManager.beginTransaction()
                        .replace(R.id.sales_order_detail_container, salesOrderDetailFragment)
                        .commit();
                }
            }
        }

        mAdapter = new SalesOrderListAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        EmptyRecyclerView recyclerView = findViewById(R.id.sales_order_list_rv);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        TextView emptyView = findViewById(R.id.no_orders_empty_view);

        mRecyclerView = recyclerView;
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(
            getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
            getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setEmptyView(emptyView);
    }

   private class SalesOrderListAdapter extends QueryRecyclerAdapter<SalesOrderEntity, BindingHolder<SalesOrderItemBinding>> {

       @Override
       public Result<SalesOrderEntity> performQuery() {
           MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
               .where(MerchantEntity.ID.eq(mSessionManager.getMerchantId()))
               .get()
               .firstOrNull();

           if (merchantEntity == null) {
               return null;
           }
           return mDataStore
               .select(SalesOrderEntity.class)
               .where(SalesOrderEntity.MERCHANT.eq(merchantEntity))
               .orderBy(SalesOrderEntity.CREATED_AT.desc())
               .get();
       }

       @Override
       public void onBindViewHolder(SalesOrderEntity item, BindingHolder<SalesOrderItemBinding> holder, int position) {
            holder.binding.setSalesOrder(item);

            if (item.getStatus().equals(getString(R.string.pending))) {
                holder.binding.salesOrderActionsWrapper.setVisibility(View.VISIBLE);
                holder.binding.statusText.setText(getString(R.string.status_pending));
                holder.binding.statusText.setBackgroundColor(ContextCompat.getColor(mContext, R.color.orange));
            } else if (item.getStatus().equals(getString(R.string.completed))) {
                holder.binding.statusText.setText(getString(R.string.status_completed));
                holder.binding.statusText.setBackgroundColor(ContextCompat.getColor(mContext, R.color.green));
            } else {
                holder.binding.statusText.setText(getString(R.string.status_rejected));
                holder.binding.statusText.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
            }

            holder.binding.timestampText.setText(TimeUtils.getTimeAgo(item.getCreatedAt().getTime(), mContext));


            CustomerEntity customerEntity = item.getCustomer();
            String customerName = customerEntity.getFirstName() + " " + customerEntity.getLastName();
            holder.binding.customerName.setText(customerName);

           List<OrderItemEntity> orderItemEntities = item.getOrderItems();
           StringBuilder stringBuilder  = new StringBuilder();
           for (OrderItemEntity orderItemEntity: orderItemEntities) {
               String productName = orderItemEntity.getProduct().getName();
               stringBuilder
                   .append(productName)
                   .append(" (")
                   .append(orderItemEntity.getQuantity())
                   .append(")").append(", ");
           }
           holder.binding.orderDescription.setText(stringBuilder.toString());

           holder.binding.getRoot().setLayoutParams(new FrameLayout.LayoutParams(
               ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
           );
       }

       @Override
       public BindingHolder<SalesOrderItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
           LayoutInflater inflater = LayoutInflater.from(parent.getContext());
           SalesOrderItemBinding binding = SalesOrderItemBinding.inflate(inflater);
           binding.getRoot().setTag(binding);

           binding.processBtn.setOnClickListener(view -> {
               if (mTwoPane) {
                   Bundle arguments = new Bundle();
                   arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID, binding.getSalesOrder().getId());
                   SalesOrderDetailFragment salesOrderDetailFragment = new SalesOrderDetailFragment();
                   salesOrderDetailFragment.setArguments(arguments);
                   fragmentManager.beginTransaction()
                       .replace(R.id.sales_order_detail_container, salesOrderDetailFragment)
                       .commit();
               } else {
                   Intent intent = new Intent(mContext, SalesOrderDetailActivity.class);
                   intent.putExtra(SalesOrderDetailFragment.ARG_ITEM_ID, binding.getSalesOrder().getId());
                   startActivity(intent);
               }
           });

           binding.itemStatusBloc.setOnClickListener(view -> processOrder(binding.getSalesOrder()));
           binding.itemDescriptionBloc.setOnClickListener(view -> processOrder(binding.getSalesOrder()));

           binding.rejectBtn.setOnClickListener(view -> {
               myAlertDialog.setTitle("Are you sure?");
               myAlertDialog.setPositiveButton(getString(R.string.confirm_reject), (dialogInterface, i) -> {
                   switch (i) {
                       case BUTTON_NEGATIVE:
                           dialogInterface.dismiss();
                           break;
                       case BUTTON_POSITIVE:
                           dialogInterface.dismiss();
                           SalesOrderEntity salesOrderEntity = mDataStore.findByKey(SalesOrderEntity.class, binding.getSalesOrder().getId()).blockingGet();
                           salesOrderEntity.setStatus(getString(R.string.rejected));
                           salesOrderEntity.setUpdateRequired(true);
                           mDataStore.update(salesOrderEntity).subscribe();
                           SyncAdapter.performSync(mContext, mSessionManager.getEmail());
                           mAdapter.queryAsync();
                           break;
                   }
               });
               myAlertDialog.setNegativeButtonText(getString(android.R.string.no));
               myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);
           });

           return new BindingHolder<>(binding);
       }

       private void processOrder(SalesOrder salesOrder) {
           if (mTwoPane) {
               Bundle arguments = new Bundle();
               arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID, salesOrder.getId());
               SalesOrderDetailFragment salesOrderDetailFragment = new SalesOrderDetailFragment();
               salesOrderDetailFragment.setArguments(arguments);
               fragmentManager.beginTransaction()
                   .replace(R.id.sales_order_detail_container, salesOrderDetailFragment)
                   .commit();
           } else {
               Intent intent = new Intent(mContext, SalesOrderDetailActivity.class);
               intent.putExtra(SalesOrderDetailFragment.ARG_ITEM_ID, salesOrder.getId());
               startActivity(intent);
           }
       }
   }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mAdapter.close();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*save RecyclerView state*/
        mBundleRecyclerViewState = new Bundle();
        Parcelable listState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        mBundleRecyclerViewState.putParcelable(KEY_RECYCLER_STATE, listState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*restore RecyclerView state*/
        if (mBundleRecyclerViewState != null) {
            Parcelable listState = mBundleRecyclerViewState.getParcelable(KEY_RECYCLER_STATE);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(listState);
        }

        mAdapter.queryAsync();
    }
}
