package co.loystar.loystarbusiness.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.LoyaltyProgram;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.EditTextUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNewLoyaltyProgramActivity extends AppCompatActivity {
    private static final String TAG = CreateNewLoyaltyProgramActivity.class.getSimpleName();
    /*views*/
    private EditText programNameView;
    private CurrencyEditText spendingTargetView;
    private EditText rewardView;
    private EditText stampsTarget;
    private View mLayout;

    private String programType;
    private Context mContext;
    private MerchantEntity merchantEntity;
    private DatabaseManager mDatabaseManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_loyalty_program);
        Toolbar toolbar = findViewById(R.id.activity_create_new_loyalty_program_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(AppCompatResources.getDrawable(this, R.drawable.ic_close_white_24px));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        SessionManager mSessionManager = new SessionManager(this);
        mDatabaseManager = DatabaseManager.getInstance(this);
        merchantEntity = mDatabaseManager.getMerchant(mSessionManager.getMerchantId());

        mLayout = findViewById(R.id.activity_create_new_loyalty_program_wrapper);
        programType = getIntent().getStringExtra(Constants.LOYALTY_PROGRAM_TYPE);
        String merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(this).getCurrency(
                mSessionManager.getCurrency()
        ).getSymbol();

        programNameView = findViewById(R.id.program_name);
        rewardView = findViewById(R.id.customer_reward);

        if (programType.equals(getString(R.string.simple_points))) {
            (findViewById(R.id.spending_target_wrapper)).setVisibility(View.VISIBLE);
            spendingTargetView = findViewById(R.id.spending_target);
            ((TextView) findViewById(R.id.spending_target_explanation)).setText(
                    String.format(
                            Locale.UK, getString(R.string.spending_target_explanation),
                            merchantCurrencySymbol)
            );

            String defaultProgramName = mSessionManager.getBusinessName() + " " + "Points Rewards";
            programNameView.setText(defaultProgramName);

            TextView rewardExplanation = findViewById(R.id.reward_text_explanation);
            String merchantBusinessType = mSessionManager.getBusinessType();

            if (merchantBusinessType.equals(getString(R.string.beverages_and_deserts))) {
                rewardExplanation.setText(getString(R.string.beverages_and_deserts_reward));
            }
            else if (merchantBusinessType.equals(getString(R.string.hair_and_beauty))) {
                rewardExplanation.setText(getString(R.string.salon_and_beauty_reward_eg));
            }
            else if (merchantBusinessType.equals(getString(R.string.fashion_and_accessories))) {
                rewardExplanation.setText(getString(R.string.discount_on_next_purchase));
            }
            else if (merchantBusinessType.equals(getString(R.string.gym_and_fitness))) {
                rewardExplanation.setText(getString(R.string.gym_and_fitness_reward_points));
            }
            else if (merchantBusinessType.equals(getString(R.string.bakery_and_pastry))) {
                rewardExplanation.setText(getString(R.string.discount_on_next_purchase));
            }
            else if (merchantBusinessType.equals(getString(R.string.travel_and_hotel))) {
                rewardExplanation.setText(getString(R.string.travel_and_hotel_reward));
            }

        } else if (programType.equals(getString(R.string.stamps_program))) {
            (findViewById(R.id.stamps_target_wrapper)).setVisibility(View.VISIBLE);
            stampsTarget = findViewById(R.id.stamps_target);
            TextView stampsTargetExplanation = findViewById(R.id.stamps_target_explanation);
            String sTemp = "%s eg. 5";

            stampsTargetExplanation.setText(String.format(sTemp, getString(R.string.stamps_target_explanation)));

            TextView rewardExplanation = findViewById(R.id.reward_text_explanation);
            String merchantBusinessType = mSessionManager.getBusinessType();

            String defaultProgramName = mSessionManager.getBusinessName() + " " + "Stamps Rewards";
            programNameView.setText(defaultProgramName);

            if (merchantBusinessType.equals(getString(R.string.beverages_and_deserts))) {
                rewardExplanation.setText(getString(R.string.beverages_and_deserts_reward));
            }
            else if (merchantBusinessType.equals(getString(R.string.hair_and_beauty))) {
                rewardExplanation.setText(getString(R.string.salon_and_beauty_reward_eg));
            }
            else if (merchantBusinessType.equals(getString(R.string.fashion_and_accessories))) {
                rewardExplanation.setText(getString(R.string.fashion_and_accessories_reward_stamps));
            }
            else if (merchantBusinessType.equals(getString(R.string.gym_and_fitness))) {
                rewardExplanation.setText(getString(R.string.gym_and_fitness_reward_stamps));
            }
            else if (merchantBusinessType.equals(getString(R.string.bakery_and_pastry))) {
                rewardExplanation.setText(getString(R.string.next_purchase_free));
            }
            else if (merchantBusinessType.equals(getString(R.string.travel_and_hotel))) {
                rewardExplanation.setText(getString(R.string.travel_and_hotel_reward));
            }
            else {
                rewardExplanation.setText(getString(R.string.next_purchase_free));
            }
        }
    }

    private boolean formIsDirty() {
        if (programType.equals(getString(R.string.simple_points))) {
            return !programNameView.getText().toString().isEmpty() || !rewardView.getText().toString().isEmpty() || spendingTargetView.getRawValue() != 0;
        } else if (programType.equals(getString(R.string.stamps_program))) {
            return !programNameView.getText().toString().isEmpty() || !rewardView.getText().toString().isEmpty() || !stampsTarget.getText().toString().isEmpty();
        }
        return false;

    }

    private void submitForm() {
        if (EditTextUtils.isEmpty(programNameView)) {
            programNameView.setError(getString(R.string.error_program_name_required));
            programNameView.requestFocus();
            return;
        }
        if (programType.equals(getString(R.string.simple_points)) && spendingTargetView.getRawValue() == 0) {
            spendingTargetView.setError(getString(R.string.error_spend_target_cant_be_zero));
            spendingTargetView.requestFocus();
            return;
        }
        if (programType.equals(getString(R.string.stamps_program)) && EditTextUtils.isEmpty(stampsTarget)) {
            stampsTarget.setError(getString(R.string.error_stamps_threshold));
            stampsTarget.requestFocus();
            return;
        }
        if (EditTextUtils.isEmpty(rewardView)) {
            rewardView.setError(getString(R.string.error_reward_required));
            rewardView.requestFocus();
            return;
        }
        showProgressDialog();

        try {
            JSONObject jsonObjectRequestData = new JSONObject();
            jsonObjectRequestData.put("name", programNameView.getText().toString());
            jsonObjectRequestData.put("reward", rewardView.getText().toString());
            jsonObjectRequestData.put("program_type", programType);

            if (programType.equals(getString(R.string.simple_points))) {
                jsonObjectRequestData.put("threshold", spendingTargetView.getFormattedValue(spendingTargetView.getRawValue()));
            }
            else if (programType.equals(getString(R.string.stamps_program))) {
                jsonObjectRequestData.put("threshold", stampsTarget.getText().toString());
            }

            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObjectRequestData);

            ApiClient mApiClient = new ApiClient(mContext);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
            mApiClient.getLoystarApi(false).createMerchantLoyaltyProgram(requestBody).enqueue(new Callback<LoyaltyProgram>() {
                @Override
                public void onResponse(@NonNull Call<LoyaltyProgram> call, @NonNull Response<LoyaltyProgram> response) {
                    dismissProgressDialog();
                    if (response.isSuccessful()) {
                        LoyaltyProgram loyaltyProgram = response.body();
                        if (loyaltyProgram == null) {
                            showSnackbar(R.string.unknown_error);
                        } else {
                            LoyaltyProgramEntity loyaltyProgramEntity = new LoyaltyProgramEntity();
                            loyaltyProgramEntity.setId(loyaltyProgram.getId());
                            loyaltyProgramEntity.setName(loyaltyProgram.getName());
                            loyaltyProgramEntity.setProgramType(loyaltyProgram.getProgram_type());
                            loyaltyProgramEntity.setReward(loyaltyProgram.getReward());
                            loyaltyProgramEntity.setThreshold(loyaltyProgram.getThreshold());
                            loyaltyProgramEntity.setCreatedAt(new Timestamp(loyaltyProgram.getCreated_at().getMillis()));
                            loyaltyProgramEntity.setUpdatedAt(new Timestamp(loyaltyProgram.getUpdated_at().getMillis()));
                            loyaltyProgramEntity.setDeleted(false);
                            loyaltyProgramEntity.setOwner(merchantEntity);

                            mDatabaseManager.insertNewLoyaltyProgram(loyaltyProgramEntity);

                            Intent intent = new Intent();
                            intent.putExtra(Constants.LOYALTY_PROGRAM_CREATED, true);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
                    else {
                        showSnackbar(R.string.error_program_create);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<LoyaltyProgram> call, @NonNull Throwable t) {
                    dismissProgressDialog();
                    showSnackbar(R.string.error_internet_connection_timed_out);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            dismissProgressDialog();
        }
    }

    private void closeKeyBoard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_with_icon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (formIsDirty()) {
                    closeKeyBoard();
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(R.string.discard_changes);
                    builder.setMessage(R.string.discard_changes_explain)
                            .setPositiveButton(R.string.discard, (dialog, id) -> {
                                dialog.dismiss();
                                setResult(RESULT_CANCELED);
                                finish();
                            })
                            .setNegativeButton(getString(R.string.cancel), (dialog, id) -> dialog.dismiss());
                    builder.show();
                }
                else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                return true;
            case R.id.action_done:
                if (formIsDirty()) {
                    closeKeyBoard();
                    submitForm();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage(getString(R.string.a_moment));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (this.isFinishing()) {
            return;
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }
}
