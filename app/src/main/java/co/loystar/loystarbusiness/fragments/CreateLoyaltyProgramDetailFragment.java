package co.loystar.loystarbusiness.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.CreateLoyaltyProgramListActivity;
import co.loystar.loystarbusiness.activities.LoyaltyProgramsListActivity;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.CreateLoyaltyProgramDetailActivityBackButtonEvent;
import co.loystar.loystarbusiness.events.CreateLoyaltyProgramDetailFragmentBackButtonEvent;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.LoyaltyProgram;
import co.loystar.loystarbusiness.models.LoyaltyProgramsFetcher;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput.CurrenciesFetcher;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 7/4/17.
 */

public class CreateLoyaltyProgramDetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = CreateLoyaltyProgramDetailFragment.class.getSimpleName();

    /*shared variables*/
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private ApiClient mApiClient;
    private LoyaltyProgram mItem;

    /*views*/
    private EditText programNameView;
    private CurrencyEditText spendingTargetView;
    private EditText rewardView;
    private EditText stampsTarget;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CreateLoyaltyProgramDetailFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mApiClient = new ApiClient(getContext());
            mItem = LoyaltyProgramsFetcher.getLoyaltyPrograms(getActivity()).getLoyaltyProgram(getArguments().getString(ARG_ITEM_ID));
        }

        Activity activity = this.getActivity();
        android.support.v7.app.ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        if (actionBar != null) {
            if (mItem != null) {
                actionBar.setTitle(mItem.getTitle());
            }
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = null;
        String merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(getContext()).getCurrency(sessionManager.getMerchantCurrency()).getSymbol();


        if (mItem != null) {
            if (mItem.id.equals("spp")) {
                rootView = inflater.inflate(R.layout.loyaltyprogram_type_simple_points_layout, container, false);
                programNameView = (EditText) rootView.findViewById(R.id.program_name);
                spendingTargetView = (CurrencyEditText) rootView.findViewById(R.id.spending_target);
                rewardView = (EditText) rootView.findViewById(R.id.customer_reward);
                ((TextView) rootView.findViewById(R.id.spending_target_explanation)).setText(String.format(Locale.UK, getString(R.string.spending_target_explanation), merchantCurrencySymbol));

                String defaultProgramName = sessionManager.getBusinessName() + " " + "Points Rewards";
                programNameView.setText(defaultProgramName);

                TextView rewardExplanation = (TextView) rootView.findViewById(R.id.reward_text_explanation);
                String merchantBusinessType = sessionManager.getMerchantBusinessType();

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
                else {
                    rewardExplanation.setText(getString(R.string.discount_on_next_purchase));
                }
            }
            else if (mItem.id.equals("slp")) {
                rootView = inflater.inflate(R.layout.loyaltyprogram_type_stamps_layout, container, false);
                programNameView = (EditText) rootView.findViewById(R.id.program_name);
                stampsTarget = (EditText) rootView.findViewById(R.id.stamps_target);
                rewardView = (EditText) rootView.findViewById(R.id.customer_reward);
                TextView stampsTargetExplanation = (TextView) rootView.findViewById(R.id.stamps_target_explanation);
                String sTemp = "%s eg. 5";
                stampsTargetExplanation.setText(String.format(sTemp, getString(R.string.stamps_target_explanation)));

                String defaultProgramName = sessionManager.getBusinessName() + " " + "Stamps Rewards";
                programNameView.setText(defaultProgramName);

                TextView rewardExplanation = (TextView) rootView.findViewById(R.id.reward_text_explanation);
                String merchantBusinessType = sessionManager.getMerchantBusinessType();

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

        return rootView;
    }

    private void closeKeyBoard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.save_with_icon, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

    private void submitForm() {
        if (programNameView.getText().toString().trim().isEmpty()) {
            programNameView.setError(getString(R.string.error_program_name_required));
            programNameView.requestFocus();
            return;
        }
        if (spendingTargetView != null && spendingTargetView.getRawValue() == 0) {
            spendingTargetView.setError(getString(R.string.error_spend_target_cant_be_zero));
            spendingTargetView.requestFocus();
            return;
        }
        if (stampsTarget != null && stampsTarget.getText().toString().trim().isEmpty()) {
            stampsTarget.setError(getString(R.string.error_stamps_threshold));
            stampsTarget.requestFocus();
            return;
        }
        if (rewardView.getText().toString().trim().isEmpty()) {
            rewardView.setError(getString(R.string.error_reward_required));
            rewardView.requestFocus();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.creating_loyalty_wait));
        progressDialog.show();

        try {
            JSONObject jsonObjectRequestData = new JSONObject();
            jsonObjectRequestData.put("name", programNameView.getText().toString());
            jsonObjectRequestData.put("reward", rewardView.getText().toString());

            if (mItem.getId().equals("spp")) {
                jsonObjectRequestData.put("threshold", spendingTargetView.getFormattedValue(spendingTargetView.getRawValue()));
                jsonObjectRequestData.put("program_type", getString(R.string.simple_points));
            }
            else if (mItem.getId().equals("slp")) {
                jsonObjectRequestData.put("threshold", stampsTarget.getText().toString());
                jsonObjectRequestData.put("program_type", getString(R.string.stamps_program));
            }

            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObjectRequestData);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

            mApiClient.getLoystarApi().createMerchantLoyaltyProgram(requestBody).enqueue(new Callback<DBMerchantLoyaltyProgram>() {
                @Override
                public void onResponse(Call<DBMerchantLoyaltyProgram> call, Response<DBMerchantLoyaltyProgram> response) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (response.isSuccessful()) {
                        databaseHelper.insertProgram(response.body());

                        Intent intent = new Intent(getActivity(), LoyaltyProgramsListActivity.class);
                        intent.putExtra("programCreated", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(getContext(), getString(R.string.error_program_create), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<DBMerchantLoyaltyProgram> call, Throwable t) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    //Crashlytics.log(2, TAG, t.getMessage());
                    Toast.makeText(getContext(), getString(R.string.error_program_create_connection), Toast.LENGTH_LONG).show();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean formIsDirty() {
        return !programNameView.getText().toString().isEmpty() || !rewardView.getText().toString().isEmpty() || spendingTargetView != null && spendingTargetView.getRawValue() != 0 || stampsTarget != null && !stampsTarget.getText().toString().isEmpty();
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Subscribe
    public void OnBackButtonClickListener(CreateLoyaltyProgramDetailActivityBackButtonEvent.OnBackButtonClicked onBackButtonClicked) {
        if (formIsDirty()) {
            closeKeyBoard();
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.discard_changes);
            builder.setMessage(R.string.discard_changes_explain)
                    .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            BusProvider.getInstance().post(new CreateLoyaltyProgramDetailFragmentBackButtonEvent.OnBackButtonClicked());
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
        }
        else {
            BusProvider.getInstance().post(new CreateLoyaltyProgramDetailFragmentBackButtonEvent.OnBackButtonClicked());
        }
    }
}
