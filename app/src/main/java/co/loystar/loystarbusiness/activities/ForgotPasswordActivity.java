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
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.SendPasswordResetEmailResponse;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    /*constants*/
    private static final String TAG = ForgotPasswordActivity.class.getSimpleName();
    private static final String hostname = BuildConfig.HOST;

    private EditText email;
    private Context mContext;
    private ProgressDialog progressDialog;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        mLayout = findViewById(R.id.forgot_password_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.reset_password));
        }
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        mContext = this;
        Button submit = (Button) findViewById(R.id.submit);
        email = (EditText) findViewById(R.id.email);
        if (submit != null) {
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }

                    if (!validateEmail()) {
                        return;
                    }

                    progressDialog = new ProgressDialog(mContext);
                    progressDialog.setIndeterminate(true);
                    progressDialog.setMessage(getString(R.string.pwd_reset_msg));
                    progressDialog.show();

                    try {
                        JSONObject requestData = new JSONObject();
                        requestData.put("email", email.getText().toString());
                        requestData.put("redirect_url", hostname + "new_reset_password");

                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                        ApiClient apiClient = new ApiClient(mContext);

                        apiClient.getLoystarApi().sendPasswordResetEmail(requestBody).enqueue(new Callback<SendPasswordResetEmailResponse>() {
                            @Override
                            public void onResponse(Call<SendPasswordResetEmailResponse> call, Response<SendPasswordResetEmailResponse> response) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                                if (response.isSuccessful()) {
                                    SendPasswordResetEmailResponse resetEmailResponse = response.body();

                                    new AlertDialog.Builder(mContext)
                                        .setTitle("Email Sent")
                                        .setMessage(resetEmailResponse.getMessage())
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                Intent intent = new Intent(mContext, SplashActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            }
                                        })
                                        .show();
                                }
                                else {
                                    new AlertDialog.Builder(mContext)
                                        .setTitle(getString(R.string.title_invalid_email))
                                        .setMessage(getString(R.string.feedback_pwdreset_noemail) + email.getText().toString())
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();
                                }
                            }

                            @Override
                            public void onFailure(Call<SendPasswordResetEmailResponse> call, Throwable t) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                                Snackbar.make(mLayout, getString(R.string.error_internet_connection), Snackbar.LENGTH_LONG).show();
                                //Crashlytics.log(2, TAG, t.getMessage());
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }


    private boolean validateEmail() {
        String emailTxt = email.getText().toString().trim();
        if (emailTxt.isEmpty()) {
            email.setError(getString(R.string.error_email_required));
            email.requestFocus();
            return false;
        }
        else if (!TextUtilsHelper.isValidEmail(emailTxt)) {
            email.setError(getString(R.string.error_invalid_email));
            email.requestFocus();
            return false;
        }
        return true;
    }

}
