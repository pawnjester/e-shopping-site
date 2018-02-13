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
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.darwindeveloper.onecalendar.clases.Day;
import com.darwindeveloper.onecalendar.views.OneCalendarView;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.SalesHistoryAdapter;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.Foreground;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

public class SalesHistoryActivity extends AppCompatActivity {

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

    @BindView(R.id.typeOfSaleSpinner)
    SpinnerButton typeOfSaleSpinner;

    @BindView(R.id.sales_detail_bs_toolbar)
    Toolbar salesDetailToolbar;

    @BindView(R.id.sales_detail_rv)
    RecyclerView recyclerView;

    @BindView(R.id.sales_date)
    TextView salesDateView;

    @BindView(R.id.total_sales)
    TextView totalSalesView;

    @BindView(R.id.total_label)
    TextView totalLabelView;

    private Context mContext;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SessionManager mSessionManager;
    private MerchantEntity merchantEntity;
    private String typeOfSaleSelected;
    private Date selectedDate;
    private double totalSalesForDateSelected;

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

        final CharSequence[] typeOfSaleEntries = getResources().getStringArray(R.array.type_of_sale);
        SpinnerButton.OnItemSelectedListener typeOfSaleItemSelectedListener = position -> {
            String selected = (String) typeOfSaleEntries[position];
            if (selected.equals(getString(R.string.cash_sales))) {
                typeOfSaleSelected = Constants.CASH_SALE;
            } else if (selected.equals(getString(R.string.card_sales))) {
                typeOfSaleSelected = Constants.CARD_SALE;
            }
        };

        typeOfSaleSpinner.setListener(typeOfSaleItemSelectedListener);

        calendarView.setOneCalendarClickListener(new OneCalendarView.OneCalendarClickListener() {
            @Override
            public void dateOnClick(Day day, int position) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(day.getDate());
                calendar.set(Calendar.YEAR, 2018);

                selectedDate = calendar.getTime();
                if (TextUtils.isEmpty(typeOfSaleSelected)) {
                    Toast.makeText(mContext, getString(R.string.error_type_of_sale_required), Toast.LENGTH_LONG).show();
                } else {
                    setTotalSales();
                }
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
        String preselectedTypeOfSale = getIntent().getStringExtra(Constants.TYPE_OF_SALE);

        if (preselectedSaleDate != null && preselectedTypeOfSale != null) {
            selectedDate = preselectedSaleDate;
            typeOfSaleSelected = preselectedTypeOfSale;
            if (typeOfSaleSelected.equals(Constants.CASH_SALE)) {
                int index = Arrays.asList(typeOfSaleEntries).indexOf(getString(R.string.cash_sales));
                typeOfSaleSpinner.setSelection(index);
            } else if (typeOfSaleSelected.equals(Constants.CARD_SALE)) {
                int index = Arrays.asList(typeOfSaleEntries).indexOf(getString(R.string.card_sales));
                typeOfSaleSpinner.setSelection(index);
            }
            setTotalSales();
        }
    }

    private void setTotalSales() {
        Calendar startDayCal = Calendar.getInstance();
        startDayCal.setTime(selectedDate);

        Calendar nextDayCal = Calendar.getInstance();
        nextDayCal.setTime(selectedDate);
        nextDayCal.add(Calendar.DAY_OF_MONTH, 1);

        if (typeOfSaleSelected.equals(Constants.CASH_SALE)) {
            totalLabelView.setText(getString(R.string.total_label_cash));

            Selection<ReactiveResult<Tuple>> resultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
            resultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
            resultSelection.where(SaleEntity.PAYED_WITH_CASH.eq(true));
            resultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

            Tuple tuple = resultSelection.get().firstOrNull();
            if (tuple == null || tuple.get(0) == null) {
                Toast.makeText(mContext, getString(R.string.no_sales_records), Toast.LENGTH_LONG).show();
            } else {
                Double total = tuple.get(0);
                if (total > 0) {
                    totalSalesForDateSelected = total;
                    showBottomSheet(true);
                } else {
                    Toast.makeText(mContext, getString(R.string.no_sales_records), Toast.LENGTH_LONG).show();
                }
            }
        } else if (typeOfSaleSelected.equals(Constants.CARD_SALE)) {
            totalLabelView.setText(getString(R.string.total_label_card));

            Selection<ReactiveResult<Tuple>> resultSelection = mDataStore.select(SaleEntity.TOTAL.sum());
            resultSelection.where(SaleEntity.MERCHANT.eq(merchantEntity));
            resultSelection.where(SaleEntity.PAYED_WITH_CARD.eq(true));
            resultSelection.where(SaleEntity.CREATED_AT.between(new Timestamp(startDayCal.getTimeInMillis()), new Timestamp(nextDayCal.getTimeInMillis())));

            Tuple tuple = resultSelection.get().firstOrNull();
            if (tuple == null || tuple.get(0) == null) {
                Toast.makeText(mContext, getString(R.string.no_sales_records), Toast.LENGTH_LONG).show();
            } else {
                Double total = tuple.get(0);
                if (total > 0) {
                    totalSalesForDateSelected = total;
                    showBottomSheet(true);
                } else {
                    Toast.makeText(mContext, getString(R.string.no_sales_records), Toast.LENGTH_LONG).show();
                }
            }
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
            SalesHistoryAdapter mAdapter = new SalesHistoryAdapter(mContext, merchantEntity, selectedDate, typeOfSaleSelected);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);

            salesDateView.setText(TextUtilsHelper.getFormattedDateString(calendar));
            String merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(mContext).getCurrency(mSessionManager.getCurrency()).getSymbol();
            totalSalesView.setText(getString(R.string.total_sale_value, merchantCurrencySymbol, String.valueOf(totalSalesForDateSelected)));

            recyclerView.setHasFixedSize(true);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_small),
                getResources().getDimensionPixelOffset(R.dimen.item_space_small))
            );
            recyclerView.setAdapter(mAdapter);

            mAdapter.queryAsync();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
}
