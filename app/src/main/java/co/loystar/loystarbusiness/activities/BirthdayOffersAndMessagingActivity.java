package co.loystar.loystarbusiness.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.BirthdayMessageTextFragment;
import co.loystar.loystarbusiness.fragments.BirthdayOffersFragment;

public class BirthdayOffersAndMessagingActivity extends AppCompatActivity {

    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday_offers_and_messaging);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Birthday Offer"));
        tabLayout.addTab(tabLayout.newTab().setText("Birthday Message Text"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        BirthdayOffersAndMessagingActivityPagerAdapter mPagerAdapter = new BirthdayOffersAndMessagingActivityPagerAdapter(
                getSupportFragmentManager(), tabLayout.getTabCount());
        mViewPager = (ViewPager) findViewById(R.id.activity_birthday_offers_and_messaging_pager);
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private class BirthdayOffersAndMessagingActivityPagerAdapter extends FragmentStatePagerAdapter {
        private int mNumOfTabs;;

        BirthdayOffersAndMessagingActivityPagerAdapter(FragmentManager fm, int numOfTabs) {
            super(fm);
            this.mNumOfTabs = numOfTabs;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new BirthdayOffersFragment();
                    break;
                case 1:
                    fragment = new BirthdayMessageTextFragment();
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return mNumOfTabs;
        }
    }
}
