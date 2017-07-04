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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBBirthdayOffer;
import co.loystar.loystarbusiness.models.db.DBBirthdayOfferPresetSMS;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 7/4/17.
 */

public class BirthdayMessageTextFragment extends Fragment {
    /*constants*/
    private static final String TAG = BirthdayMessageTextFragment.class.getSimpleName();

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private DBBirthdayOffer mBirthdayOffer;
    private ApiClient mApiClient;
    private boolean hasOffer = false;
    private DBBirthdayOfferPresetSMS mPresetTemplate;

    /*views*/
    private TextView charCounterView;
    private View noPresetTemplateView;
    private View presetTemplateView;
    private ProgressDialog mProgressDialog;
    private TextView mPresetTextView;
    private ImageView insertCustomerView;
    private ImageView insertOfferView;
    private Button resetFieldView;
    private TextInputEditText msgBox;

    public BirthdayMessageTextFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_birthday_message_text, container, false);

        noPresetTemplateView = rootView.findViewById(R.id.no_preset_template_layout);
        presetTemplateView = rootView.findViewById(R.id.fragment_birthday_messaging_preset_card_view);
        BrandButtonNormal mCreatePresetTemplate = (BrandButtonNormal) noPresetTemplateView.findViewById(R.id.create_preset_template);
        ImageView mEditPresetTemplate = (ImageView) rootView.findViewById(R.id.edit_preset_template_image_view);
        mPresetTextView = (TextView) rootView.findViewById(R.id.preset_text);

        mApiClient = new ApiClient(getContext());
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.a_moment));

        sessionManager = new SessionManager(getContext());
        mPresetTemplate = databaseHelper.getBirthdayPresetSMSByMerchantId(sessionManager.getMerchantId());
        mBirthdayOffer = databaseHelper.getBirthdayOfferByMerchantId(sessionManager.getMerchantId());

        if (mPresetTemplate == null) {
            noPresetTemplateView.setVisibility(View.GONE);
            presetTemplateView.setVisibility(View.VISIBLE);
        }
        else {
            mPresetTextView.setText(mPresetTemplate.getPreset_sms_text());
        }

        final TextWatcher mTextEditorWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String fullText = null;
                if (hasOffer) {
                    fullText = s.toString().replaceFirst("\\[(BIRTHDAY_OFFER)\\]", mBirthdayOffer.getOffer_description());
                }
                else {
                    fullText = s.toString();
                }
                double sms_char_length = 160;
                int sms_unit = (int) Math.ceil(fullText.length() / sms_char_length);
                String char_unit_temp = "%s %s/%s";
                String char_temp_unit = fullText.length() == 1 ? "char" : "chars";
                String text_template = "%s %s";
                String unit_text = sms_unit != 1 ? "units" : "unit";
                String sms_unit_text = String.format(text_template, sms_unit, unit_text);
                String char_unit_text = String.format(char_unit_temp, fullText.length(), char_temp_unit, sms_unit_text);
                charCounterView.setText(char_unit_text);
            }

            public void afterTextChanged(Editable s) {
            }
        };

        hasOffer = mBirthdayOffer != null;

        mEditPresetTemplate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(getContext());
                View promptsView = li.inflate(R.layout.birthday_preset_sms_template_layout, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getContext());

                alertDialogBuilder.setView(promptsView);

                msgBox = (TextInputEditText) promptsView.findViewById(R.id.msg_box);

                msgBox.setText(mPresetTemplate.getPreset_sms_text());

                insertCustomerView = (ImageView) promptsView.findViewById(R.id.insert_customer_name_img);
                insertOfferView = (ImageView) promptsView.findViewById(R.id.insert_offer_img);
                resetFieldView = (Button) promptsView.findViewById(R.id.reset_field);
                charCounterView = (TextView) promptsView.findViewById(R.id.char_counter);

                if (hasOffer) {
                    insertOfferView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            msgBox.getText().insert(msgBox.getSelectionStart(), "[BIRTHDAY_OFFER]");
                        }
                    });
                }
                else {
                    insertOfferView.setVisibility(View.INVISIBLE);
                    (promptsView.findViewById(R.id.insert_b_offer_placeholder_guide)).setVisibility(View.INVISIBLE);
                }

                resetFieldView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String preset_sms_text = null;
                        if (hasOffer) {
                            preset_sms_text = getResources().getString(R.string.default_birthday_preset_sms_with_offer, sessionManager.getBusinessName());
                        }
                        else {
                            preset_sms_text = getResources().getString(R.string.default_birthday_preset_sms_with_no_offer, sessionManager.getBusinessName());
                        }

                        msgBox.setText("");
                        msgBox.setText(preset_sms_text);
                    }
                });

                insertCustomerView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msgBox.getText().insert(msgBox.getSelectionStart(), "[CUSTOMER_NAME]");
                    }
                });

                msgBox.addTextChangedListener(mTextEditorWatcher);


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

                                            mApiClient.getLoystarApi().updateBirthdayOfferPresetSMS(String.valueOf(mPresetTemplate.getId()), requestBody).enqueue(new Callback<DBBirthdayOfferPresetSMS>() {
                                                @Override
                                                public void onResponse(Call<DBBirthdayOfferPresetSMS> call, Response<DBBirthdayOfferPresetSMS> response) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    if (response.isSuccessful()) {
                                                        DBBirthdayOfferPresetSMS birthdayOfferPresetSMS = response.body();
                                                        databaseHelper.insertOrReplaceBirthdayOfferPresetSMS(birthdayOfferPresetSMS);
                                                        mPresetTextView.setText(birthdayOfferPresetSMS.getPreset_sms_text());
                                                        Toast.makeText(getContext(), getString(R.string.birthday_offer_preset_sms_update_success), Toast.LENGTH_LONG).show();
                                                    }

                                                    else {
                                                        Toast.makeText(getContext(), getString(R.string.error_birthday_offer_preset_sms_update), Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<DBBirthdayOfferPresetSMS> call, Throwable t) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    Toast.makeText(getContext(), getString(R.string.error_internet_connection), Toast.LENGTH_LONG).show();
                                                    //Crashlytics.log(2, TAG, t.getMessage());
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
        });

        mCreatePresetTemplate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(getContext());
                View promptsView = li.inflate(R.layout.birthday_preset_sms_template_layout, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getContext());

                alertDialogBuilder.setView(promptsView);

                msgBox = (TextInputEditText) promptsView.findViewById(R.id.msg_box);

                insertCustomerView = (ImageView) promptsView.findViewById(R.id.insert_customer_name_img);
                insertOfferView = (ImageView) promptsView.findViewById(R.id.insert_offer_img);
                resetFieldView = (Button) promptsView.findViewById(R.id.reset_field);
                charCounterView = (TextView) promptsView.findViewById(R.id.char_counter);

                if (hasOffer) {
                    insertOfferView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            msgBox.getText().insert(msgBox.getSelectionStart(), "[BIRTHDAY_OFFER]");
                        }
                    });
                }
                else {
                    insertOfferView.setVisibility(View.INVISIBLE);
                    (promptsView.findViewById(R.id.insert_b_offer_placeholder_guide)).setVisibility(View.INVISIBLE);
                }

                resetFieldView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String preset_sms_text = null;
                        if (hasOffer) {
                            preset_sms_text = getResources().getString(R.string.default_birthday_preset_sms_with_offer, sessionManager.getBusinessName());
                        }
                        else {
                            preset_sms_text = getResources().getString(R.string.default_birthday_preset_sms_with_no_offer, sessionManager.getBusinessName());
                        }

                        msgBox.setText("");
                        msgBox.setText(preset_sms_text);
                    }
                });

                insertCustomerView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msgBox.getText().insert(msgBox.getSelectionStart(), "[CUSTOMER_NAME]");
                    }
                });

                msgBox.addTextChangedListener(mTextEditorWatcher);


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
                                            mApiClient.getLoystarApi().createBirthdayOfferPresetSMS(requestBody).enqueue(new Callback<DBBirthdayOfferPresetSMS>() {
                                                @Override
                                                public void onResponse(Call<DBBirthdayOfferPresetSMS> call, Response<DBBirthdayOfferPresetSMS> response) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    if (response.isSuccessful()) {
                                                        mPresetTemplate = response.body();
                                                        databaseHelper.insertBirthdayOfferPresetSMS(mPresetTemplate);

                                                        noPresetTemplateView.setVisibility(View.GONE);
                                                        presetTemplateView.setVisibility(View.VISIBLE);
                                                        mPresetTextView.setText(mPresetTemplate.getPreset_sms_text());

                                                    }
                                                    else {
                                                        Toast.makeText(getContext(), getString(R.string.error_birthday_offer_preset_sms_create), Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<DBBirthdayOfferPresetSMS> call, Throwable t) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    Toast.makeText(getContext(), getString(R.string.error_birthday_offer_preset_sms_create), Toast.LENGTH_LONG).show();
                                                    //Crashlytics.log(2, TAG, t.getMessage());
                                                }
                                            });

                                            mApiClient.getLoystarApi().updateBirthdayOfferPresetSMS(String.valueOf(mPresetTemplate.getId()), requestBody).enqueue(new Callback<DBBirthdayOfferPresetSMS>() {
                                                @Override
                                                public void onResponse(Call<DBBirthdayOfferPresetSMS> call, Response<DBBirthdayOfferPresetSMS> response) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    if (response.isSuccessful()) {
                                                        DBBirthdayOfferPresetSMS birthdayOfferPresetSMS = response.body();
                                                        databaseHelper.insertOrReplaceBirthdayOfferPresetSMS(birthdayOfferPresetSMS);
                                                        mPresetTextView.setText(birthdayOfferPresetSMS.getPreset_sms_text());
                                                        Toast.makeText(getContext(), getString(R.string.birthday_offer_preset_sms_update_success), Toast.LENGTH_LONG).show();
                                                    }

                                                    else {
                                                        Toast.makeText(getContext(), getString(R.string.error_birthday_offer_preset_sms_update), Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<DBBirthdayOfferPresetSMS> call, Throwable t) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }
                                                    Toast.makeText(getContext(), getString(R.string.error_internet_connection), Toast.LENGTH_LONG).show();
                                                    //Crashlytics.log(2, TAG, t.getMessage());
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
        });

        return rootView;
    }

    private boolean validatePresetMessage() {
        boolean msgIsValid = true;
        String text = msgBox.getText().toString();
        if (text.trim().isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_preset_sms_required), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getContext(), getString(R.string.error_preset_sms_special_strings_cname_not_found), Toast.LENGTH_LONG).show();
                    msgIsValid = false;
                }
            }
            else if (foundMatches.size() > 2) {
                Toast.makeText(getContext(), getString(R.string.error_preset_sms_special_strings_more_found), Toast.LENGTH_LONG).show();
                msgIsValid = false;
            }
        }
        else {
            Toast.makeText(getContext(), getString(R.string.error_preset_sms_special_strings_cname_not_found), Toast.LENGTH_LONG).show();
            msgIsValid = false;
        }

        return msgIsValid;
    }

    private void closeKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
