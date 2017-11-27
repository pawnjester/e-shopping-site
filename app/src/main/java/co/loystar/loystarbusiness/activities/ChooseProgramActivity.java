package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.databinding.ChooseProgramItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgram;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

public class ChooseProgramActivity extends AppCompatActivity {

    private Context mContext;
    private ExecutorService executor;
    private SessionManager mSessionManager;
    private ChooseProgramAdapter mAdapter;
    private ReactiveEntityStore<Persistable> mDataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_program);
        Toolbar toolbar = findViewById(R.id.activity_choose_program_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);

        mAdapter = new ChooseProgramAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        View recyclerView = findViewById(R.id.choose_program_rv);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.queryAsync();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mAdapter.close();
        super.onDestroy();
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

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {

        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                ChooseProgramItemBinding chooseProgramItemBinding = (ChooseProgramItemBinding) view.getTag();
                if (chooseProgramItemBinding != null) {
                    LoyaltyProgram loyaltyProgram = chooseProgramItemBinding.getLoyaltyProgram();
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constants.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                    intent.putExtras(bundle);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    private class ChooseProgramAdapter extends QueryRecyclerAdapter<LoyaltyProgramEntity, BindingHolder<ChooseProgramItemBinding>> {

        ChooseProgramAdapter() {
            super(LoyaltyProgramEntity.$TYPE);
        }

        @Override
        public Result<LoyaltyProgramEntity> performQuery() {
            MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                    .where(MerchantEntity.ID.eq(mSessionManager.getMerchantId()))
                    .get()
                    .firstOrNull();

            if (merchantEntity == null) {
                return null;
            }

            Selection<ReactiveResult<LoyaltyProgramEntity>> programsSelection = mDataStore.select(LoyaltyProgramEntity.class);
            programsSelection.where(LoyaltyProgramEntity.OWNER.eq(merchantEntity));
            programsSelection.where(LoyaltyProgramEntity.DELETED.notEqual(true));
            return programsSelection.orderBy(LoyaltyProgramEntity.UPDATED_AT.desc()).get();
        }

        @Override
        public void onBindViewHolder(LoyaltyProgramEntity item, BindingHolder<ChooseProgramItemBinding> holder, int position) {
            holder.binding.setLoyaltyProgram(item);
            if (item.getProgramType().equals(getString(R.string.simple_points))) {
                holder.binding.programType.setText(getString(R.string.simple_points_program_label));
                holder.binding.programTarget.setText(
                        getString(
                                R.string.program_target_value,
                                String.valueOf(item.getThreshold()),
                                "points")
                );
            } else if (item.getProgramType().equals(getString(R.string.stamps_program))) {
                holder.binding.programType.setText(getString(R.string.stamps_program_label));
                holder.binding.programTarget.setText(
                        getString(
                                R.string.program_target_value,
                                String.valueOf(item.getThreshold()),
                                "stamps")
                );
            }
            holder.binding.getRoot().setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            );
        }

        @Override
        public BindingHolder<ChooseProgramItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ChooseProgramItemBinding binding = ChooseProgramItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            return new BindingHolder<>(binding);
        }
    }
}
