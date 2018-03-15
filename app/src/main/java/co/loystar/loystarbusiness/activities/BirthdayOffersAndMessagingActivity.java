package co.loystar.loystarbusiness.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.BirthdayMessageTextFragment;
import co.loystar.loystarbusiness.fragments.BirthdayOffersFragment;

public class BirthdayOffersAndMessagingActivity extends BaseActivity {
    private ViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday_offers_and_messaging);

        TabLayout tabLayout = findViewById(R.id.activity_birthday_offers_and_messaging_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Birthday Offer"));
        tabLayout.addTab(tabLayout.newTab().setText("Birthday Message Text"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        BirthdayOffersAndMessagingActivityPagerAdapter mPagerAdapter = new BirthdayOffersAndMessagingActivityPagerAdapter(
                getSupportFragmentManager(), tabLayout.getTabCount());
        mViewPager = findViewById(R.id.activity_birthday_offers_and_messaging_vp);
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

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private class BirthdayOffersAndMessagingActivityPagerAdapter extends FragmentStatePagerAdapter {
        private int mNumOfTabs;

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
