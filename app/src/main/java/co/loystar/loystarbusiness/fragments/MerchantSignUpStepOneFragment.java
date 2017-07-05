package co.loystar.loystarbusiness.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.MerchantBackOffice;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.CheckPhoneAndEmailResponse;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.IntlPhoneInput.IntlPhoneInput;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 7/4/17.
 */

public class MerchantSignUpStepOneFragment extends Fragment {
    private OnMerchantSignUpStepOneInteractionListener mListener;
    private static final String TAG = MerchantSignUpStepOneFragment.class.getCanonicalName();

    /*Views*/
    private TextInputLayout business_name_wrapper;
    private TextInputLayout email_wrapper;
    private EditText business_name;
    private IntlPhoneInput business_phone;
    private EditText business_email;
    private ProgressBar check_phone_spinner;
    private ProgressBar check_email_spinner;
    private EditText first_name;
    private TextInputLayout first_name_wrapper;
    private View mLayout;
    private ProgressDialog progressDialog;
    private String businessPhoneVal;
    private String emailVal;

    public MerchantSignUpStepOneFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ShowToast")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_merchant_sign_up_step_one, container, false);
        mLayout = rootView.findViewById(R.id.sign_up_step_one_content);
        android.support.v7.app.ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.title_activity_merchant_signup);
        }

        String merchantPhoneNumber = getArguments().getString(MerchantBackOffice.CUSTOMER_PHONE_NUMBER, "");

        /*Initialize views*/
        business_name_wrapper = (TextInputLayout) rootView.findViewById(R.id.business_name_wrapper);
        email_wrapper = (TextInputLayout) rootView.findViewById(R.id.email_wrapper);
        business_email = (EditText) rootView.findViewById(R.id.business_email);
        first_name_wrapper = (TextInputLayout) rootView.findViewById(R.id.first_name_wrapper);
        first_name  = (EditText) rootView.findViewById(R.id.first_name);
        Button nextButton = (Button) rootView.findViewById(R.id.next_button);
        business_name = (EditText) rootView.findViewById(R.id.business_name);
        business_phone = (IntlPhoneInput) rootView.findViewById(R.id.business_phone);
        TextView login = (TextView) rootView.findViewById(R.id.login);
        if (!TextUtils.isEmpty(merchantPhoneNumber)) {
            business_phone.setNumber(merchantPhoneNumber);
        }

        check_phone_spinner = (ProgressBar) rootView.findViewById(R.id.check_phone_pb);
        check_email_spinner = (ProgressBar) rootView.findViewById(R.id.check_email_pb);
        check_phone_spinner.setVisibility(View.GONE);
        check_email_spinner.setVisibility(View.GONE);


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onMerchantSignUpStepOneInteraction(getString(R.string.already_have_account));
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitForm();
            }
        });

        return rootView;
    }

    private void submitForm() {
        if (!validateEmptyField(first_name, first_name_wrapper, "First name is required")) {
            return;
        }
        if (!validateEmptyField(business_name, business_name_wrapper, "Business name is required")) {
            return;
        }
        if (!business_phone.isValid()) {
            String phone = business_phone.getNumber();
            if (phone == null) {
                Snackbar.make(mLayout, R.string.error_phone_required,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
            else {
                Snackbar.make(mLayout, R.string.error_phone_invalid,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
            business_phone.requestFocus();
            return;
        }
        if (!validateEmail()) {
            return;
        }

        validateSpecialFields();
    }

    private void validateSpecialFields() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Snackbar.make(mLayout, R.string.error_phone_not_verified,
                    Snackbar.LENGTH_LONG)
                    .show();
            business_phone.requestFocus();
            return;
        }

        /*get the phone number from the active firebase session. this prevents a user
          from changing the phone number after firebase verification*/
        businessPhoneVal = u.getPhoneNumber();
        emailVal = business_email.getText().toString();

        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        JSONObject jsonObjectData = new JSONObject();
        try {
            jsonObjectData.put("contact_number", businessPhoneVal);
            jsonObjectData.put("email", emailVal);

            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObjectData);

            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.a_moment));
            progressDialog.show();

            check_phone_spinner.setVisibility(View.VISIBLE);
            check_email_spinner.setVisibility(View.VISIBLE);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
            ApiClient apiClient = new ApiClient(getActivity());
            apiClient.getLoystarApi().checkPhoneAndEmailAvailability(requestBody).enqueue(new Callback<CheckPhoneAndEmailResponse>() {
                @Override
                public void onResponse(Call<CheckPhoneAndEmailResponse> call, Response<CheckPhoneAndEmailResponse> response) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    check_phone_spinner.setVisibility(View.GONE);
                    check_email_spinner.setVisibility(View.GONE);

                    if (response.isSuccessful()) {
                        CheckPhoneAndEmailResponse responseBody = response.body();

                        Activity activity = getActivity();
                        if(activity != null && isAdded()) {
                            if (!responseBody.getPhoneAvailable()) {
                                Snackbar.make(mLayout, R.string.error_phone_taken,
                                        Snackbar.LENGTH_LONG)
                                        .show();
                                business_phone.requestFocus();
                            }
                            else if (!responseBody.getEmailAvailable()) {
                                business_email.setError(getString(R.string.error_email_taken));
                                business_email.requestFocus();
                            }
                            else {
                                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("business_name", business_name.getText().toString());
                                editor.putString("business_email", emailVal);
                                editor.putString("business_phone", businessPhoneVal);
                                editor.putString("first_name", first_name.getText().toString());
                                editor.apply();
                                mListener.onMerchantSignUpStepOneInteraction(getString(R.string.go));
                            }
                        }
                    }
                    else {
                        Snackbar.make(mLayout, getString(R.string.unexpected_error), Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<CheckPhoneAndEmailResponse> call, Throwable t) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    check_phone_spinner.setVisibility(View.GONE);
                    check_email_spinner.setVisibility(View.GONE);

                    Snackbar.make(mLayout, getString(R.string.error_internet_connection_timed_out), Snackbar.LENGTH_LONG).show();
                }
            });


        } catch (JSONException e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }

    }

    public boolean validateEmptyField(EditText field, TextInputLayout textInputLayout, String error_msg) {
        if (field.getText().toString().trim().isEmpty()) {
            field.setError(error_msg);
            field.requestFocus();
            return false;
        } else {
            textInputLayout.setErrorEnabled(false);
        }

        return true;
    }


    private boolean validateEmail() {
        String email = business_email.getText().toString().trim();
        if (email.isEmpty()) {
            business_email.setError(getString(R.string.error_email_required));
            business_email.requestFocus();
            return false;
        }
        else if (!TextUtilsHelper.isValidEmail(email)) {
            business_email.setError(getString(R.string.error_invalid_email));
            business_email.requestFocus();
            return false;
        }
        else {
            email_wrapper.setErrorEnabled(false);
        }

        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMerchantSignUpStepOneInteractionListener) {
            mListener = (OnMerchantSignUpStepOneInteractionListener) context;
            BusProvider.getInstance().register(this);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMerchantSignUpStepOneInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        BusProvider.getInstance().unregister(this);
    }

    public interface OnMerchantSignUpStepOneInteractionListener {
        void onMerchantSignUpStepOneInteraction(String uri);
    }
}
