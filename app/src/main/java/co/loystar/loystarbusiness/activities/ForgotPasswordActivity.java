package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
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

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.models.databinders.PasswordReset;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

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
        Toolbar toolbar = findViewById(R.id.activity_forgot_password_toolbar);
        setSupportActionBar(toolbar);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mLayout = findViewById(R.id.forgot_password_container);
        mContext = this;
        mResetPassFormView = findViewById(R.id.reset_password_email_form);
        mProgressView = findViewById(R.id.password_reset_email_progress);

        findViewById(R.id.i_have_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ForgotPasswordActivity.this, ConfirmPasswordResetActivity.class);
                startActivity(intent);
            }
        });

        Button submit = findViewById(R.id.submit);
        email = findViewById(R.id.email);
        if (submit != null) {
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    closeKeyBoard();
                    if (!validateEmail()) {
                        return;
                    }

                    showProgress(true);

                    try {
                        JSONObject requestData = new JSONObject();
                        requestData.put("email", email.getText().toString());
                        requestData.put("redirect_url", hostname + "new_reset_password");

                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                        ApiClient apiClient = new ApiClient(mContext);

                        apiClient.getLoystarApi(false).sendPasswordResetEmail(requestBody).enqueue(new Callback<PasswordReset>() {
                            @Override
                            public void onResponse(@NonNull Call<PasswordReset> call, @NonNull Response<PasswordReset> response) {
                                showProgress(false);

                                if (response.isSuccessful()) {
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
                            public void onFailure(@NonNull Call<PasswordReset> call, @NonNull Throwable t) {
                                showProgress(false);
                                showSnackbar(R.string.error_internet_connection_timed_out);
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
        else if (!TextUtilsHelper.isValidEmailAddress(emailTxt)) {
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

    private void closeKeyBoard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            AccountManager.get(ForgotPasswordActivity.this).addAccount(
                    AccountGeneral.ACCOUNT_TYPE,
                    AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS,
                    null,
                    null,
                    ForgotPasswordActivity.this,
                    accountManagerFuture -> finish(),
                    null
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
