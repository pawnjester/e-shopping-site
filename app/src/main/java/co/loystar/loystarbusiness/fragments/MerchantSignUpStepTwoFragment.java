package co.loystar.loystarbusiness.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.pojos.BusinessType;
import co.loystar.loystarbusiness.utils.BusinessTypesFetcher;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.Currency.Currency;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrencyPicker;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;

public class MerchantSignUpStepTwoFragment extends Fragment
        implements CurrencyPicker.OnCurrencySelectedListener{

    private OnMerchantSignUpStepTwoFragmentInteractionListener mListener;
    private View rootView;
    private String selectedBusinessType;
    private String selectedCurrency;
    private EditText merchantPasswordView;
    private EditText merchantConfirmPasswordView;
    private SharedPreferences sharedPref;

    public MerchantSignUpStepTwoFragment() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_merchant_sign_up_step_two, container, false);
        if (getActivity() == null) {
            return rootView;
        }
        sharedPref = getActivity().getSharedPreferences(getString(R.string.merchant_sign_up_pref), Context.MODE_PRIVATE);
        ArrayList<BusinessType> getBusinessTypes = BusinessTypesFetcher.getBusinessTypes(getActivity());
        final CharSequence[] businessTypeEntries = new CharSequence[getBusinessTypes.size()];
        for (int i = 0; i < getBusinessTypes.size(); i++) {
            businessTypeEntries[i] = getBusinessTypes.get(i).getTitle();
        }
        SpinnerButton businessTypeSpinner = rootView.findViewById(R.id.businessCategorySpinner);
        businessTypeSpinner.setEntries(businessTypeEntries);
        SpinnerButton.OnItemSelectedListener businessTypeSelectedListener = position -> selectedBusinessType = (String) businessTypeEntries[position];
        businessTypeSpinner.setListener(businessTypeSelectedListener);

        if (sharedPref.contains(Constants.BUSINESS_CATEGORY)) {
            businessTypeSpinner.setSelection(getBusinessTypes.indexOf(
                    BusinessTypesFetcher.getBusinessTypes(getActivity())
                            .getBusinessTypeByTitle(
                                    sharedPref.getString(Constants.BUSINESS_CATEGORY, "")
                            ))
            );
        }

        CurrencyPicker currencyPicker = rootView.findViewById(R.id.currencySpinner);
        currencyPicker.setListener(this);
        if (sharedPref.contains(Constants.CURRENCY)) {
            currencyPicker.setCurrencySelection(sharedPref.getString(Constants.CURRENCY, ""));
        }

        TextInputLayout merchantPasswordWrapper = rootView.findViewById(R.id.merchantPasswordWrapper);
        TextInputLayout merchantConfirmPasswordWrapper = rootView.findViewById(R.id.merchantConfirmPasswordWrapper);
        merchantPasswordWrapper.setPasswordVisibilityToggleEnabled(true);
        merchantConfirmPasswordWrapper.setPasswordVisibilityToggleEnabled(true);
        merchantPasswordView = rootView.findViewById(R.id.passwordView);
        merchantConfirmPasswordView = rootView.findViewById(R.id.confirmPasswordView);

        if (sharedPref.contains(Constants.PASSWORD)) {
            merchantPasswordView.setText(sharedPref.getString(Constants.PASSWORD, ""));
            merchantConfirmPasswordView.setText(sharedPref.getString(Constants.PASSWORD, ""));
        }

        RxView.clicks(rootView.findViewById(R.id.signUpStepTwoSubmit)).subscribe(o -> submitForm());

        TextView policyText = rootView.findViewById(R.id.privacyPolicyText);
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
        policyText.setTypeface(App.getInstance().getTypeface());

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMerchantSignUpStepTwoFragmentInteractionListener) {
            mListener = (OnMerchantSignUpStepTwoFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMerchantSignUpStepTwoFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void submitForm() {
        if (selectedBusinessType == null) {
            showSnackbar(R.string.error_business_category_required);
            return;
        }
        if (selectedCurrency == null) {
            showSnackbar(R.string.error_currency_required);
            return;
        }
        if (!isValidPassword(merchantPasswordView.getText().toString())) {
            merchantPasswordView.setError(getString(R.string.error_incorrect_password_extra));
            merchantPasswordView.requestFocus();
            return;
        }
        if (!isValidPassword(merchantConfirmPasswordView.getText().toString())) {
            merchantConfirmPasswordView.setError(getString(R.string.error_incorrect_password_extra));
            merchantConfirmPasswordView.requestFocus();
            return;
        }
        if (!merchantPasswordView.getText().toString().equals(merchantConfirmPasswordView.getText().toString())) {
            merchantConfirmPasswordView.setError(getString(R.string.error_passwords_mismatch));
            merchantConfirmPasswordView.requestFocus();
            return;
        }

        closeKeyBoard();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.BUSINESS_CATEGORY, selectedBusinessType);
        editor.putString(Constants.PASSWORD, merchantPasswordView.getText().toString());
        editor.putString(Constants.CURRENCY, selectedCurrency);
        editor.apply();

        mListener.onMerchantSignUpStepTwoFragmentInteraction();
    }

    private boolean isValidPassword(String pass) {
        return pass != null && pass.length() > 5;
    }

    @Override
    public void onCurrencySelected(Currency currency) {
        selectedCurrency = currency.getCode();
    }

    public interface OnMerchantSignUpStepTwoFragmentInteractionListener {
        void onMerchantSignUpStepTwoFragmentInteraction();
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(rootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
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

}
