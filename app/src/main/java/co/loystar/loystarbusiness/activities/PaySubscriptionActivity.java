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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.databinders.PaySubscription;
import co.loystar.loystarbusiness.models.databinders.PricingPlan;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.MobileNumberInput;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaySubscriptionActivity extends AppCompatActivity {

    private static final String TAG = PaySubscriptionActivity.class.getCanonicalName();
    private static Integer selectedDuration = 1;
    private static double total_amount = 1;
    private static String selectedPlan = "Lite";
    private static double litePlanPrice;

    private Context mContext;
    private SessionManager sessionManager;
    private ApiClient mApiClient;

    /*Views*/
    private Dialog enterMoMoneyDialog,payChoiceDialog;
    private TextView saveMsgView;
    private TextView totalPriceView;
    private TextView litePlanPriceView;
    private int walletProviderSelectedIndex;
    private String currencySymbol;
    private View mProgressView;
    private View mPaymentView;
    private Spinner subscriptionSpinner;
    private MobileNumberInput editMobileMoneyNumber;
    private String   mobileMoneyNo, walletProvider;
    private ProgressDialog progressDialog;
    private View errorView;
    private TextView litePlanSmsBundleView;
    private TextView currencySymbolView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_subscription);
        Toolbar toolbar = findViewById(R.id.activity_pay_subscription_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        sessionManager = new SessionManager(this);
        mApiClient = new ApiClient(this);
        currencySymbol = CurrenciesFetcher.getCurrencies(mContext).getCurrency(sessionManager.getCurrency()).getSymbol();

        /*Initialize Views*/
        BrandButtonNormal payBtn = findViewById(R.id.payBtn);
        totalPriceView = findViewById(R.id.totalPrice);
        mProgressView = findViewById(R.id.fetchPriceProgress);
        mPaymentView = findViewById(R.id.paySubscriptionWrapper);
        litePlanPriceView = findViewById(R.id.litePlanPrice);
        errorView = findViewById(R.id.fetchPriceErrorWrapper);
        currencySymbolView = findViewById(R.id.currencySymbol);
        litePlanSmsBundleView = findViewById(R.id.smsBundleLiteText);
        Button tryAgainBtn = findViewById(R.id.try_again);
        saveMsgView = findViewById(R.id.saveMsg);
        subscriptionSpinner = findViewById(R.id.subscriptionsSpinner);

        payBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (sessionManager.getCurrency().equals("NGN")) {
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

            mApiClient.getLoystarApi(false).getPricingPlanPrice(requestBody).enqueue(new Callback<PricingPlan>() {
                @Override
                public void onResponse(Call<PricingPlan> call, Response<PricingPlan> response) {
                    if (response.isSuccessful()) {
                        showProgress(false, false);
                        PricingPlan pricingPlan = response.body();

                        litePlanPriceView.setText(pricingPlan.getPrice());
                        litePlanSmsBundleView.setText(String.format(Locale.UK, getString(R.string.sms_bundle_lite), pricingPlan.getSmsAllowed()));
                        currencySymbolView.setText(currencySymbol);
                        litePlanPrice = Double.parseDouble(pricingPlan.getPrice());

                        String[] subscriptionDurationList = pricingPlan.getSubscriptionDurationList();
                        List<String> stringList = new ArrayList<>(Arrays.asList(subscriptionDurationList));

                        setSubscriptionsDurationSpinner(stringList);
                    }
                    else {
                        showProgress(false, true);
                    }
                }

                @Override
                public void onFailure(Call<PricingPlan> call, Throwable t) {
                    showProgress(false, true);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setSubscriptionsDurationSpinner( List<String> stringList) {
        ArrayAdapter<String> subsDurationArrayAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, stringList);
        subscriptionSpinner.setAdapter(subsDurationArrayAdapter);
        subscriptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String dr = parent.getItemAtPosition(position).toString();
                if (dr.equals(getString(R.string.six_months))) {
                    selectedDuration = 6;
                    total_amount = litePlanPrice * selectedDuration;
                    String tmt = "%s %.2f ";
                    String total_amount_text = String.format(Locale.UK, tmt, currencySymbol, total_amount);
                    saveMsgView.setText(R.string.saving_msg_6);
                    totalPriceView.setText(total_amount_text);

                } else if (dr.equals(getString(R.string.twelve_months))) {
                    selectedDuration = 12;
                    total_amount = litePlanPrice * selectedDuration;
                    String tmt = "%s %.2f ";
                    String total_amount_text = String.format(Locale.UK, tmt, currencySymbol, total_amount);
                    saveMsgView.setText(R.string.saving_msg_12);
                    totalPriceView.setText(total_amount_text);
                } else if (dr.equals(getString(R.string.three_months))) {
                    selectedDuration = 3;
                    total_amount = litePlanPrice * selectedDuration;
                    String tmt = "%s %.2f ";
                    String total_amount_text = String.format(Locale.UK, tmt, currencySymbol, total_amount);
                    saveMsgView.setText(R.string.saving_msg_3);
                    totalPriceView.setText(total_amount_text);
                } else if (dr.equals(getString(R.string.one_month))) {
                    selectedDuration = 1;
                    total_amount = litePlanPrice * selectedDuration;
                    String tmt = "%s %.2f ";
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

        ImageView mtnMobileMoney = payChoiceDialog.findViewById(R.id.mtn_mobile_money);
        ImageView airtelMoney = payChoiceDialog.findViewById(R.id.airtel_money);

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
        enterMoMoneyDialog.setContentView(R.layout.mobile_money_number_input);
        enterMoMoneyDialog.setTitle(getResources().getString(R.string.pay_with_momoney));

        editMobileMoneyNumber = enterMoMoneyDialog.findViewById(R.id.mobile_money_number);
        RadioGroup rgWalletProvider = enterMoMoneyDialog.findViewById(R.id.wallet_provider);
        RadioButton mtnProvider = enterMoMoneyDialog.findViewById(R.id.mtn_provider);
        RadioButton airtelProvider = enterMoMoneyDialog.findViewById(R.id.airtel_provider);

        /*set checked state*/
        switch (walletProviderSelectedIndex) {
            case 1:
                rgWalletProvider.check(mtnProvider.getId());
                break;
            case 2:
                rgWalletProvider.check(airtelProvider.getId());
                break;
        }

        Button chargePayment = enterMoMoneyDialog.findViewById(R.id.okpay);

        chargePayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!editMobileMoneyNumber.isValid()) {
                    if (editMobileMoneyNumber.getNumber() == null) {
                        editMobileMoneyNumber.setErrorText(getString(R.string.error_mobile_money_number_required));
                    }
                    else {
                        editMobileMoneyNumber.setErrorText(getString(R.string.error_phone_invalid));
                    }
                    return;
                }

                enterMoMoneyDialog.dismiss();

                progressDialog = new ProgressDialog(mContext);
                progressDialog.setTitle("Subscription");
                progressDialog.setMessage("Please wait! subscription in progress...");
                progressDialog.show();

                mobileMoneyNo = editMobileMoneyNumber.getText();

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

                    mApiClient.getLoystarApi(false).paySubscriptionWithMobileMoney(requestBody).enqueue(new Callback<PaySubscription>() {
                        @Override
                        public void onResponse(Call<PaySubscription> call, Response<PaySubscription> response) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            if (response.isSuccessful()) {
                                PaySubscription paySubscription = response.body();

                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                        mContext);

                                alertDialogBuilder.setTitle("Payment request issued");
                                alertDialogBuilder
                                        .setMessage(paySubscription.getDescription())
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
                                Toast.makeText(mContext, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<PaySubscription> call, Throwable t) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(mContext, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (JSONException e) {
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

}
