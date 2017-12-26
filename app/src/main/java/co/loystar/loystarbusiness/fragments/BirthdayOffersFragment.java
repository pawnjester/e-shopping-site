package co.loystar.loystarbusiness.fragments;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.BirthdayOffer;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.ui.buttons.ActionButton;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BirthdayOffersFragment extends Fragment {
    private static final String TAG = BirthdayOffersFragment.class.getSimpleName();

    private BirthdayOfferEntity mBirthdayOffer;
    private MerchantEntity merchantEntity;
    private DatabaseManager mDatabaseManager;
    private ApiClient mApiClient;
    private ProgressDialog mProgressDialog;
    private View noBirthdayOfferView;
    private View birthdayOfferView;
    private TextView mOfferText;
    ActionButton mDeleteOffer;
    ActionButton mEditOffer;
    private View rootView;

    public BirthdayOffersFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_birthday_offers, container, false);

        if (getActivity() == null) {
            return rootView;
        }

        SessionManager sessionManager = new SessionManager(getActivity());
        mDatabaseManager = DatabaseManager.getInstance(getActivity());
        merchantEntity = mDatabaseManager.getMerchant(sessionManager.getMerchantId());

        noBirthdayOfferView = rootView.findViewById(R.id.noBirthdayOfferView);
        birthdayOfferView = rootView.findViewById(R.id.birthdayOfferCardView);
        mOfferText = rootView.findViewById(R.id.birthdayOfferText);
        BrandButtonNormal mCreateOffer = noBirthdayOfferView.findViewById(R.id.createBirthdayOffer);
        mDeleteOffer = birthdayOfferView.findViewById(R.id.deleteBirthdayOffer);
        mEditOffer = birthdayOfferView.findViewById(R.id.editBirthdayOffer);

        mApiClient = new ApiClient(getContext());
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.a_moment));

        mBirthdayOffer = mDatabaseManager.getMerchantBirthdayOffer(sessionManager.getMerchantId());
        if (mBirthdayOffer == null) {
            birthdayOfferView.setVisibility(View.GONE);
            noBirthdayOfferView.setVisibility(View.VISIBLE);

        } else {
            mOfferText.setText(mBirthdayOffer.getOfferDescription());
        }

        mDeleteOffer.setOnClickListener(view -> new AlertDialog.Builder(getActivity())
            .setTitle("Are you sure?")
            .setMessage("This offer will be deleted permanently.")
            .setPositiveButton(getString(R.string.confirm_delete_positive), (dialog, which) -> {
                dialog.dismiss();
                closeKeyboard();

                mProgressDialog.show();

                mApiClient.getLoystarApi(false).deleteBirthdayOffer(mBirthdayOffer.getId()).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                        if (mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }

                        if (response.isSuccessful() || response.code() == 404) {
                            assert merchantEntity != null;

                            merchantEntity.setBirthdayOffer(null);
                            mDatabaseManager.updateMerchant(merchantEntity);

                            birthdayOfferView.setVisibility(View.GONE);
                            noBirthdayOfferView.setVisibility(View.VISIBLE);
                            showSnackbar(R.string.birthday_offer_delete_success);
                        }
                        else {
                            showSnackbar(R.string.error_birthday_offer_delete);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        if (mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                        showSnackbar(R.string.error_internet_connection_timed_out);
                    }
                });
            })
            .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show());

        mEditOffer.setOnClickListener(view -> {

            LayoutInflater li = LayoutInflater.from(getActivity());
            @SuppressLint("InflateParams") View promptsView = li.inflate(R.layout.edit_birthday_offer, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

            alertDialogBuilder.setView(promptsView);

            final EditText userInput = promptsView
                .findViewById(R.id.birthday_offer_input);

            userInput.setText(mBirthdayOffer.getOfferDescription());

            alertDialogBuilder
                .setPositiveButton(getString(R.string.update_offer),
                    (dialog, id) -> {
                        if (userInput.getText().toString().isEmpty()) {
                            userInput.setError(getString(R.string.error_birthday_offer_text_required));
                            userInput.requestFocus();
                            return;
                        }

                        closeKeyboard();
                        mProgressDialog.show();

                        try {
                            JSONObject req = new JSONObject();
                            req.put("offer_description", userInput.getText().toString());
                            JSONObject requestData = new JSONObject();
                            requestData.put("data", req);

                            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                            mApiClient.getLoystarApi(false).updateBirthdayOffer(mBirthdayOffer.getId(), requestBody).enqueue(new Callback<BirthdayOffer>() {
                                @Override
                                public void onResponse(@NonNull Call<BirthdayOffer> call, @NonNull Response<BirthdayOffer> response) {
                                    if (mProgressDialog.isShowing()) {
                                        mProgressDialog.dismiss();
                                    }

                                    if (response.isSuccessful()) {
                                        BirthdayOffer birthdayOffer = response.body();
                                        if (birthdayOffer != null) {
                                            BirthdayOfferEntity birthdayOfferEntity = merchantEntity.getBirthdayOffer();
                                            birthdayOffer.setOffer_description(birthdayOffer.getOffer_description());
                                            birthdayOfferEntity.setCreatedAt(new Timestamp(birthdayOffer.getCreated_at().getMillis()));
                                            mDatabaseManager.updateBirthdayOffer(birthdayOfferEntity);

                                            mOfferText.setText(birthdayOffer.getOffer_description());
                                            showSnackbar(R.string.birthday_offer_update_success);
                                        }
                                    }
                                    else {
                                        showSnackbar(R.string.error_birthday_offer_update);
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<BirthdayOffer> call, @NonNull Throwable t) {
                                    if (mProgressDialog.isShowing()) {
                                        mProgressDialog.dismiss();
                                    }
                                    showSnackbar(R.string.error_internet_connection_timed_out);
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    })
                .setNegativeButton(android.R.string.no,
                    (dialog, id) -> dialog.cancel());

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        });

        mCreateOffer.setOnClickListener(view -> {
            LayoutInflater li = LayoutInflater.from(getContext());
            @SuppressLint("InflateParams") View promptsView = li.inflate(R.layout.create_birthday_offer, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

            alertDialogBuilder.setView(promptsView);

            final EditText userInput = promptsView
                .findViewById(R.id.birthday_offer_input);


            alertDialogBuilder
                .setPositiveButton(getString(R.string.create_offer),
                    (dialog, id) -> {
                        if (userInput.getText().toString().isEmpty()) {
                            userInput.setError(getString(R.string.error_birthday_offer_text_required));
                            userInput.requestFocus();
                            return;
                        }

                        closeKeyboard();
                        mProgressDialog.show();

                        try {
                            JSONObject req = new JSONObject();
                            req.put("offer_description", userInput.getText().toString());
                            JSONObject requestData = new JSONObject();
                            requestData.put("data", req);

                            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                            mApiClient.getLoystarApi(false).createBirthdayOffer(requestBody).enqueue(new Callback<BirthdayOffer>() {
                                @Override
                                public void onResponse(@NonNull Call<BirthdayOffer> call, @NonNull Response<BirthdayOffer> response) {
                                    if (mProgressDialog.isShowing()) {
                                        mProgressDialog.dismiss();
                                    }

                                    if (response.isSuccessful()) {
                                        BirthdayOffer birthdayOffer = response.body();
                                        if (birthdayOffer == null) {
                                            showSnackbar(R.string.unknown_error);
                                        } else {
                                            BirthdayOfferEntity birthdayOfferEntity = new BirthdayOfferEntity();
                                            birthdayOfferEntity.setId(birthdayOffer.getId());
                                            birthdayOfferEntity.setCreatedAt(new Timestamp(birthdayOffer.getCreated_at().getMillis()));
                                            birthdayOfferEntity.setOfferDescription(birthdayOffer.getOffer_description());
                                            birthdayOfferEntity.setUpdatedAt(new Timestamp(birthdayOffer.getUpdated_at().getMillis()));

                                            assert merchantEntity != null;
                                            merchantEntity.setBirthdayOffer(birthdayOfferEntity);
                                            mDatabaseManager.updateMerchant(merchantEntity);
                                            mBirthdayOffer = merchantEntity.getBirthdayOffer();

                                            noBirthdayOfferView.setVisibility(View.GONE);
                                            birthdayOfferView.setVisibility(View.VISIBLE);
                                            mOfferText.setText(birthdayOffer.getOffer_description());

                                            showSnackbar(R.string.birthday_offer_create_success);
                                        }
                                    }

                                    else {
                                        showSnackbar(R.string.error_birthday_offer_create);
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<BirthdayOffer> call, @NonNull Throwable t) {
                                    if (mProgressDialog.isShowing()) {
                                        mProgressDialog.dismiss();
                                    }
                                    showSnackbar(R.string.error_internet_connection_timed_out);
                                }
                            });


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    })
                .setNegativeButton(android.R.string.no,
                    (dialog, id) -> dialog.cancel());

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        });

        return rootView;
    }

    private void closeKeyboard() {
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

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(rootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
