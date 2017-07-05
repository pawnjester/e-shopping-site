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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendSMS extends AppCompatActivity {
    /*constants*/
    private static final String TAG = SendSMS.class.getSimpleName();

    private String customerNumber;
    private TextView charCounterView;
    private EditText msgBox;
    private TextView unitCounterView;
    private Context mContext;
    int totalSmsCredits;
    private View mLayout;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sms);
        mLayout = findViewById(R.id.send_sms_cord_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        String customerName =  getIntent().getExtras().getString(MerchantBackOffice.CUSTOMER_NAME, "");
        if (!customerName.isEmpty()) {
            String title_temp = "Send SMS To %s";
            String cName = customerName.replace("\"", "").substring(0, 1).toUpperCase() +
                customerName.replace("\"", "").substring(1);
            String title_txt = String.format(title_temp, cName);

            if (toolbar != null) {
                toolbar.setTitle(title_txt);
            }
        }

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }



        mContext = this;

        customerNumber = getIntent().getStringExtra(MerchantBackOffice.CUSTOMER_PHONE_NUMBER);

        charCounterView = (TextView) findViewById(R.id.char_counter);
        unitCounterView = (TextView) findViewById(R.id.unit_counter);
        msgBox = (EditText) findViewById(R.id.msg_box);
        Button sendBtn = (Button) findViewById(R.id.send);

        final TextWatcher mTextEditorWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                double sms_char_length = 160;
                int sms_unit = (int) Math.ceil(s.length() / sms_char_length);
                String char_temp = "%s %s";
                String char_temp_unit = s.length() == 1 ? "Character" : "Characters";
                String char_counter_text = String.format(char_temp, s.length(), char_temp_unit);
                String text_template = "%s %s";
                String unit_text = sms_unit != 1 ? "Units" : "Unit";
                String sms_unit_text = String.format(text_template, sms_unit, unit_text);
                unitCounterView.setText(sms_unit_text);
                charCounterView.setText(char_counter_text);
                totalSmsCredits = sms_unit;
            }

            public void afterTextChanged(Editable s) {
            }
        };

        msgBox.addTextChangedListener(mTextEditorWatcher);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (msgBox.getText().toString().isEmpty()) {
                    msgBox.setError(getString(R.string.error_message_required));
                    msgBox.requestFocus();
                    return;
                }

                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                sendMessage();
            }
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
                .setPositiveButton(getString(R.string.send), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();

                        final ProgressDialog progressDialog = new ProgressDialog(mContext);
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

                            ApiClient apiClient = LoystarApplication.getInstance().getApiClient();

                            apiClient.getLoystarApi().sendSms(requestBody).enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    if (progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }

                                    if (response.isSuccessful()) {
                                        Snackbar.make(mLayout, R.string.sms_sent_notice,
                                            Snackbar.LENGTH_INDEFINITE)
                                            .setAction(R.string.ok, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    SessionManager sessionManager = new SessionManager(SendSMS.this);
                                                    DBCustomer customer = databaseHelper.getCustomerByPhone(customerNumber, sessionManager.getMerchantId());
                                                    Intent intent = new Intent(mContext, CustomerListActivity.class);
                                                    if (customer != null) {
                                                        intent.putExtra(MerchantBackOffice.CUSTOMER_ID, customer.getId());
                                                    }
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    startActivity(intent);
                                                }
                                            })
                                            .show();
                                    }
                                    else {
                                        Snackbar.make(mLayout, getString(R.string.error_sending_sms), Snackbar.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    if (progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                    //Crashlytics.log(2, TAG, t.getMessage());
                                    Snackbar.make(mLayout, getString(R.string.error_internet_connection_timed_out), Snackbar.LENGTH_LONG).show();
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

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

}
