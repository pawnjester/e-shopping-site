package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.LoyaltyProgramsDetailActivityBackButtonEvent;
import co.loystar.loystarbusiness.events.LoyaltyProgramsDetailFragmentBackButtonEvent;
import co.loystar.loystarbusiness.fragments.LoyaltyProgramsDetailFragment;

/**
 * Created by ordgen on 7/4/17.
 */

public class LoyaltyProgramsDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_loyaltyprogram_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.loyalty_programs_list_detail_toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putLong(LoyaltyProgramsDetailFragment.ARG_ITEM_ID,
                    getIntent().getLongExtra(LoyaltyProgramsDetailFragment.ARG_ITEM_ID, 0L));
            LoyaltyProgramsDetailFragment fragment = new LoyaltyProgramsDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.loyaltyprogram_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            BusProvider.getInstance().post(new LoyaltyProgramsDetailActivityBackButtonEvent.OnBackButtonClicked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Subscribe
    public void OnBackButtonClickListener(LoyaltyProgramsDetailFragmentBackButtonEvent.OnBackButtonClicked onBackButtonClicked) {
        navigateUpTo(new Intent(this, LoyaltyProgramsListActivity.class));
    }
}
