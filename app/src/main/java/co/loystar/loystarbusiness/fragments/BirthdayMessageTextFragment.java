package co.loystar.loystarbusiness.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.BirthdayOfferPresetSms;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.ui.buttons.ActionButton;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BirthdayMessageTextFragment extends Fragment {

    /*constants*/
    private static final String TAG = BirthdayMessageTextFragment.class.getSimpleName();

    private SessionManager sessionManager;
    private DatabaseManager mDatabaseManager;
    private BirthdayOfferEntity mBirthdayOffer;
    private ApiClient mApiClient;
    private BirthdayOfferPresetSmsEntity offerPresetSmsEntity;
    private MerchantEntity merchantEntity;

    /*views*/
    private TextView charCounterView;
    private ProgressDialog mProgressDialog;
    private TextView mPresetTextView;
    private ImageView insertCustomerView;
    private ImageView insertOfferView;
    private Button resetFieldView;
    private TextInputEditText msgBox;

    public BirthdayMessageTextFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthday_message_text, container, false);
        sessionManager = new SessionManager(getActivity());
        mDatabaseManager = DatabaseManager.getInstance(getActivity());
        merchantEntity = mDatabaseManager.getMerchant(sessionManager.getMerchantId());

        View birthdayMessageCardView = rootView.findViewById(R.id.birthdayMessageCardView);
        ActionButton birthdayMessageActionBtn = rootView.findViewById(R.id.birthdayMessageActionBtn);
        mPresetTextView = rootView.findViewById(R.id.presetText);

        mApiClient = new ApiClient(getContext());
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.a_moment));

        offerPresetSmsEntity = mDatabaseManager.getMerchantBirthdayOfferPresetSms(sessionManager.getMerchantId());
        mBirthdayOffer = mDatabaseManager.getMerchantBirthdayOffer(sessionManager.getMerchantId());

        if (offerPresetSmsEntity == null) {
            mPresetTextView.setText(getString(R.string.no_birthday_message));
            birthdayMessageCardView.setVisibility(View.VISIBLE);
        }
        else {
            mPresetTextView.setText(offerPresetSmsEntity.getPresetSmsText());
        }

        birthdayMessageActionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(getContext());
                View promptsView = li.inflate(R.layout.birthday_preset_sms, null);

                msgBox = promptsView.findViewById(R.id.msg_box);
                insertCustomerView = promptsView.findViewById(R.id.insertCustomerName);
                insertOfferView = promptsView.findViewById(R.id.insertBirthdayOffer);
                resetFieldView = promptsView.findViewById(R.id.resetBtn);
                charCounterView = promptsView.findViewById(R.id.charCounter);

                insertOfferView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msgBox.getText().insert(msgBox.getSelectionStart(), "[BIRTHDAY_OFFER]");
                    }
                });

                if (mBirthdayOffer == null) {
                    insertOfferView.setVisibility(View.GONE);
                    (promptsView.findViewById(R.id.birthdayOfferPlaceHolderLabel)).setVisibility(View.GONE);
                    (promptsView.findViewById(R.id.birthdayOfferPlaceHolderValue)).setVisibility(View.GONE);
                }

                resetFieldView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String presetSmsText = null;
                        if (mBirthdayOffer == null) {
                            presetSmsText = getResources().getString(R.string.default_birthday_preset_sms_with_no_offer, sessionManager.getBusinessName());
                        }
                        else {
                            presetSmsText = getResources().getString(R.string.default_birthday_preset_sms_with_offer, sessionManager.getBusinessName());
                        }

                        msgBox.setText("");
                        msgBox.setText(presetSmsText);
                    }
                });

                insertCustomerView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msgBox.getText().insert(msgBox.getSelectionStart(), "[CUSTOMER_NAME]");
                    }
                });

                final TextWatcher mTextEditorWatcher = new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String fullText;
                        if (mBirthdayOffer == null) {
                            fullText = s.toString();
                        }
                        else {
                            fullText = s.toString().replaceFirst("\\[(BIRTHDAY_OFFER)\\]", mBirthdayOffer.getOfferDescription());
                        }
                        double smsCharLength = 160;
                        int smsUnit = (int) Math.ceil(fullText.length() / smsCharLength);
                        String charUnitTemp = "%s %s/%s";
                        String charTempUnit = fullText.length() == 1 ? "char" : "chars";
                        String textTemplate = "%s %s";
                        String unitText = smsUnit != 1 ? "units" : "unit";
                        String smsUnitText = String.format(textTemplate, smsUnit, unitText);
                        String charUnitText = String.format(charUnitTemp, fullText.length(), charTempUnit, smsUnitText);
                        charCounterView.setText(charUnitText);
                    }

                    public void afterTextChanged(Editable s) {
                    }
                };

                msgBox.addTextChangedListener(mTextEditorWatcher);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getContext());
                alertDialogBuilder.setView(promptsView);

                if (offerPresetSmsEntity == null) {
                    String pText;
                    if (mBirthdayOffer == null) {
                        pText = getResources().getString(R.string.default_birthday_preset_sms_with_no_offer, sessionManager.getBusinessName());
                    }
                    else {
                        pText = getResources().getString(R.string.default_birthday_preset_sms_with_offer, sessionManager.getBusinessName());
                    }
                    msgBox.setText(pText);
                    alertDialogBuilder
                            .setPositiveButton(getString(R.string.create),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (!validatePresetMessage()) {
                                                return;
                                            }

                                            closeKeyboard();
                                            mProgressDialog.show();

                                            try {
                                                JSONObject req = new JSONObject();
                                                req.put("preset_sms_text", msgBox.getText().toString());
                                                JSONObject requestData = new JSONObject();
                                                requestData.put("data", req);

                                                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                                                mApiClient.getLoystarApi(false).createBirthdayOfferPresetSMS(requestBody).enqueue(new Callback<BirthdayOfferPresetSms>() {
                                                    @Override
                                                    public void onResponse(Call<BirthdayOfferPresetSms> call, Response<BirthdayOfferPresetSms> response) {
                                                        if (mProgressDialog.isShowing()) {
                                                            mProgressDialog.dismiss();
                                                        }

                                                        if (response.isSuccessful()) {
                                                            BirthdayOfferPresetSms birthdayOfferPresetSms = response.body();

                                                            BirthdayOfferPresetSmsEntity birthdayOfferPresetSmsEntity = new BirthdayOfferPresetSmsEntity();
                                                            birthdayOfferPresetSmsEntity.setId(birthdayOfferPresetSms.getId());
                                                            birthdayOfferPresetSmsEntity.setPresetSmsText(birthdayOfferPresetSms.getPreset_sms_text());
                                                            birthdayOfferPresetSmsEntity.setCreatedAt(new Timestamp(birthdayOfferPresetSms.getCreated_at().getMillis()));
                                                            birthdayOfferPresetSmsEntity.setUpdatedAt(new Timestamp(birthdayOfferPresetSms.getUpdated_at().getMillis()));

                                                            merchantEntity.setBirthdayOfferPresetSms(birthdayOfferPresetSmsEntity);
                                                            mDatabaseManager.updateMerchant(merchantEntity);
                                                            mPresetTextView.setText(birthdayOfferPresetSms.getPreset_sms_text());

                                                        }
                                                        else {
                                                            Toast.makeText(getContext(), getString(R.string.error_birthday_offer_preset_sms_create), Toast.LENGTH_LONG).show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Call<BirthdayOfferPresetSms> call, Throwable t) {
                                                        if (mProgressDialog.isShowing()) {
                                                            mProgressDialog.dismiss();
                                                        }
                                                        Toast.makeText(getContext(), getString(R.string.error_birthday_offer_preset_sms_create), Toast.LENGTH_LONG).show();
                                                    }
                                                });

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                    })
                            .setNegativeButton(android.R.string.no,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } else {
                    msgBox.setText(offerPresetSmsEntity.getPresetSmsText());
                    alertDialogBuilder
                            .setPositiveButton(getString(R.string.update),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (!validatePresetMessage()) {
                                                return;
                                            }

                                            closeKeyboard();
                                            mProgressDialog.show();

                                            try {
                                                JSONObject req = new JSONObject();
                                                req.put("preset_sms_text", msgBox.getText().toString());
                                                JSONObject requestData = new JSONObject();
                                                requestData.put("data", req);

                                                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                                                mApiClient.getLoystarApi(false).updateBirthdayOfferPresetSMS(String.valueOf(offerPresetSmsEntity.getId()), requestBody).enqueue(new Callback<BirthdayOfferPresetSms>() {
                                                    @Override
                                                    public void onResponse(Call<BirthdayOfferPresetSms> call, Response<BirthdayOfferPresetSms> response) {
                                                        if (mProgressDialog.isShowing()) {
                                                            mProgressDialog.dismiss();
                                                        }

                                                        if (response.isSuccessful()) {
                                                            BirthdayOfferPresetSms birthdayOfferPresetSms = response.body();
                                                            offerPresetSmsEntity.setPresetSmsText(birthdayOfferPresetSms.getPreset_sms_text());
                                                            offerPresetSmsEntity.setUpdatedAt(new Timestamp(birthdayOfferPresetSms.getUpdated_at().getMillis()));

                                                            mDatabaseManager.updateBirthdayPresetSms(offerPresetSmsEntity);
                                                            mPresetTextView.setText(birthdayOfferPresetSms.getPreset_sms_text());
                                                            Toast.makeText(getContext(), getString(R.string.birthday_offer_preset_sms_update_success), Toast.LENGTH_LONG).show();
                                                        }

                                                        else {
                                                            Toast.makeText(getContext(), getString(R.string.error_birthday_offer_preset_sms_update), Toast.LENGTH_LONG).show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Call<BirthdayOfferPresetSms> call, Throwable t) {
                                                        if (mProgressDialog.isShowing()) {
                                                            mProgressDialog.dismiss();
                                                        }
                                                        Toast.makeText(getContext(), getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();
                                                    }
                                                });

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                    })
                            .setNegativeButton(android.R.string.no,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        });

        return rootView;
    }

    private boolean validatePresetMessage() {
        boolean msgIsValid = true;
        String text = msgBox.getText().toString();
        if (text.trim().isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_message_required), Toast.LENGTH_LONG).show();
            msgIsValid = false;
        }
        Pattern MY_PATTERN = Pattern.compile("\\[(.*?)\\]");
        Matcher m = MY_PATTERN.matcher(text);
        ArrayList<String> foundMatches = new ArrayList<>();

        while (m.find()) {
            String s = m.group(1);
            if (s.equals("BIRTHDAY_OFFER") || s.equals("CUSTOMER_NAME")) {
                foundMatches.add(s);
            }
        }
        if (foundMatches.size() > 0) {
            if (foundMatches.size() == 1) {
                if (foundMatches.get(0).equals("BIRTHDAY_OFFER")) {
                    Toast.makeText(getContext(), getString(R.string.error_special_strings_customer_name_not_found), Toast.LENGTH_LONG).show();
                    msgIsValid = false;
                }
            }
            else if (foundMatches.size() > 2) {
                Toast.makeText(getContext(), getString(R.string.error_preset_sms_special_strings_more_found), Toast.LENGTH_LONG).show();
                msgIsValid = false;
            }
        }
        else {
            Toast.makeText(getContext(), getString(R.string.error_special_strings_customer_name_not_found), Toast.LENGTH_LONG).show();
            msgIsValid = false;
        }

        return msgIsValid;
    }

    private void closeKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

}
