package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.DateTime;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.models.pojos.StampItem;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;

public class AddStampsActivity extends AppCompatActivity {

    private static final String TAG = AddStampsActivity.class.getCanonicalName();

    private int mProgramId;
    private Integer mProductId;
    private int amountSpent;
    private int mCustomerId;
    private int totalCustomerStamps;
    private SessionManager mSessionManager;
    private DatabaseManager mDatabaseManager;
    private Context mContext;
    private CustomerEntity mCustomer;
    private MerchantEntity merchantEntity;
    private List<StampItem> mStampItems = new ArrayList<>();

    /*views*/
    private TextView totalStampsTextView;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stamps);
        Toolbar toolbar = findViewById(R.id.activity_add_stamps_toolbar);
        setSupportActionBar(toolbar);

        mContext = this;
        mSessionManager = new SessionManager(this);
        mDatabaseManager = DatabaseManager.getInstance(this);
        merchantEntity = mDatabaseManager.getMerchant(mSessionManager.getMerchantId());

        mProductId = getIntent().getIntExtra(Constants.PRODUCT_ID, 0);
        mProgramId = getIntent().getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
        amountSpent = getIntent().getIntExtra(Constants.AMOUNT_SPENT, 0);
        mCustomerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);

        mCustomer = mDatabaseManager.getCustomerById(mCustomerId);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (mCustomer != null) {
                String title = "Add Stamps " + "(" + TextUtilsHelper.capitalize(mCustomer.getFirstName()) + ")";
                actionBar.setTitle(title);
            }
        }

        BrandButtonNormal addStampsBtn = findViewById(R.id.add_stamps);
        RxView.clicks(addStampsBtn).subscribe(o -> {
            addStamps();
        });

        RecyclerView recyclerView = findViewById(R.id.stamps_rv);
        LoyaltyProgramEntity loyaltyProgram = mDatabaseManager.getLoyaltyProgramById(mProgramId);
        assert loyaltyProgram != null;
        int stampsThreshold = loyaltyProgram.getThreshold();
        totalCustomerStamps = mDatabaseManager.getTotalCustomerStampsForProgram(mProgramId, mCustomerId);

        setUpGridView(recyclerView, totalCustomerStamps, stampsThreshold);
    }

    private void addStamps() {
        int initialCustomerStamps = mDatabaseManager.getTotalCustomerStampsForProgram(mProgramId, mCustomerId);
        int userStampsForThisTransaction = totalCustomerStamps - initialCustomerStamps;
        if (mProductId != null) {
            ProductEntity product = mDatabaseManager.getProductById(mProductId);
            if (product != null) {
                amountSpent = (int) (userStampsForThisTransaction * product.getPrice());
            }
        }

        SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
        SalesTransactionEntity lastTransactionRecord = mDatabaseManager.getMerchantTransactionsLastRecord(mSessionManager.getMerchantId());

        /* set temporary id*/
        if (lastTransactionRecord == null) {
            transactionEntity.setId(1);
        } else {
            transactionEntity.setId(lastTransactionRecord.getId() + 1);
        }
        transactionEntity.setSynced(false);
        transactionEntity.setSendSms(true);
        transactionEntity.setAmount(amountSpent);
        transactionEntity.setMerchantLoyaltyProgramId(mProgramId);
        transactionEntity.setPoints(0);
        transactionEntity.setStamps(userStampsForThisTransaction);
        transactionEntity.setCreatedAt(new Timestamp(new DateTime().getMillis()));
        transactionEntity.setProductId(mProductId);
        transactionEntity.setProgramType(getString(R.string.stamps_program));
        if (mCustomer != null) {
            transactionEntity.setUserId(mCustomer.getUserId());
        }

        transactionEntity.setMerchant(merchantEntity);
        transactionEntity.setCustomer(mCustomer);

        mDatabaseManager.insertNewSalesTransaction(transactionEntity);
        SyncAdapter.performSync(mContext, mSessionManager.getEmail());

        int newTotalStamps = initialCustomerStamps + userStampsForThisTransaction;

        Bundle bundle = new Bundle();
        bundle.putInt(Constants.TOTAL_CUSTOMER_STAMPS, newTotalStamps);
        bundle.putBoolean(Constants.PRINT_RECEIPT, true);
        bundle.putBoolean(Constants.SHOW_CONTINUE_BUTTON, true);
        bundle.putInt(Constants.LOYALTY_PROGRAM_ID, mProgramId);
        bundle.putInt(Constants.CUSTOMER_ID, mCustomerId);

        Intent intent = new Intent(mContext, TransactionsConfirmation.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void setUpGridView(@NonNull RecyclerView recyclerView, int totalStamps, int stampsThreshold) {
        totalStampsTextView = findViewById(R.id.total_stamps);
        mRecyclerView = recyclerView;
        totalStampsTextView.setText(getString(R.string.total_stamps, String.valueOf(totalStamps)));

        for (int i = 0; i< stampsThreshold; i++) {
            if (i < totalStamps) {
                mStampItems.add(new StampItem(true));
            } else {
                mStampItems.add(new StampItem(false));
            }
        }

        StampsAdapter mAdapter = new StampsAdapter(mStampItems);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, 3);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                StampItem stampItem = mStampItems.get(position);
                if (stampItem.isStamped()) {
                    totalCustomerStamps -= 1;
                    stampItem.setStamped(false);
                    mStampItems.set(position, stampItem);
                    mRecyclerView.getAdapter().notifyItemChanged(position, stampItem);
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                    totalStampsTextView.setText(getString(R.string.total_stamps, String.valueOf(totalCustomerStamps)));
                } else {
                    totalCustomerStamps += 1;
                    stampItem.setStamped(true);
                    mStampItems.set(position, stampItem);
                    mRecyclerView.getAdapter().notifyItemChanged(position, stampItem);
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                    totalStampsTextView.setText(getString(R.string.total_stamps, String.valueOf(totalCustomerStamps)));
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class StampsAdapter extends RecyclerView.Adapter<StampsAdapter.ViewHolder> {
        private List<StampItem> stampItems;

        StampsAdapter(List<StampItem> items) {
            stampItems = items;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView mLabelView;
            private ImageView mImageView;

            ViewHolder(View itemView) {
                super(itemView);
                mLabelView = itemView.findViewById(R.id.grid_item_label);
                mImageView = itemView.findViewById(R.id.grid_item_image);
            }
        }

        @Override
        public StampsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stamp_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(StampsAdapter.ViewHolder holder, int position) {
            StampItem stampItem = stampItems.get(position);
            String txt = "" + (position + 1);
            holder.mLabelView.setText(txt);
            if (stampItem.isStamped()) {
                holder.mImageView.setImageResource(R.drawable.ic_tick);
            } else {
                holder.mImageView.setImageResource(android.R.color.transparent);
            }
        }

        @Override
        public int getItemCount() {
            return stampItems.size();
        }
    }
}
