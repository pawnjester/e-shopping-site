package co.loystar.loystarbusiness.activities;

/**
 * Created by ordgen on 7/4/17.
 */


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.DefaultLoyaltyProgramsAdapter;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.LoyaltyProgramsAdapterItemClickEvent;
import co.loystar.loystarbusiness.fragments.CreateLoyaltyProgramDetailFragment;
import co.loystar.loystarbusiness.models.LoyaltyProgram;
import co.loystar.loystarbusiness.models.LoyaltyProgramsFetcher;

/**
 * An activity representing a list of LoyaltyProgramTypes. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link CreateLoyaltyProgramDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */

public class CreateLoyaltyProgramListActivity extends AppCompatActivity {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_loyaltyprogram_list);
        View mLayout = findViewById(R.id.activity_create_loyaltyprogram_list_container);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getTitle());
        }
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;

        View recyclerView = findViewById(R.id.loyaltyprogramtype_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        if (findViewById(R.id.loyaltyprogramtype_detail_container) != null) {
            mTwoPane = true;
        }

        boolean programUpdatedIntent = getIntent().getBooleanExtra("programCreated", false);
        if (programUpdatedIntent) {
            Snackbar.make(mLayout, getString(R.string.program_created_success), Snackbar.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        ArrayList<LoyaltyProgram> loyaltyPrograms = LoyaltyProgramsFetcher.getLoyaltyPrograms(mContext);
        DefaultLoyaltyProgramsAdapter defaultLoyaltyProgramsAdapter = new DefaultLoyaltyProgramsAdapter(loyaltyPrograms);
        recyclerView.setAdapter(defaultLoyaltyProgramsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Subscribe
    public void onLoyaltyProgramTypeClickEvent(LoyaltyProgramsAdapterItemClickEvent.OnItemClicked onItemClicked) {
        LoyaltyProgram program = LoyaltyProgramsFetcher.getLoyaltyPrograms(mContext).get(onItemClicked.getAdapterPosition());
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(CreateLoyaltyProgramDetailFragment.ARG_ITEM_ID, program.getId());
            CreateLoyaltyProgramDetailFragment fragment = new CreateLoyaltyProgramDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.loyaltyprogramtype_detail_container, fragment)
                    .commit();
        } else {
            Intent intent = new Intent(mContext, CreateLoyaltyProgramDetailActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra(CreateLoyaltyProgramDetailFragment.ARG_ITEM_ID, program.getId());
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
