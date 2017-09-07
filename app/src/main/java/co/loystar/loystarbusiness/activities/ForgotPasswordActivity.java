package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.SendPasswordResetEmailResponse;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    /*constants*/
    private static final String TAG = ForgotPasswordActivity.class.getSimpleName();
    private static final String hostname = BuildConfig.HOST;

    private EditText email;
    private Context mContext;
    private View mLayout;
    private View mProgressView;
    private View mResetPassFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        mLayout = findViewById(R.id.forgot_password_container);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.reset_password));
        }
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mResetPassFormView = findViewById(R.id.reset_password_email_form);
        mProgressView = findViewById(R.id.password_reset_email_progress);

        findViewById(R.id.i_have_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ForgotPasswordActivity.this, ConfirmPasswordResetActivity.class);
                startActivity(intent);
            }
        });

        mContext = this;
        Button submit = findViewById(R.id.submit);
        email = findViewById(R.id.email);
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

                    showProgress(true);

                    try {
                        JSONObject requestData = new JSONObject();
                        requestData.put("email", email.getText().toString());
                        requestData.put("redirect_url", hostname + "new_reset_password");

                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                        ApiClient apiClient = LoystarApplication.getInstance().getApiClient();

                        apiClient.getLoystarApi().sendPasswordResetEmail(requestBody).enqueue(new Callback<SendPasswordResetEmailResponse>() {
                            @Override
                            public void onResponse(Call<SendPasswordResetEmailResponse> call, Response<SendPasswordResetEmailResponse> response) {
                                showProgress(false);

                                if (response.isSuccessful()) {
                                    SendPasswordResetEmailResponse resetEmailResponse = response.body();

                                    new AlertDialog.Builder(mContext)
                                        .setTitle("Reset Code Sent")
                                        .setMessage(getString(R.string.reset_code_instructions))
                                        .setPositiveButton(getString(R.string.enter_code), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                Intent intent = new Intent(ForgotPasswordActivity.this, ConfirmPasswordResetActivity.class);
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
                                showProgress(false);
                                Snackbar.make(mLayout, getString(R.string.error_internet_connection_timed_out), Snackbar.LENGTH_LONG).show();
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

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mResetPassFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mResetPassFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mResetPassFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AccountManager.get(ForgotPasswordActivity.this).addAccount(
                AccountGeneral.ACCOUNT_TYPE,
                AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS,
                null,
                null,
                ForgotPasswordActivity.this,
                null,
                null
        );
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AccountManager.get(ForgotPasswordActivity.this).addAccount(
                AccountGeneral.ACCOUNT_TYPE,
                AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS,
                null,
                null,
                ForgotPasswordActivity.this,
                null,
                null
        );
    }

}
