package co.loystar.loystarbusiness.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.util.Date;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.CardSalesHistoryFragment;
import co.loystar.loystarbusiness.fragments.CashSalesHistoryFragment;
import co.loystar.loystarbusiness.utils.Constants;

public class SalesHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_history);

        Toolbar toolbar = findViewById(R.id.activity_sales_history_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Date preselectedSaleDate = null;
        if (getIntent().hasExtra(Constants.SALE_DATE)) {
            preselectedSaleDate = (Date) getIntent().getSerializableExtra(Constants.SALE_DATE);
        }
        String preselectedTypeOfSale = getIntent().getStringExtra(Constants.TYPE_OF_SALE);

        if (preselectedSaleDate != null && preselectedTypeOfSale != null) {

        }

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = findViewById(R.id.activity_sales_history_vp);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new CashSalesHistoryFragment();
                    break;
                case 1:
                    fragment = new CardSalesHistoryFragment();
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
