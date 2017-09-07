package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmPasswordResetActivity extends AppCompatActivity {

    private EditText resetPasswordInput;
    private EditText confirmPasswordInput;
    private EditText resetCodeInput;
    private View mProgressView;
    private View confirmResetPassView;
    private View mLayout;
    private ApiClient mApiClient = LoystarApplication.getInstance().getApiClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_password_reset);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mLayout = findViewById(R.id.confirm_reset_pass_container);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        resetCodeInput = findViewById(R.id.reset_code);
        resetPasswordInput = findViewById(R.id.reset_password);
        confirmPasswordInput = findViewById(R.id.reset_confirm_password);
        TextInputLayout resetCodeInputLayout = findViewById(R.id.reset_code_layout);
        TextInputLayout resetPasswordInputLayout = findViewById(R.id.reset_pass_layout);
        TextInputLayout confirmPasswordInputLayout = findViewById(R.id.confirm_reset_pass_layout);
        resetCodeInputLayout.setPasswordVisibilityToggleEnabled(true);
        resetPasswordInputLayout.setPasswordVisibilityToggleEnabled(true);
        confirmPasswordInputLayout.setPasswordVisibilityToggleEnabled(true);

        BrandButtonNormal submitBtn = findViewById(R.id.confirm_reset_pass_submit);
        mProgressView = findViewById(R.id.confirm_password_reset_email_progress);
        confirmResetPassView = findViewById(R.id.confirm_password_reset_view);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateForm();
            }
        });
    }

    private void validateForm() {
        String resetCode = resetCodeInput.getText().toString();
        String password = resetPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        if (TextUtils.isEmpty(resetCode)) {
            resetCodeInput.setError(getString(R.string.error_reset_code_required));
            resetCodeInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            resetPasswordInput.setError(getString(R.string.error_password_required));
            resetPasswordInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.error_confirm_password_required));
            confirmPasswordInput.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Snackbar.make(mLayout, getString(R.string.error_passwords_mismatch), Snackbar.LENGTH_LONG).show();
            return;
        }

        try {
            showProgress(true);
            JSONObject data = new JSONObject();
            data.put("reset_code", resetCode);
            data.put("password", password);
            data.put("password_confirmation", confirmPassword);

            JSONObject requestData = new JSONObject();
            requestData.put("data", data);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
            mApiClient.getLoystarApi().resetMerchantPassword(requestBody).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    showProgress(false);
                    if (response.isSuccessful()) {
                        new AlertDialog.Builder(ConfirmPasswordResetActivity.this)
                                .setTitle("Password Reset Successful")
                                .setMessage(getString(R.string.password_reset_success))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        AccountManager.get(ConfirmPasswordResetActivity.this).addAccount(
                                                AccountGeneral.ACCOUNT_TYPE,
                                                AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS,
                                                null,
                                                null,
                                                ConfirmPasswordResetActivity.this,
                                                null,
                                                null
                                        );
                                    }
                                })
                                .show();
                    } else if (response.code() == 404) {
                        Snackbar.make(mLayout, getString(R.string.error_reset_password_code_expired), Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(mLayout, getString(R.string.error_reset_password), Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showProgress(false);
                }
            });
        } catch (JSONException e) {
            showProgress(false);
            e.printStackTrace();
        }
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        confirmResetPassView.setVisibility(show ? View.GONE : View.VISIBLE);
        confirmResetPassView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                confirmResetPassView.setVisibility(show ? View.GONE : View.VISIBLE);
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
