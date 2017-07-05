package co.loystar.loystarbusiness.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;

import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE;

/*Required Intent Extras*/
/**
 * String mProgramValue => customer program value to show
 *  String mCustomerName => customer name to show
 *  String programType => loyalty program type
 *  Long mProgramId => Loyalty programId
 *  Boolean showContinueBtn => boolean flag to show the continue button
 * */
public class TransactionsConfirmation extends AppCompatActivity {

    public static final String SHOW_CONTINUE_BUTTON = "showContinueBtn";
    private Context mContext;
    private View mLayout;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions_confirmation);
        mLayout = findViewById(R.id.confirmations_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        SessionManager sessionManager = new SessionManager(this);
        final DBMerchant merchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());
        Bundle extras = getIntent().getExtras();
        String mCustomerName = extras.getString(RecordDirectSalesActivity.CUSTOMER_NAME);
        String mProgramValue = extras.getString(RecordDirectSalesActivity.CUSTOMER_PROGRAM_WORTH);
        final String programType = extras.getString(RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE);
        final Long mProgramId = extras.getLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, 0L);
        final boolean showContinueBtn = extras.getBoolean(SHOW_CONTINUE_BUTTON, false);

        TextView programTypeTextView = (TextView) findViewById(R.id.program_type_text);
        TextView customerNameView = (TextView) findViewById(R.id.customer_name_value);
        TextView customerLoyaltyWorthView = (TextView) findViewById(R.id.customer_loyalty_worth);
        BrandButtonNormal continueBtn = (BrandButtonNormal) findViewById(R.id.activity_transactions_confirmation_continue_btn) ;

        if (showContinueBtn) {
            continueBtn.setVisibility(View.VISIBLE);
        }

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (programType != null) {
                    if (merchant.getTurn_on_point_of_sale() != null
                            && merchant.getTurn_on_point_of_sale() ) {

                        if (programType.equals(getString(R.string.simple_points))) {
                            Intent intent = new Intent(mContext, RecordPointsSalesWithPosActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mProgramId);
                            startActivity(intent);
                        }
                        else if (programType.equals(getString(R.string.stamps_program))) {
                            Intent intent = new Intent(mContext, RecordStampsSalesWithPosActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mProgramId);
                            startActivity(intent);
                        }
                    }
                    else {
                        Bundle data = new Bundle();
                        data.putString(LOYALTY_PROGRAM_TYPE, programType);
                        data.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mProgramId);

                        Intent intent = new Intent(mContext, RecordDirectSalesActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtras(data);
                        startActivity(intent);
                    }
                }
            }
        });

        if (!AccountGeneral.merchantAccountIsActive()) {
            new AlertDialog.Builder(mContext)
                    .setTitle("Your Account Is Inactive")
                    .setMessage("Please note that all SMS communications are disabled until you resubscribe.")
                    .setPositiveButton(getString(R.string.pay_subscription), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(mContext, PaySubscription.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        if (mCustomerName != null) {
            String customerFirstNameText = mCustomerName.substring(0, 1).toUpperCase() + mCustomerName.substring(1) + " now has";
            customerNameView.setText(customerFirstNameText);
            customerLoyaltyWorthView.setText(mProgramValue);
        }

        if (programType != null) {
            if (programType.equals(getString(R.string.simple_points))) {
                programTypeTextView.setText(getString(R.string.points));
            }
            if (programType.equals(getString(R.string.stamps_program))) {
                int stamps = Integer.parseInt(mProgramValue);
                if (stamps == 1) {
                    programTypeTextView.setText(getString(R.string.stamp));
                }
                else {
                    programTypeTextView.setText(getString(R.string.stamps));
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        registerReceiver(syncFinishedReceiver, new IntentFilter(SyncAdapter.SYNC_FINISHED));
        registerReceiver(syncStartedReceiver, new IntentFilter(SyncAdapter.SYNC_STARTED));

        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(syncFinishedReceiver);
        unregisterReceiver(syncStartedReceiver);
    }

    private BroadcastReceiver syncStartedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(mLayout, R.string.sms_would_be_sent_notice,
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    };

    private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(mLayout, R.string.sms_sent_notice, Snackbar.LENGTH_LONG).show();
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateUpTo(new Intent(mContext, MerchantBackOffice.class));
    }
}
