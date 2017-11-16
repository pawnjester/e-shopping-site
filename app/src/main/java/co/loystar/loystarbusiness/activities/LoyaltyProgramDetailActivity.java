package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.LoyaltyProgramDetailFragment;

public class LoyaltyProgramDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty_program_detail);
        Toolbar toolbar = findViewById(R.id.activity_loyalty_program_detail_toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putInt(LoyaltyProgramDetailFragment.ARG_ITEM_ID,
                    getIntent().getIntExtra(LoyaltyProgramDetailFragment.ARG_ITEM_ID, 0));
            LoyaltyProgramDetailFragment fragment = new LoyaltyProgramDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.loyalty_program_detail_container, fragment)
                    .commit();
        }
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, LoyaltyProgramListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
