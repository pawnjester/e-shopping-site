package co.loystar.loystarbusiness.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.GetPricingPlanDataResponse;
import co.loystar.loystarbusiness.api.pojos.PaySubscriptionWithMobileMoneyResponse;
import co.loystar.loystarbusiness.utils.GeneralUtils;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput.CurrenciesFetcher;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaySubscription extends AppCompatActivity {

    /*Constants*/
    private static final String TAG = PaySubscription.class.getCanonicalName();
    private static Integer selectedDuration = 1;
    private static double total_amount = 1;
    private static String selectedPlan = "Lite";
    private static double litePlanPrice;

    private Context mContext;
    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private ApiClient mApiClient = LoystarApplication.getInstance().getApiClient();

    /*Views*/
    private Dialog enterMoMoneyDialog,payChoiceDialog;
    private TextView saveMsgView;
    private TextView totalPriceView;
    private TextView litePlanAmountView;
    private int walletProviderSelectedIndex;
    private String currencySymbol;
    private View mProgressView;
    private View mPaymentView;
    private Spinner subscription_duration_spinner;
    private EditText editMobileMoneyNumber;
    private String   mobileMoneyNo, walletProvider;
    private ProgressDialog progressDialog;
    private View errorView;
    private TextView litePlanSmsBundleView;
    private TextView currencySymbolView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_subscription);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        currencySymbol = CurrenciesFetcher.getCurrencies(mContext).getCurrency(sessionManager.getMerchantCurrency()).getSymbol();

        /*Initialize Views*/
        Button payBtn = (Button) findViewById(R.id.pay_btn);
        totalPriceView = (TextView) findViewById(R.id.total_price);
        mProgressView = findViewById(R.id.fetch_price_progress);
        mPaymentView = findViewById(R.id.pay_subscription_view);
        litePlanAmountView = (TextView) findViewById(R.id.lite_plan_amount);
        errorView = findViewById(R.id.error_view);
        currencySymbolView = (TextView) findViewById(R.id.currency_symbol);
        litePlanSmsBundleView = (TextView) findViewById(R.id.sms_bundle_lite_text);
        Button tryAgainBtn = (Button) findViewById(R.id.try_again);
        saveMsgView = (TextView) findViewById(R.id.save_msg);
        if (saveMsgView != null) {
            saveMsgView.setVisibility(View.INVISIBLE);
        }

        subscription_duration_spinner  = (Spinner) findViewById(R.id.subs_duration);

        payBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (sessionManager.getMerchantCurrency().equals("NGN")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://loystar.co/loystar-lite-pay/"));
                    startActivity(browserIntent);
                    return;
                }
                setupPayChoiceDialog();
                payChoiceDialog.show();
            }
        });

        tryAgainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                errorView.setVisibility(View.GONE);
                fetchPricingInfo();
            }
        });

        /*Track Screen Views*/
        /*Answers.getInstance().logContentView(new ContentViewEvent().putContentName("PaySubscriptionScreen").putContentType("Activity")
                .putContentId("PaySubscriptionScreen")
                .putCustomAttribute("Time of the Day ", "Not set")
                .putCustomAttribute("Screen Orientation", "Not set"));*/

        fetchPricingInfo();
    }

    private void fetchPricingInfo() {
        showProgress(true, false);

        JSONObject jsonObjectData = new JSONObject();
        try {
            jsonObjectData.put("plan_name", "Lite");
            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObjectData);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

            mApiClient.getLoystarApi().getPricingPlanPrice(requestBody).enqueue(new Callback<GetPricingPlanDataResponse>() {
                @Override
                public void onResponse(Call<GetPricingPlanDataResponse> call, Response<GetPricingPlanDataResponse> response) {
                    if (response.isSuccessful()) {
                        showProgress(false, false);
                        GetPricingPlanDataResponse planPriceResponse = response.body();

                        litePlanAmountView.setText(planPriceResponse.getPrice());
                        litePlanSmsBundleView.setText(String.format(Locale.UK, getString(R.string.sms_bundle_lite), planPriceResponse.getSmsAllowed()));

                        String tmt = "%s%s";
                        String currencyTxt = String.format(tmt,
                                CurrenciesFetcher.getCurrencies(mContext).getCurrency(planPriceResponse.getCurrency()).getSymbol(),
                                "<sup></sup>");
                        currencySymbolView.setText(GeneralUtils.fromHtml(currencyTxt));
                        litePlanPrice = Double.parseDouble(planPriceResponse.getPrice());

                        String[] subscriptionDurationList = planPriceResponse.getSubscriptionDurationList();
                        List<String> stringList = new ArrayList<>(Arrays.asList(subscriptionDurationList));

                        setSubscriptionsDurationSpinner(stringList);
                    }
                    else {
                        showProgress(false, true);
                    }
                }

                @Override
                public void onFailure(Call<GetPricingPlanDataResponse> call, Throwable t) {
                    showProgress(false, true);
                    //Crashlytics.log(2, TAG, t.getMessage());
                }
            });

        } catch (JSONException e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    private void setSubscriptionsDurationSpinner( List<String> stringList) {
        ArrayAdapter<String> subsDurationArrayAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, stringList);
        subscription_duration_spinner.setAdapter(subsDurationArrayAdapter);
        subscription_duration_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String dr = parent.getItemAtPosition(position).toString();
                if (dr.equals(getString(R.string.six_months))) {
                    selectedDuration = 6;
                    total_amount = litePlanPrice * selectedDuration;
                    String tmt = "TOTAL: %s %.2f ";
                    String total_amount_text = String.format(Locale.UK, tmt, currencySymbol, total_amount);
                    saveMsgView.setText(R.string.saving_msg_6);
                    saveMsgView.setVisibility(View.INVISIBLE);
                    totalPriceView.setText(total_amount_text);

                } else if (dr.equals(getString(R.string.twelve_months))) {
                    selectedDuration = 12;
                    total_amount = litePlanPrice * selectedDuration;
                    String tmt = "TOTAL: %s %.2f ";
                    String total_amount_text = String.format(Locale.UK, tmt, currencySymbol, total_amount);
                    saveMsgView.setText(R.string.saving_msg_12);
                    saveMsgView.setVisibility(View.INVISIBLE);
                    totalPriceView.setText(total_amount_text);
                } else if (dr.equals(getString(R.string.three_months))) {
                    selectedDuration = 3;
                    total_amount = litePlanPrice * selectedDuration;
                    String tmt = "TOTAL: %s %.2f ";
                    String total_amount_text = String.format(Locale.UK, tmt, currencySymbol, total_amount);
                    saveMsgView.setText(R.string.saving_msg_3);
                    saveMsgView.setVisibility(View.INVISIBLE);
                    totalPriceView.setText(total_amount_text);
                } else if (dr.equals(getString(R.string.one_month))) {
                    selectedDuration = 1;
                    total_amount = litePlanPrice * selectedDuration;
                    String tmt = "TOTAL: %s %.2f ";
                    saveMsgView.setVisibility(View.INVISIBLE);
                    String total_amount_text = String.format(Locale.UK, tmt, currencySymbol, total_amount);
                    totalPriceView.setText(total_amount_text);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupPayChoiceDialog() {

        payChoiceDialog = new Dialog(mContext);
        payChoiceDialog.setContentView(R.layout.payment_choice_dialog);
        payChoiceDialog.setTitle(getResources().getString(R.string.how_to_pay));

        ImageView mtnMobileMoney = (ImageView) payChoiceDialog.findViewById(R.id.mtn_mobile_money);
        ImageView airtelMoney = (ImageView) payChoiceDialog.findViewById(R.id.airtel_money);

        mtnMobileMoney.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            /*set selected index*/
                walletProviderSelectedIndex = 1;
                walletProvider = "MTN";

                payChoiceDialog.dismiss();
            /*setup second dialog*/
                setupEnterMobileMoneyNumberDialog();
            /*then show it*/
                enterMoMoneyDialog.show();

            }
        });

        airtelMoney.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                walletProviderSelectedIndex = 2;
                walletProvider = "AIRTEL";

                payChoiceDialog.dismiss();
            /*setup second dialog*/
                setupEnterMobileMoneyNumberDialog();
            /*then show it*/
                enterMoMoneyDialog.show();
            }
        });
    }

    private void setupEnterMobileMoneyNumberDialog() {

        enterMoMoneyDialog = new Dialog(mContext);
        enterMoMoneyDialog.setContentView(R.layout.enter_mobile_money_number);
        enterMoMoneyDialog.setTitle(getResources().getString(R.string.pay_with_momoney));

        editMobileMoneyNumber = (EditText)enterMoMoneyDialog.findViewById(R.id.mobile_money_number);
        RadioGroup rgWalletProvider = (RadioGroup) enterMoMoneyDialog.findViewById(R.id.wallet_provider);
        RadioButton mtnProvider = (RadioButton) enterMoMoneyDialog.findViewById(R.id.mtn_provider);
        RadioButton airtelProvider = (RadioButton) enterMoMoneyDialog.findViewById(R.id.airtel_provider);

        /*set checked state*/
        switch (walletProviderSelectedIndex) {
            case 1:
                rgWalletProvider.check(mtnProvider.getId());
                break;
            case 2:
                rgWalletProvider.check(airtelProvider.getId());
                break;
        }

        Button chargePayment = (Button) enterMoMoneyDialog.findViewById(R.id.okpay);

        chargePayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (editMobileMoneyNumber.getText().toString().trim().isEmpty()) {
                    editMobileMoneyNumber.setError(getString(R.string.error_mobile_money_number_required));
                    return;
                }

                enterMoMoneyDialog.dismiss();

                progressDialog = new ProgressDialog(PaySubscription.this);
                progressDialog.setTitle("Subscription");
                progressDialog.setMessage("Please wait! subscription in progress...");
                progressDialog.show();

                mobileMoneyNo = editMobileMoneyNumber.getText().toString();

                try {
                    JSONObject jsonObjectRequestData = new JSONObject();
                    jsonObjectRequestData.put("wallet_provider", walletProvider);
                    jsonObjectRequestData.put("customer_phone", mobileMoneyNo);
                    jsonObjectRequestData.put("amount", total_amount);
                    jsonObjectRequestData.put("duration", selectedDuration);
                    jsonObjectRequestData.put("plan_type", selectedPlan);

                    JSONObject requestData =  new JSONObject();
                    requestData.put("data", jsonObjectRequestData);


                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                    mApiClient.getLoystarApi().paySubscriptionWithMobileMoney(requestBody).enqueue(new Callback<PaySubscriptionWithMobileMoneyResponse>() {
                        @Override
                        public void onResponse(Call<PaySubscriptionWithMobileMoneyResponse> call, Response<PaySubscriptionWithMobileMoneyResponse> response) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            if (response.isSuccessful()) {
                                PaySubscriptionWithMobileMoneyResponse mobileMoneyResponse = response.body();

                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                        mContext);

                                String guide_text = getString(R.string.mobile_money_transaction_guide);
                                String tmt = "%s %s ";
                                String message_text = String.format(tmt, mobileMoneyResponse.getDescription(), guide_text);
                                alertDialogBuilder.setTitle("Payment request issued");
                                alertDialogBuilder
                                        .setMessage(message_text)
                                        .setCancelable(false)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                onBackPressed();
                                            }
                                        });

                                AlertDialog alertDialog = alertDialogBuilder.create();
                                alertDialog.show();

                            }
                            else {
                                Toast.makeText(PaySubscription.this, getString(R.string.unexpected_error), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<PaySubscriptionWithMobileMoneyResponse> call, Throwable t) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(PaySubscription.this, getString(R.string.unexpected_error), Toast.LENGTH_LONG).show();
                            //Crashlytics.log(2, TAG, t.getMessage());
                        }
                    });
                } catch (JSONException e) {
                    //Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Shows the progress UI and hides the payment view.
     */
    private void showProgress(final boolean show, final boolean showErrorView) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        if (showErrorView) {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
            mPaymentView.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
        }
        else {
            mPaymentView.setVisibility(show ? View.GONE : View.VISIBLE);
            mPaymentView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mPaymentView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    public void onRadioButtonClicked(View view) {
        /* Is the button now checked?*/
        boolean checked = ((RadioButton) view).isChecked();

        /*Check which radio button was clicked*/
        switch(view.getId()) {
            case R.id.mtn_provider:
                if (checked)
                    walletProvider = "MTN";
                break;
            case R.id.airtel_provider:
                if (checked)
                    walletProvider = "AIRTEL";
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}