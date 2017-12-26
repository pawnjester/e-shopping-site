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
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.utils.Constants;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendSmsActivity extends AppCompatActivity {

    private String customerNumber;
    private TextView charCounterView;
    private EditText msgBox;
    private TextView unitCounterView;
    private Context mContext;
    int totalSmsCredits;
    private View mLayout;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sms);
        Toolbar toolbar = findViewById(R.id.send_sms_activity_toolbar);

        if (getIntent().getExtras() != null) {
            String customerName = getIntent().getExtras().getString(Constants.CUSTOMER_NAME, "");
            if (!customerName.isEmpty()) {
                String title_temp = "Message %s";
                String cName = customerName.replace("\"", "").substring(0, 1).toUpperCase() +
                        customerName.replace("\"", "").substring(1);
                String titleTxt = String.format(title_temp, cName);

                if (toolbar != null) {
                    toolbar.setTitle(titleTxt);
                }
            }
        }
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        mLayout = findViewById(R.id.send_sms_activity_wrapper);
        customerNumber = getIntent().getStringExtra(Constants.PHONE_NUMBER);

        charCounterView = findViewById(R.id.charCounter);
        unitCounterView = findViewById(R.id.unitCounter);
        msgBox = findViewById(R.id.msgBox);
        Button sendBtn = findViewById(R.id.send);

        RxTextView.textChangeEvents(msgBox).subscribe(textViewTextChangeEvent -> {
           CharSequence s = textViewTextChangeEvent.text();
            double sms_char_length = 160;
            int sms_unit = (int) Math.ceil(s.length() / sms_char_length);
            String charTemp = "%s %s";
            String charTempUnit = s.length() == 1 ? "Character" : "Characters";
            String charCounterText = String.format(charTemp, s.length(), charTempUnit);
            String textTemplate = "%s %s";
            String unitText = sms_unit != 1 ? "Units" : "Unit";
            String smsUnitText = String.format(textTemplate, sms_unit, unitText);
            unitCounterView.setText(smsUnitText);
            charCounterView.setText(charCounterText);
            totalSmsCredits = sms_unit;
        });

        RxView.clicks(sendBtn).subscribe(o -> {
            if (msgBox.getText().toString().isEmpty()) {
                msgBox.setError(getString(R.string.error_message_required));
                msgBox.requestFocus();
                return;
            }

            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
            sendMessage();
        });
    }

    private void sendMessage() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(75, 16, 16, 16);


        final TextView msgBoxTextView = new TextView(mContext);
        msgBoxTextView.setText(msgBox.getText().toString());
        layout.addView(msgBoxTextView, layoutParams);

        final TextView total_sms = new TextView(mContext);
        String total_sms_temp = "Estimated SMS credits to be charged: %s";
        String total_sms_txt = String.format(total_sms_temp, totalSmsCredits);
        total_sms.setText(total_sms_txt);
        layout.addView(total_sms, layoutParams);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Preview Message");
        alertDialogBuilder.setView(layout);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.send), (dialog, id) -> {
                    dialog.dismiss();

                    progressDialog = new ProgressDialog(mContext);
                    progressDialog.setTitle("Please wait...");
                    progressDialog.setMessage("Sending Message...");
                    progressDialog.show();

                    try {
                        JSONObject req = new JSONObject();
                        req.put("phone_number", customerNumber);
                        req.put("message_text", msgBox.getText().toString());

                        JSONObject requestData = new JSONObject();
                        requestData.put("data", req);

                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                        ApiClient apiClient = new ApiClient(mContext);

                        apiClient.getLoystarApi(false).sendSms(requestBody).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                                if (response.isSuccessful()) {
                                    Snackbar.make(mLayout, R.string.message_sent_notice,
                                            Snackbar.LENGTH_INDEFINITE)
                                            .setAction(R.string.ok, view -> {
                                                DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
                                                CustomerEntity customer = databaseManager.getCustomerByPhone(customerNumber);
                                                Intent intent = new Intent(mContext, CustomerListActivity.class);
                                                if (customer != null) {
                                                    intent.putExtra(Constants.CUSTOMER_ID, customer.getId());
                                                }
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);
                                            })
                                            .show();
                                }
                                else {
                                    showSnackbar(R.string.error_sending_sms);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                showSnackbar(R.string.error_internet_connection_timed_out);
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, id) -> dialog.dismiss());

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

}
