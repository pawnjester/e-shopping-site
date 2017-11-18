package co.loystar.loystarbusiness.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import co.loystar.loystarbusiness.R;

public class BirthdayOffersAndMessagingActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday_offers_and_messaging);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TabLayout tabLayout = findViewById(R.id.activity_birthday_offers_and_messaging_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Birthday Offer"));
        tabLayout.addTab(tabLayout.newTab().setText("Birthday Message Text"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
    }

}
