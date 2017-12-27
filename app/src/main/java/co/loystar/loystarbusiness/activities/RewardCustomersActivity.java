package co.loystar.loystarbusiness.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.api.ApiUtils;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Transaction;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.AlphaNumericInputFilter;
import co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialogAdapter;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RewardCustomersActivity extends AppCompatActivity {
    private Context mContext;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;
    private View mLayout;
    private int mSelectedProgramId;
    private int mSelectedCustomerId;
    private CustomerEntity mCustomer;
    private LoyaltyProgramEntity mLoyaltyProgram;
    private AutoCompleteTextView customerSelectView;
    private List<LoyaltyProgramEntity> mLoyaltyPrograms;
    private EditText redemptionCodeView;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward_customers);
        Toolbar toolbar = findViewById(R.id.reward_customers_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mDatabaseManager = DatabaseManager.getInstance(this);
        mSessionManager = new SessionManager(this);
        ArrayList<CustomerEntity> mCustomers = new ArrayList<>(mDatabaseManager.getMerchantCustomers(mSessionManager.getMerchantId()));

        mLayout = findViewById(R.id.reward_customers_wrapper);
        redemptionCodeView = findViewById(R.id.redemption_code);
        ArrayList<InputFilter> curInputFilters = new ArrayList<>(Arrays.asList(redemptionCodeView.getFilters()));
        curInputFilters.add(0, new AlphaNumericInputFilter());
        final InputFilter[] newInputFilters = curInputFilters.toArray(new InputFilter[curInputFilters.size()]);
        redemptionCodeView.setFilters(newInputFilters);

        customerSelectView = findViewById(R.id.activity_reward_customers_customer_autocomplete);
        customerSelectView.setThreshold(1);
        CustomerAutoCompleteDialogAdapter autoCompleteDialogAdapter = new CustomerAutoCompleteDialogAdapter(mContext, mCustomers);
        customerSelectView.setAdapter(autoCompleteDialogAdapter);

        customerSelectView.setOnItemClickListener((adapterView, view, i, l) -> {
            mCustomer = (CustomerEntity) adapterView.getItemAtPosition(i);
            if (mCustomer != null) {
                mSelectedCustomerId = mCustomer.getId();
                customerSelectView.setText(mCustomer.getFirstName());
            }
        });

        mSelectedCustomerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        mCustomer = mDatabaseManager.getCustomerById(mSelectedCustomerId);
        if (mCustomer != null) {
            customerSelectView.setText(mCustomer.getFirstName());
        }

        mLoyaltyPrograms = mDatabaseManager.getMerchantLoyaltyPrograms(mSessionManager.getMerchantId());
        CharSequence[] programLabels = new CharSequence[mLoyaltyPrograms.size()];
        for (int i = 0; i < mLoyaltyPrograms.size(); i++) {
            programLabels[i] = mLoyaltyPrograms.get(i).getName();
        }
        SpinnerButton selectProgramSpinner = findViewById(R.id.reward_customers_select_program_spinner);
        SpinnerButton.OnItemSelectedListener programItemSelectedListener = position -> {
            mLoyaltyProgram = mLoyaltyPrograms.get(position);
            mSelectedProgramId = mLoyaltyProgram.getId();
        };
        selectProgramSpinner.setListener(programItemSelectedListener);
        selectProgramSpinner.setEntries(programLabels);

        BrandButtonNormal submitBtn = findViewById(R.id.activity_reward_customers_submit_btn);
        submitBtn.setOnClickListener(view -> {
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
            if (mCustomer == null) {
                customerSelectView.setError(getString(R.string.error_select_customer));
                customerSelectView.requestFocus();
                return;
            }
            if (mLoyaltyProgram == null) {
                showSnackbar(R.string.error_loyalty_program_required);
                return;
            }


            if (mCustomer == null) {
                return;
            }

            progressDialog = new ProgressDialog(mContext);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.a_moment));
            progressDialog.show();

            ApiClient apiClient = new ApiClient(mContext);
            apiClient.getLoystarApi(false).redeemReward(
                    redemptionCodeView.getText().toString(),
                    mSelectedCustomerId,
                    mSelectedProgramId).enqueue(new Callback<Transaction>() {
                @Override
                public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> response) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    if (response.isSuccessful()) {
                        Transaction transaction = response.body();

                        SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
                        if (transaction == null) {
                            showSnackbar(R.string.unknown_error);
                        } else {
                            transactionEntity.setId(transaction.getId());
                            transactionEntity.setAmount(transaction.getAmount());
                            transactionEntity.setMerchantLoyaltyProgramId(transaction.getMerchant_loyalty_program_id());
                            transactionEntity.setPoints(transaction.getPoints());
                            transactionEntity.setStamps(transaction.getStamps());
                            transactionEntity.setSynced(true);
                            transactionEntity.setCreatedAt(new Timestamp(transaction.getCreated_at().getMillis()));
                            transactionEntity.setProductId(transaction.getProduct_id());
                            transactionEntity.setProgramType(transaction.getProgram_type());
                            transactionEntity.setUserId(transaction.getUser_id());

                            transactionEntity.setCustomer(mCustomer);
                            transactionEntity.setMerchant(mDatabaseManager.getMerchant(mSessionManager.getMerchantId()));
                            mDatabaseManager.insertNewSalesTransaction(transactionEntity);

                            Bundle bundle = new Bundle();

                            LoyaltyProgramEntity loyaltyProgramEntity = mDatabaseManager.getLoyaltyProgramById(mSelectedProgramId);
                            if (loyaltyProgramEntity != null) {
                                if (loyaltyProgramEntity.getProgramType().equals(getString(R.string.simple_points))) {
                                    int totalPoints = mDatabaseManager.getTotalCustomerPointsForProgram(mSelectedProgramId, mSelectedCustomerId);
                                    bundle.putInt(Constants.TOTAL_CUSTOMER_POINTS, totalPoints);
                                }
                                else if (loyaltyProgramEntity.getProgramType().equals(getString(R.string.stamps_program))) {
                                    int totalStamps = mDatabaseManager.getTotalCustomerStampsForProgram(mSelectedProgramId, mSelectedCustomerId);
                                    bundle.putInt(Constants.TOTAL_CUSTOMER_STAMPS, totalStamps);
                                }
                            }
                            bundle.putBoolean(Constants.SHOW_CONTINUE_BUTTON, false);
                            bundle.putBoolean(Constants.PRINT_RECEIPT, false);
                            bundle.putInt(Constants.LOYALTY_PROGRAM_ID, mSelectedProgramId);
                            bundle.putInt(Constants.CUSTOMER_ID, mSelectedCustomerId);

                            Intent intent = new Intent(mContext, SaleWithoutPosConfirmationActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtras(bundle);
                            startActivity(intent);
                        }

                    } else if (response.code() == 412) {
                        ObjectMapper mapper = ApiUtils.getObjectMapper(false);
                        try {
                            ResponseBody responseBody = response.errorBody();
                            if (responseBody == null) {
                                showSnackbar(R.string.unknown_error);
                            } else {
                                JsonNode responseObject = mapper.readTree(responseBody.charStream());
                                JSONObject errorObject = new JSONObject(responseObject.toString());
                                JSONObject error = errorObject.getJSONObject("error");

                                LayoutInflater inflater = LayoutInflater.from(mContext);
                                View rewardView = inflater.inflate(R.layout.reward_dialog_layout, null);

                                TextView programThresholdView  = rewardView.findViewById(R.id.program_threshold_value);
                                TextView customerValueLabel = rewardView.findViewById(R.id.customer_value_label);
                                TextView customerValue = rewardView.findViewById(R.id.customer_value);

                                if (mLoyaltyProgram.getProgramType().equals(getString(R.string.simple_points))) {
                                    customerValueLabel.setText(R.string.total_customer_points);
                                    customerValue.setText(error.getString("totalCustomerPoints"));

                                }
                                else if (mLoyaltyProgram.getProgramType().equals(getString(R.string.stamps_program))) {
                                    customerValueLabel.setText(R.string.total_customer_stamps);
                                    customerValue.setText(error.getString("totalCustomerStamps"));
                                }

                                programThresholdView.setText(error.getString("threshold"));

                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                                alertDialogBuilder.setView(rewardView);
                                alertDialogBuilder.setTitle(error.getString("message"));
                                alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);

                                alertDialogBuilder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());

                                alertDialogBuilder.create().show();
                            }

                        } catch (IOException | JSONException e) {
                            showSnackbar(R.string.unknown_error);
                            e.printStackTrace();
                        }
                    } else if (response.code() == 404) {
                        redemptionCodeView.setError(getString(R.string.error_redemption_code_incorrect));
                        redemptionCodeView.requestFocus();
                    } else if (response.code() == 422) {
                        ObjectMapper mapper = ApiUtils.getObjectMapper(false);
                        try {
                            ResponseBody responseBody = response.errorBody();
                            if (responseBody == null) {
                                showSnackbar(R.string.unknown_error);
                            } else {
                                JsonNode responseObject = mapper.readTree(responseBody.charStream());
                                JSONObject errorObject = new JSONObject(responseObject.toString());
                                JSONObject errors = errorObject.getJSONObject("errors");
                                JSONArray fullMessagesArray = errors.getJSONArray("full_messages");
                                Snackbar.make(mLayout, fullMessagesArray.join(", "), Snackbar.LENGTH_LONG).show();
                            }
                        } catch (IOException | JSONException e) {
                            showSnackbar(R.string.unknown_error);
                            e.printStackTrace();
                        }
                    } else {
                        showSnackbar(R.string.unknown_error);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable t) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    showSnackbar(R.string.error_internet_connection_timed_out);
                }
            });

        });
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
