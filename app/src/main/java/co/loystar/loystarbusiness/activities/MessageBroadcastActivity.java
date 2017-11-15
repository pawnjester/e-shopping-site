package co.loystar.loystarbusiness.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.utils.TimeUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class MessageBroadcastActivity extends AppCompatActivity {
    private TextView charCounterView;
    private TextInputEditText msgBox;
    private TextView unitCounterView;
    private Context mContext;
    int totalSmsCredits;
    private List<CustomerEntity> mCustomerList;
    private View mLayout;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_broadcast);
        Toolbar toolbar = findViewById(R.id.message_broadcast_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(this);
        SessionManager mSessionManager = new SessionManager(this);

        mCustomerList = mDatabaseManager.getMerchantCustomers(mSessionManager.getMerchantId());

        mLayout = findViewById(R.id.message_broadcast_wrapper);
        charCounterView = findViewById(R.id.charCounter);
        unitCounterView = findViewById(R.id.unitCounter);
        ImageView insertFname = findViewById(R.id.insertFname);
        msgBox = findViewById(R.id.msg_box);

        String recTemp = "This message will be sent to %s customers";
        if (mCustomerList.size() == 1) {
            recTemp = "This message will be sent to %s customer";
        }
        String recTempTxt = String.format(recTemp, mCustomerList.size());
        TextView noOfRecipients = findViewById(R.id.noOfRecipientsText);
        noOfRecipients.setText(recTempTxt);

        if (insertFname != null) {
            insertFname.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    msgBox.getText().insert(msgBox.getSelectionStart(), "[CUSTOMER_NAME]");
                }
            });
        }


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
                totalSmsCredits = mCustomerList.size() * sms_unit;
            }

            public void afterTextChanged(Editable s) {
            }
        };

        msgBox.addTextChangedListener(mTextEditorWatcher);
    }

    private void sendMessages() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(75, 16, 16, 16);


        final TextView msgBoxTextView = new TextView(mContext);
        msgBoxTextView.setText(
                msgBox.getText().toString().replace("[CUSTOMER_NAME]",
                        mCustomerList.get(0).getFirstName()));
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
                        progressDialog = new ProgressDialog(mContext);
                        progressDialog.setMessage(getString(R.string.a_moment));
                        progressDialog.setIndeterminate(true);
                        progressDialog.show();

                        try {
                            JSONObject req = new JSONObject();
                            StdDateFormat mDateFormat = new StdDateFormat();
                            req.put("message_text", msgBox.getText().toString());
                            req.put("client_initiated_time", mDateFormat.format(new DateTime()));

                            JSONObject requestData = new JSONObject();
                            requestData.put("data", req);

                            RequestBody requestBody = RequestBody.create(
                                    MediaType.parse("application/json; charset=utf-8"),
                                    requestData.toString()
                            );

                            ApiClient apiClient = new ApiClient(mContext);

                            apiClient.getLoystarApi(false).sendSmsBlast(requestBody).enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                                    if (progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }

                                    if (response.isSuccessful()) {
                                        Snackbar.make(mLayout, R.string.sms_messages_queued,
                                                Snackbar.LENGTH_INDEFINITE)
                                                .setAction(R.string.ok, new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        Intent intent = new Intent(mContext, CustomerListActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(intent);
                                                    }
                                                })
                                                .show();
                                    }
                                    else {
                                        showSnackbar(R.string.error_sending_sms_blast);
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    if (progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                    showSnackbar((R.string.error_internet_connection_timed_out));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_action_menu, menu);
        if (mCustomerList.isEmpty()) {
            menu.findItem(R.id.action_send).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                validateAndSendMessages();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void validateAndSendMessages() {
        if (msgBox.getText().toString().trim().isEmpty()) {
            msgBox.setError(getString(R.string.error_message_required));
            msgBox.requestFocus();
            return;
        }

        if (!AccountGeneral.isAccountActive(mContext)) {
            new AlertDialog.Builder(mContext)
                    .setTitle("Your Account Is Inactive")
                    .setMessage("SMS communications are disabled until you resubscribe.")
                    .setPositiveButton(getString(R.string.pay_subscription), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
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
            return;
        }

        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        sendMessages();
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

}
