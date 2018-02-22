package co.loystar.loystarbusiness.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.darwindeveloper.onecalendar.clases.Day;
import com.darwindeveloper.onecalendar.views.OneCalendarView;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.fragments.CardSalesHistoryFragment;
import co.loystar.loystarbusiness.fragments.CashSalesHistoryFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.EventBus.SalesDetailFragmentEventBus;
import co.loystar.loystarbusiness.utils.Foreground;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import timber.log.Timber;

public class SalesHistoryActivity extends RxAppCompatActivity {

    private static final int REQUEST_CHOOSE_PROGRAM = 110;

    @BindView(R.id.saleDateCalendarSelect)
    OneCalendarView calendarView;

    @BindView(R.id.noSalesView)
    View noSalesView;

    @BindView(R.id.calendarBloc)
    View calendarBloc;

    @BindView(R.id.stateImage)
    ImageView stateImage;

    @BindView(R.id.stateIntroText)
    TextView stateIntroText;

    @BindView(R.id.stateDescriptionText)
    TextView stateDescriptionText;

    @BindView(R.id.stateActionBtn)
    BrandButtonNormal stateActionBtn;

    @BindView(R.id.sales_detail_bs_toolbar)
    Toolbar salesDetailToolbar;

    @BindView(R.id.sales_date)
    TextView salesDateView;

    @BindView(R.id.tabs)
    TabLayout tabLayout;

    @BindView(R.id.activity_sales_history_vp)
    ViewPager mViewPager;

