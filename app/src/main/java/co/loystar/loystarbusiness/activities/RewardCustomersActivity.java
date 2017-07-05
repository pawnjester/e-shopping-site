package co.loystar.loystarbusiness.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.JsonUtils;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.utils.AlphaNumericInputFilter;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog.CustomerAutoCompleteDialogAdapter;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialog;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogEntity;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogOnItemSelectedListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.CUSTOMER_NAME;
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.CUSTOMER_PROGRAM_WORTH;
import static co.loystar.loystarbusiness.activities.RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE;

/**
 * Created by ordgen on 7/4/17.
 */

/** Intent Extras
 * Long mSelectedCustomerId = Preselected customer Id (optional)
 * */

public class RewardCustomersActivity extends AppCompatActivity implements
        SingleChoiceSpinnerDialogOnItemSelectedListener {

    /*constants*/
    public static final String CUSTOMER_ID = "customerId";

    private Context mContext;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private View mLayout;
    private Long mSelectedProgramId;
    private Long mSelectedCustomerId;
    private DBCustomer mCustomer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward_customers);
        mLayout = findViewById(R.id.reward_customer_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mContext = this;

        final EditText redemptionCodeView = (EditText) findViewById(R.id.redemption_code);
        ArrayList<InputFilter> curInputFilters = new ArrayList<>(Arrays.asList(redemptionCodeView.getFilters()));
        curInputFilters.add(0, new AlphaNumericInputFilter());
        InputFilter[] newInputFilters = curInputFilters.toArray(new InputFilter[curInputFilters.size()]);
        redemptionCodeView.setFilters(newInputFilters);

        List<DBMerchantLoyaltyProgram> loyaltyPrograms = databaseHelper.listMerchantPrograms(sessionManager.getMerchantId());
        SingleChoiceSpinnerDialog loyaltyProgramSpinner = (SingleChoiceSpinnerDialog) findViewById(R.id.activity_reward_customers_program_type_spinner);
        ArrayList<SingleChoiceSpinnerDialogEntity> spinnerItems = new ArrayList<>();
        for (DBMerchantLoyaltyProgram loyaltyProgram: loyaltyPrograms) {
            SingleChoiceSpinnerDialogEntity entity = new SingleChoiceSpinnerDialogEntity(loyaltyProgram.getName(), loyaltyProgram.getId());
            spinnerItems.add(entity);
        }

        Long preselectedProgram = null;
        if (loyaltyPrograms.size() == 1) {
            preselectedProgram = loyaltyPrograms.get(0).getId();
            mSelectedProgramId = preselectedProgram;
        }

        loyaltyProgramSpinner.setItems(spinnerItems, "Select One", this, "Select Program", preselectedProgram, null, "", "");

        final ArrayList<DBCustomer> mCustomers = databaseHelper.listMerchantCustomers(sessionManager.getMerchantId());
        final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.activity_reward_customers_customer_autocomplete);
        autoCompleteTextView.setThreshold(1);
        CustomerAutoCompleteDialogAdapter autoCompleteDialogAdapter = new CustomerAutoCompleteDialogAdapter(mContext, mCustomers);
        autoCompleteTextView.setAdapter(autoCompleteDialogAdapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                DBCustomer customer = (DBCustomer) adapterView.getItemAtPosition(i);
                if (customer != null) {
                    mSelectedCustomerId = customer.getId();
                    autoCompleteTextView.setText(customer.getFirst_name());
                }
            }
        });

        mSelectedCustomerId = getIntent().getLongExtra(CUSTOMER_ID, 0L);
        mCustomer = databaseHelper.getCustomerById(mSelectedCustomerId);
        if (mCustomer != null) {
            autoCompleteTextView.setText(mCustomer.getFirst_name());
        }

        BrandButtonNormal submitBtn = (BrandButtonNormal) findViewById(R.id.activity_reward_customers_submit_btn);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (redemptionCodeView.getText().toString().isEmpty() || redemptionCodeView.getText().toString().length() != 6) {
                    if (redemptionCodeView.getText().toString().isEmpty()) {
                        redemptionCodeView.setError(getString(R.string.error_redemption_code_required));
                        redemptionCodeView.requestFocus();
                        return;
                    }
                    else if (redemptionCodeView.getText().toString().length() != 6) {
                        redemptionCodeView.setError(getString(R.string.error_redemption_code_length));
                        redemptionCodeView.requestFocus();
                        return;
                    }
                }
                if (mSelectedCustomerId == 0) {
                    autoCompleteTextView.setError(getString(R.string.error_select_customer));
                    autoCompleteTextView.requestFocus();
                    return;
                }
                if (mSelectedProgramId == null) {
                    Snackbar.make(mLayout, getString(R.string.error_loyalty_program_required), Snackbar.LENGTH_LONG).show();
                    return;
                }

                mCustomer = databaseHelper.getCustomerById(mSelectedCustomerId);

                if (mCustomer == null) {
                    return;
                }

                final DBMerchantLoyaltyProgram loyaltyProgram = databaseHelper.getProgramById(mSelectedProgramId, sessionManager.getMerchantId());

                final ProgressDialog progressDialog = new ProgressDialog(mContext);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage(getString(R.string.a_moment));
                progressDialog.show();

                ApiClient apiClient = LoystarApplication.getInstance().getApiClient();
                apiClient.getLoystarApi().redeemReward(
                        redemptionCodeView.getText().toString(),
                        mSelectedCustomerId.toString(),
                        mSelectedProgramId.toString()).enqueue(new Callback<DBTransaction>() {
                    @Override
                    public void onResponse(Call<DBTransaction> call, Response<DBTransaction> response) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (response.isSuccessful()) {
                            Long id = databaseHelper.insertTransaction(response.body());

                            if (id != null) {
                                Bundle arguments = new Bundle();

                                arguments.putBoolean(TransactionsConfirmation.SHOW_CONTINUE_BUTTON, false);
                                arguments.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, mSelectedProgramId);
                                arguments.putLong(RecordDirectSalesActivity.CUSTOMER_ID, mSelectedCustomerId);
                                arguments.putString(CUSTOMER_NAME, mCustomer.getFirst_name());

                                if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                                    int totalPoints = databaseHelper.getTotalUserPointsForProgram(
                                            mCustomer.getUser_id(), mSelectedProgramId, sessionManager.getMerchantId());

                                    arguments.putString(LOYALTY_PROGRAM_TYPE, getString(R.string.simple_points));
                                    arguments.putString(CUSTOMER_PROGRAM_WORTH, String.valueOf(totalPoints));

                                }
                                else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                                    int totalStamps = databaseHelper.getTotalUserStampsForProgram(
                                            mCustomer.getUser_id(), mSelectedProgramId, sessionManager.getMerchantId());

                                    arguments.putString(LOYALTY_PROGRAM_TYPE, getString(R.string.stamps_program));
                                    arguments.putString(CUSTOMER_PROGRAM_WORTH, String.valueOf(totalStamps));
                                }

                                Intent intent = new Intent(mContext, TransactionsConfirmation.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtras(arguments);
                                startActivity(intent);
                            }

                        }
                        else {
                            if (response.code() == 412) {

                                ObjectMapper mapper = JsonUtils.objectMapper;
                                try {
                                    JsonNode responseObject = mapper.readTree(response.errorBody().charStream());
                                    JSONObject errorObject = new JSONObject(responseObject.toString());
                                    JSONObject error = errorObject.getJSONObject("error");

                                    LayoutInflater inflater = LayoutInflater.from(mContext);
                                    View rewardView = inflater.inflate(R.layout.reward_dialog_layout, null);

                                    TextView programThresholdView  = (TextView) rewardView.findViewById(R.id.program_threshold_value);
                                    TextView customerValueLabel = (TextView) rewardView.findViewById(R.id.customer_value_label);
                                    TextView customerValue = (TextView) rewardView.findViewById(R.id.customer_value);

                                    if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                                        customerValueLabel.setText(R.string.total_customer_points);
                                        customerValue.setText(error.getString("totalCustomerPoints"));

                                    }
                                    else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                                        customerValueLabel.setText(R.string.total_customer_stamps);
                                        customerValue.setText(error.getString("totalCustomerStamps"));
                                    }

                                    programThresholdView.setText(error.getString("threshold"));

                                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                                    alertDialogBuilder.setView(rewardView);
                                    alertDialogBuilder.setTitle(error.getString("message"));
                                    alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);

                                    alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });

                                    alertDialogBuilder.create().show();

                                } catch (IOException | JSONException e) {
                                    //Crashlytics.logException(e);
                                    e.printStackTrace();
                                }
                            }
                            else if (response.code() == 404) {
                                redemptionCodeView.setError(getString(R.string.error_redemption_code_incorrect));
                                redemptionCodeView.requestFocus();
                            }
                            else {
                                Snackbar.make(mLayout, getString(R.string.something_went_wrong), Snackbar.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<DBTransaction> call, Throwable t) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Snackbar.make(mLayout, getString(R.string.error_internet_connection_timed_out), Snackbar.LENGTH_LONG).show();
                    }
                });

            }
        });


    }


    @Override
    public void itemSelectedId(Long Id) {
        mSelectedProgramId = Id;
    }
}
