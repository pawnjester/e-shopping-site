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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.pojos.LoyaltyProgram;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.LoyaltyProgramsFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;

public class NewLoyaltyProgramListActivity extends AppCompatActivity {
    private static final int REQ_CREATE_PROGRAM = 115;
    private Context mContext;
    private List<LoyaltyProgram> loyaltyPrograms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_loyalty_program_list);
        Toolbar toolbar = findViewById(R.id.activity_new_loyalty_program_list_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;

        View recyclerView = findViewById(R.id.new_loyalty_programs_rv);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        loyaltyPrograms = LoyaltyProgramsFetcher.getLoyaltyPrograms(mContext);
        NewLoyaltyProgramListAdapter mAdapter = new NewLoyaltyProgramListAdapter(loyaltyPrograms);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(
                mContext,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {

            }

            @Override
            public void onLongClick(View view, int position) {
                LoyaltyProgram loyaltyProgram = loyaltyPrograms.get(position);
                if (loyaltyProgram != null) {
                    Intent intent = new Intent(mContext, CreateNewLoyaltyProgramActivity.class);
                    intent.putExtra(Constants.LOYALTY_PROGRAM_TYPE, loyaltyProgram.getId());
                    startActivityForResult(intent, REQ_CREATE_PROGRAM);
                }
            }
        }));
    }

    private class NewLoyaltyProgramListAdapter extends RecyclerView.Adapter<NewLoyaltyProgramListAdapter.ViewHolder> {
        private List<LoyaltyProgram> mPrograms;

        NewLoyaltyProgramListAdapter(List<LoyaltyProgram> loyaltyPrograms) {
            mPrograms = loyaltyPrograms;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView programTitle;
            private TextView programDescription;
            private TextView programActionText;

            ViewHolder(View itemView) {
                super(itemView);
                programTitle = itemView.findViewById(R.id.default_program_title);
                programDescription = itemView.findViewById(R.id.default_program_description);
                programActionText = itemView.findViewById(R.id.default_program_action_text);
            }
        }

        @Override
        public NewLoyaltyProgramListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.default_loyalty_program_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(NewLoyaltyProgramListAdapter.ViewHolder holder, int position) {
            LoyaltyProgram loyaltyProgram = mPrograms.get(position);
            holder.programTitle.setText(loyaltyProgram.getTitle());
            holder.programDescription.setText(loyaltyProgram.getDescription());
            holder.programActionText.setText(getString(R.string.set_program_details_text));
        }

        @Override
        public int getItemCount() {
            return mPrograms.size();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CREATE_PROGRAM && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
