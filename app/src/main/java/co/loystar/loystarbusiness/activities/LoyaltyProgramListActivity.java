package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
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
import co.loystar.loystarbusiness.databinding.LoyaltyProgramItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.DividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

/**
 * An activity representing a list of LoyaltyPrograms. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link LoyaltyProgramDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class LoyaltyProgramListActivity extends AppCompatActivity {
    private final String KEY_RECYCLER_STATE = "recycler_state";
    private Bundle mBundleRecyclerViewState;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private View mLayout;
    private Context mContext;
    private ExecutorService executor;
    private SessionManager mSessionManager;
    private LoyaltyProgramListAdapter mAdapter;
    private EmptyRecyclerView mRecyclerView;
    private ReactiveEntityStore<Persistable> mDataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyaltyprogram_list);

        Toolbar toolbar = findViewById(R.id.activity_loyalty_program_list_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FloatingActionButton fab = findViewById(R.id.activity_loyalty_program_list_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        mLayout = findViewById(R.id.loyalty_program_list_wrapper);

        mAdapter = new LoyaltyProgramListAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        if (findViewById(R.id.loyalty_program_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        EmptyRecyclerView recyclerView = findViewById(R.id.loyalty_programs_rv);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.loyalty_programs_list_empty_container);
        BrandButtonNormal addBtn = emptyView.findViewById(R.id.no_loyalty_program_add_program_btn);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent(mContext, AddNewCustomerActivity.class);
                startActivity(intent);*/
            }
        });

        mRecyclerView = recyclerView;
        //mRecyclerView.setHasFixedSize(true);
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

    /*public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final ItemListActivity mParentActivity;
        private final List<DummyContent.DummyItem> mValues;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DummyContent.DummyItem item = (DummyContent.DummyItem) view.getTag();
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(LoyaltyProgramDetailFragment.ARG_ITEM_ID, item.id);
                    LoyaltyProgramDetailFragment fragment = new LoyaltyProgramDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.loyaltyprogram_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, LoyaltyProgramDetailActivity.class);
                    intent.putExtra(LoyaltyProgramDetailFragment.ARG_ITEM_ID, item.id);

                    context.startActivity(intent);
                }
            }
        };

        SimpleItemRecyclerViewAdapter(LoyaltyProgramListActivity parent,
                                      List<DummyContent.DummyItem> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.loyaltyprogram_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mIdView.setText(mValues.get(position).id);
            holder.mContentView.setText(mValues.get(position).content);

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mIdView;
            final TextView mContentView;

            ViewHolder(View view) {
                super(view);
                mIdView = (TextView) view.findViewById(R.id.id_text);
                mContentView = (TextView) view.findViewById(R.id.content);
            }
        }
    }*/

    private class LoyaltyProgramListAdapter extends QueryRecyclerAdapter<LoyaltyProgramEntity, BindingHolder<LoyaltyProgramItemBinding>> {

        LoyaltyProgramListAdapter() {
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
            return programsSelection.get();
        }

        @Override
        public void onBindViewHolder(LoyaltyProgramEntity item, BindingHolder<LoyaltyProgramItemBinding> holder, int position) {
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
        public BindingHolder<LoyaltyProgramItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            LoyaltyProgramItemBinding binding = LoyaltyProgramItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            binding.deleteProgramBtn.setTag(binding);
            binding.editProgramBtn.setTag(binding);

            binding.deleteProgramBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LoyaltyProgramItemBinding loyaltyProgramItemBinding = (LoyaltyProgramItemBinding) view.getTag();
                    if (loyaltyProgramItemBinding != null) {
                        new AlertDialog.Builder(mContext)
                                .setTitle("Are you sure?")
                                .setMessage("You won't be able to recover this program.")
                                .setPositiveButton(mContext.getString(R.string.confirm_delete_positive), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
            });

            binding.editProgramBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LoyaltyProgramItemBinding loyaltyProgramItemBinding = (LoyaltyProgramItemBinding) view.getTag();
                    if (loyaltyProgramItemBinding != null) {

                    }
                }
            });
            return new BindingHolder<>(binding);
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
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
}
