package co.loystar.loystarbusiness.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.BusinessType;
import co.loystar.loystarbusiness.models.BusinessTypesFetcher;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput.CurrencySearchableSpinner;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialog;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogEntity;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogOnItemSelectedListener;

/**
 * Created by ordgen on 7/4/17.
 */

public class MerchantSignUpFinalStepFragment extends Fragment implements SingleChoiceSpinnerDialogOnItemSelectedListener {
    private OnMerchantSignUpFinalFragmentInteractionListener mListener;
    private EditText merchantPassword;
    private String businessTypeSelectedItem = "";
    private EditText merchantConfirmPassword;
    private View mLayout;
    private CurrencySearchableSpinner currencySpinner;

    public MerchantSignUpFinalStepFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_merchant_sign_up_final_step, container, false);
        mLayout = rootView.findViewById(R.id.sign_up_step_two_content);


        SingleChoiceSpinnerDialog businessTypeSpinner = (SingleChoiceSpinnerDialog) rootView.findViewById(R.id.business_type_spinner);
        ArrayList<BusinessType> businessTypes = BusinessTypesFetcher.getBusinessTypes(getContext());
        ArrayList<SingleChoiceSpinnerDialogEntity> spinnerItems = new ArrayList<>();
        for (BusinessType businessType: businessTypes) {
            SingleChoiceSpinnerDialogEntity entity = new SingleChoiceSpinnerDialogEntity(businessType.getTitle(), (long) businessType.getId());
            spinnerItems.add(entity);
        }


        businessTypeSpinner.setItems(spinnerItems, "Select One", this, "Select Category", null, null, "", "");

        currencySpinner = (CurrencySearchableSpinner) rootView.findViewById(R.id.currency_spinner);

        Button completeButton = (Button) rootView.findViewById(R.id.complete_signup_btn);
        TextInputLayout merchantPasswordWrapper = (TextInputLayout) rootView.findViewById(R.id.merchant_password_wrapper);
        TextInputLayout merchantConfirmPasswordWrapper = (TextInputLayout) rootView.findViewById(R.id.merchant_confirm_password_wrapper);
        merchantPasswordWrapper.setPasswordVisibilityToggleEnabled(true);
        merchantConfirmPasswordWrapper.setPasswordVisibilityToggleEnabled(true);
        merchantPassword = (EditText) rootView.findViewById(R.id.merchant_password);
        merchantConfirmPassword = (EditText) rootView.findViewById(R.id.merchant_cpassword);


        completeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateForm();
            }
        });
        TextView policyText = (TextView) rootView.findViewById(R.id.privacy_policy_txt);

        SpannableString spannableString = new SpannableString(getString(R.string.privacy_policy_txt));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://loystar.co/loystar-privacy-policy/"));
                startActivity(browserIntent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };
        spannableString.setSpan(clickableSpan, 38, 52, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        policyText.setText(spannableString);
        policyText.setMovementMethod(LinkMovementMethod.getInstance());
        policyText.setHighlightColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
        policyText.setTypeface(LoystarApplication.getInstance().getTypeface());
        return rootView;
    }

    private void validateForm() {
        if (TextUtils.isEmpty(businessTypeSelectedItem)) {
            Snackbar.make(mLayout, R.string.error_business_category_required,
                    Snackbar.LENGTH_LONG)
                    .show();
            return;
        }
        if (!currencySpinner.isValid()) {
            Snackbar.make(mLayout, R.string.error_currency_required,
                    Snackbar.LENGTH_LONG)
                    .show();
            return;
        }
        if (!isValidPassword(merchantPassword.getText().toString())) {
            merchantPassword.setError(getString(R.string.error_incorrect_password_extra));
            merchantPassword.requestFocus();
            return;
        }
        if (!isValidPassword(merchantConfirmPassword.getText().toString())) {
            merchantConfirmPassword.setError(getString(R.string.error_incorrect_password_extra));
            merchantConfirmPassword.requestFocus();
            return;
        }
        if (!merchantPassword.getText().toString().equals(merchantConfirmPassword.getText().toString())) {
            merchantConfirmPassword.setError(getString(R.string.error_passwords_mismatch));
            merchantConfirmPassword.requestFocus();
            return;
        }
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        submitForm();
    }

    private boolean isValidPassword(String pass) {
        return pass != null && pass.length() > 5;
    }

    private void submitForm() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("business_type", businessTypeSelectedItem);
        editor.putString("password", merchantPassword.getText().toString());
        editor.putString("currency", currencySpinner.getCurrencyCode());
        editor.apply();
        mListener.onMerchantSignUpFinalFragmentInteraction(getString(R.string.go));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMerchantSignUpFinalFragmentInteractionListener) {
            mListener = (OnMerchantSignUpFinalFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMerchantSignUpFinalFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void itemSelectedId(Long Id) {
        BusinessType businessType = BusinessTypesFetcher.getBusinessTypes(getContext()).getBusinessTypeById(Integer.parseInt(String.valueOf(Id)));
        if (businessType != null) {
            businessTypeSelectedItem = businessType.getTitle();
        }
    }

    public interface OnMerchantSignUpFinalFragmentInteractionListener {
        void onMerchantSignUpFinalFragmentInteraction(String uri);
    }
}
