package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.CreateLoyaltyProgramDetailActivityBackButtonEvent;
import co.loystar.loystarbusiness.events.CreateLoyaltyProgramDetailFragmentBackButtonEvent;
import co.loystar.loystarbusiness.fragments.CreateLoyaltyProgramDetailFragment;

/**
 * Created by ordgen on 7/4/17.
 */

public class CreateLoyaltyProgramDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_loyaltyprogram_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.loyalty_program_type_list_detail_toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(AppCompatDrawableManager.get().getDrawable(this, R.drawable.ic_close_white_24px));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {

            Bundle arguments = new Bundle();
            arguments.putString(CreateLoyaltyProgramDetailFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(CreateLoyaltyProgramDetailFragment.ARG_ITEM_ID));
            CreateLoyaltyProgramDetailFragment fragment = new CreateLoyaltyProgramDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.loyaltyprogramtype_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            BusProvider.getInstance().post(new CreateLoyaltyProgramDetailActivityBackButtonEvent.OnBackButtonClicked());
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
    public void OnBackButtonClickListener(CreateLoyaltyProgramDetailFragmentBackButtonEvent.OnBackButtonClicked onBackButtonClicked) {
        navigateUpTo(new Intent(this, CreateLoyaltyProgramListActivity.class));
    }
}