    private Context mContext;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SessionManager mSessionManager;
    private MerchantEntity merchantEntity;
    private Date selectedDate;

    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_history);

        Toolbar toolbar = findViewById(R.id.activity_sales_history_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        ButterKnife.bind(this);
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        calendarView.setOneCalendarClickListener(new OneCalendarView.OneCalendarClickListener() {
            @Override
            public void dateOnClick(Day day, int position) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(day.getDate());
                calendar.set(Calendar.YEAR, 2018);

                selectedDate = calendar.getTime();
                setTotalSales();
            }

            @Override
            public void dateOnLongClick(Day day, int position) {

            }
        });

        calendarView.setOnCalendarChangeListener(new OneCalendarView.OnCalendarChangeListener() {
            @Override
            public void prevMonth() {

            }

            @Override
            public void nextMonth() {

            }
        });

        stateActionBtn.setOnClickListener(view -> startSale());

        mDataStore.count(SaleEntity.class).get().single().subscribe(integer -> {
            if (integer == 0) {
                showNoSalesView();
            }
        });

        setUpBottomSheet();
        Date preselectedSaleDate = null;
        if (getIntent().hasExtra(Constants.SALE_DATE)) {
            preselectedSaleDate = (Date) getIntent().getSerializableExtra(Constants.SALE_DATE);
        }

        if (preselectedSaleDate != null) {
            selectedDate = preselectedSaleDate;
            setTotalSales();
        }
    }

    private void setTotalSales() {
        Calendar startDayCal = Calendar.getInstance();
        startDayCal.setTime(selectedDate);

        Calendar nextDayCal = Calendar.getInstance();
        nextDayCal.setTime(selectedDate);
        nextDayCal.add(Calendar.DAY_OF_MONTH, 1);

        Selection<ReactiveResult<Tuple>> cashResultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
        cashResultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
        cashResultSelection.where(SaleEntity.PAYED_WITH_CASH.eq(true));
        cashResultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

        Tuple cashTuple = cashResultSelection.get().firstOrNull();
        double totalCashSalesForDateSelected;
        if (cashTuple == null || cashTuple.get(0) == null) {
            totalCashSalesForDateSelected = 0;
        } else {
            Double total = cashTuple.get(0);
            if (total > 0) {
                totalCashSalesForDateSelected = total;
            } else {
                totalCashSalesForDateSelected = 0;
            }
        }

        Selection<ReactiveResult<Tuple>> cardResultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
        cardResultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
        cardResultSelection.where(SaleEntity.PAYED_WITH_CARD.eq(true));
        cardResultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

        Tuple cardTuple = cardResultSelection.get().firstOrNull();
        double totalCardSalesForDateSelected;
        if (cardTuple == null || cardTuple.get(0) == null) {
            totalCardSalesForDateSelected = 0;
        } else {
            Double total = cardTuple.get(0);
            if (total > 0) {
                totalCardSalesForDateSelected = total;
            } else {
                totalCardSalesForDateSelected = 0;
            }
        }

        if (totalCardSalesForDateSelected == 0 && totalCashSalesForDateSelected == 0) {
            Toast.makeText(mContext, getString(R.string.no_sales_records), Toast.LENGTH_LONG).show();
        } else {
            showBottomSheet(true);
        }
    }

    private void setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.sale_detail_bottom_sheet_container));
        salesDetailToolbar.setNavigationOnClickListener(view -> showBottomSheet(false));
    }

    /**
     * Shows the no sales view and hides the calendar view.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showNoSalesView() {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        noSalesView.setVisibility(View.VISIBLE);
        stateImage.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_firstsale));
        stateIntroText.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateDescriptionText.setText(getString(R.string.start_sale_empty_state));
        stateActionBtn.setText(getString(R.string.start_sale_btn_label));

        noSalesView.animate()
            .setDuration(shortAnimTime)
            .alpha(1)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    calendarBloc.setVisibility(View.GONE);
                }
            });
    }

    private void startSale() {
        mDataStore.count(LoyaltyProgramEntity.class)
            .get()
            .single()
            .toObservable()
            .subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(Integer integer) {
                    if (integer == 0) {
                        if (Foreground.get().isForeground()) {
                            new AlertDialog.Builder(mContext)
                                .setTitle("No Loyalty Program Found!")
                                .setMessage("To record a sale, you would have to start a loyalty program.")
                                .setPositiveButton(mContext.getString(R.string.start_loyalty_program_btn_label), (dialog, which) -> {
                                    dialog.dismiss();
                                    startLoyaltyProgram();
                                })
                                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss()).show();
                        }
                    } else if (integer == 1) {
                        LoyaltyProgramEntity loyaltyProgramEntity = merchantEntity.getLoyaltyPrograms().get(0);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
                        if (isPosTurnedOn) {
                            startSaleWithPos();
                        } else {
                            startSaleWithoutPos(loyaltyProgramEntity.getId());
                        }
                    } else {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
                        if (isPosTurnedOn) {
                            startSaleWithPos();
                        } else {
                            chooseProgram();
                        }
                    }
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onComplete() {

                }
            });
    }

    private void chooseProgram() {
        Intent intent = new Intent(this, ChooseProgramActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_PROGRAM);
    }

    private void startSaleWithPos() {
        Intent intent = new Intent(this, SaleWithPosActivity.class);
        startActivity(intent);
    }

    private void startSaleWithoutPos(int programId) {
        Intent intent = new Intent(this, SaleWithoutPosActivity.class);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startLoyaltyProgram() {
        Intent intent = new Intent(mContext, LoyaltyProgramListActivity.class);
        intent.putExtra(Constants.CREATE_LOYALTY_PROGRAM, true);
        startActivity(intent);
    }

    private void showBottomSheet(boolean show) {
        if (show) {
            SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(mSectionsPagerAdapter);

            mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
            tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
            tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);

            salesDateView.setText(TextUtilsHelper.getFormattedDateString(calendar));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            mSectionsPagerAdapter.notifyDataSetChanged();
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            showBottomSheet(false);
        } else {
            super.onBackPressed();
        }
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
                    fragment = CashSalesHistoryFragment.getInstance(selectedDate);
                    break;
                case 1:
                    fragment = CardSalesHistoryFragment.getInstance(selectedDate);
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            if (object instanceof  CardSalesHistoryFragment) {
                ((CardSalesHistoryFragment) object).update(selectedDate);
            } else if (object instanceof CashSalesHistoryFragment) {
                ((CashSalesHistoryFragment) object).update(selectedDate);
            }
            return super.getItemPosition(object);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SalesDetailFragmentEventBus
            .getInstance()
            .getFragmentEventObservable()
            .compose(bindToLifecycle())
            .subscribe(bundle -> {
                if (bundle.getInt(Constants.FRAGMENT_EVENT_ID, 0) == SalesDetailFragmentEventBus.ACTION_START_SALE) {
                    startSale();
                }
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CHOOSE_PROGRAM) {
                int programId = data.getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
                startSaleWithoutPos(programId);
            }

        }
    }

    public interface UpdateSelectedDateInterface {
        void update(Date date);
    }
}
