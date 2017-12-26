package co.loystar.loystarbusiness.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.jakewharton.rxbinding2.view.RxView;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.databinders.EmailAvailability;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput.InternationalPhoneInput;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MerchantSignUpStepOneFragment extends Fragment {

    private static final String TAG = MerchantSignUpStepOneFragment.class.getSimpleName();
    private OnMerchantSignUpStepOneFragmentInteractionListener mListener;

    private View rootView;
    private SharedPreferences sharedPref;
    private EditText businessNameView;
    private InternationalPhoneInput businessPhoneView;
    private EditText businessEmailView;
    private ProgressBar checkEmailSpinner;
    private EditText firstNameView;
    private ProgressDialog progressDialog;

    public MerchantSignUpStepOneFragment() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_merchant_sign_up_step_one, container, false);
        if (getActivity() == null) {
            return rootView;
        }
        sharedPref = getActivity().getSharedPreferences(getString(R.string.merchant_sign_up_pref), Context.MODE_PRIVATE);
        firstNameView = rootView.findViewById(R.id.firstName);
        businessNameView = rootView.findViewById(R.id.businessName);
        businessPhoneView = rootView.findViewById(R.id.businessPhone);
        businessEmailView = rootView.findViewById(R.id.businessEmail);
        checkEmailSpinner = rootView.findViewById(R.id.checkEmailProgress);
        if (getArguments() != null) {
            businessPhoneView.setNumber(getArguments().getString(Constants.PHONE_NUMBER, ""));
        }
        businessPhoneView.setEnabled(false);

        if (sharedPref.contains(Constants.FIRST_NAME)) {
            firstNameView.setText(sharedPref.getString(Constants.FIRST_NAME, ""));
        }

        if (sharedPref.contains(Constants.BUSINESS_NAME)) {
            businessNameView.setText(sharedPref.getString(Constants.BUSINESS_NAME, ""));
        }

        if (sharedPref.contains(Constants.BUSINESS_EMAIL)) {
            businessEmailView.setText(sharedPref.getString(Constants.BUSINESS_EMAIL, ""));
        }

        RxView.clicks(rootView.findViewById(R.id.haveAccountView)).subscribe(o -> {
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        });
        RxView.clicks(rootView.findViewById(R.id.signUpStepOneSubmit)).subscribe(o -> submitForm());
        return rootView;
    }

    private void submitForm() {
        final String firstName = firstNameView.getText().toString();
        final String businessName = businessNameView.getText().toString();
        final String businessPhone = businessPhoneView.getNumber();
        final String businessEmail = businessEmailView.getText().toString();
        if (TextUtils.isEmpty(firstName)) {
            firstNameView.setError(getString(R.string.error_first_name_required));
            firstNameView.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(businessName)) {
            businessNameView.setError(getString(R.string.error_business_name_required));
            businessNameView.requestFocus();
            return;
        }
        if (!isValidEmail(businessEmail)) {
            return;
        }

        closeKeyBoard();

        ApiClient apiClient = new ApiClient(getActivity());
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.a_moment));
        progressDialog.show();

        checkEmailSpinner.setVisibility(View.VISIBLE);
        apiClient.getLoystarApi(false).checkMerchantEmailAvailability(businessEmail).enqueue(new Callback<EmailAvailability>() {
            @Override
            public void onResponse(@NonNull Call<EmailAvailability> call, @NonNull Response<EmailAvailability> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                checkEmailSpinner.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    EmailAvailability emailAvailability = response.body();
                    if (emailAvailability == null) {
                        showSnackbar(R.string.unknown_error);
                    } else {
                        if (emailAvailability.isEmailAvailable()) {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(Constants.BUSINESS_NAME, businessName);
                            editor.putString(Constants.BUSINESS_EMAIL, businessEmail);
                            editor.putString(Constants.PHONE_NUMBER, businessPhone);
                            editor.putString(Constants.FIRST_NAME, firstName);
                            editor.apply();
                            mListener.onMerchantSignUpStepOneFragmentInteraction();
                        } else {
                            businessEmailView.setError(getString(R.string.error_email_not_unique));
                            businessEmailView.requestFocus();
                        }
                    }
                } else {
                    showSnackbar(R.string.unknown_error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<EmailAvailability> call, @NonNull Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                checkEmailSpinner.setVisibility(View.GONE);
                showSnackbar(R.string.error_internet_connection_timed_out);
            }
        });
    }

    private boolean isValidEmail(String email) {
        if (email.isEmpty()) {
            businessEmailView.setError(getString(R.string.error_email_required));
            businessEmailView.requestFocus();
            return false;
        }
        else if (!TextUtilsHelper.isValidEmailAddress(email)) {
            businessEmailView.setError(getString(R.string.error_invalid_email));
            businessEmailView.requestFocus();
            return false;
        }

        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMerchantSignUpStepOneFragmentInteractionListener) {
            mListener = (OnMerchantSignUpStepOneFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMerchantSignUpStepOneFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void closeKeyBoard() {
        if (getActivity() == null) {
            return;
        }
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public interface OnMerchantSignUpStepOneFragmentInteractionListener {
        void onMerchantSignUpStepOneFragmentInteraction();
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(rootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

}
