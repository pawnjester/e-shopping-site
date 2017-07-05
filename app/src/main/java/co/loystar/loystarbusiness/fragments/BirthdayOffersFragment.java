package co.loystar.loystarbusiness.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBBirthdayOffer;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 7/4/17.
 */

public class BirthdayOffersFragment extends Fragment {
    /*constants*/
    private static final String TAG = BirthdayOffersFragment.class.getSimpleName();

    private DBBirthdayOffer birthdayOffer;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private ApiClient mApiClient;
    private ProgressDialog mProgressDialog;
    private View noBirthdayOfferView;
    private View birthdayOfferView;
    private TextView mOfferText;
    ImageView mDeleteOffer;
    ImageView mEditOffer;

    public BirthdayOffersFragment() {};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthday_offers, container, false);
        SessionManager sessionManager = new SessionManager(getContext());

        noBirthdayOfferView = rootView.findViewById(R.id.no_birthday_offer_layout);
        birthdayOfferView = rootView.findViewById(R.id.birthday_offer_card_view);
        mOfferText = (TextView) rootView.findViewById(R.id.offer_text);
        BrandButtonNormal mCreateOffer = (BrandButtonNormal) noBirthdayOfferView.findViewById(R.id.create_offer);
        mDeleteOffer = (ImageView) birthdayOfferView.findViewById(R.id.delete_offer_image_view);
        mEditOffer = (ImageView) birthdayOfferView.findViewById(R.id.edit_offer_image_view);

        mApiClient = new ApiClient(getContext());
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.a_moment));

        birthdayOffer = databaseHelper.getBirthdayOfferByMerchantId(sessionManager.getMerchantId());
        if (birthdayOffer == null) {
            birthdayOfferView.setVisibility(View.GONE);
            noBirthdayOfferView.setVisibility(View.VISIBLE);

        } else {
            mOfferText.setText(birthdayOffer.getOffer_description());
        }

        mDeleteOffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Are you sure?")
                        .setMessage("This offer will be deleted permanently.")
                        .setPositiveButton(getString(R.string.confirm_delete_positive), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                closeKeyboard();

                                mProgressDialog.show();

                                mApiClient.getLoystarApi().deleteBirthdayOffer(String.valueOf(birthdayOffer.getId())).enqueue(new Callback<ResponseBody>() {
                                    @Override
                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                                        if (mProgressDialog.isShowing()) {
                                            mProgressDialog.dismiss();
                                        }

                                        if (response.isSuccessful()) {
                                            databaseHelper.deleteBirthdayOffer(birthdayOffer);

                                            birthdayOfferView.setVisibility(View.GONE);
                                            noBirthdayOfferView.setVisibility(View.VISIBLE);

                                            Toast.makeText(getContext(), getString(R.string.birthday_offer_delete_success), Toast.LENGTH_LONG).show();
                                        }

                                        else {
                                            Toast.makeText(getContext(), getString(R.string.error_birthday_offer_delete), Toast.LENGTH_LONG).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                                        if (mProgressDialog.isShowing()) {
                                            mProgressDialog.dismiss();
                                        }

                                        Toast.makeText(getContext(), getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();

                                        //Crashlytics.log(2, TAG, t.getMessage());
                                    }
                                });
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        mEditOffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LayoutInflater li = LayoutInflater.from(getContext());
                View promptsView = li.inflate(R.layout.edit_birthday_offer_layout, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getContext());

                alertDialogBuilder.setView(promptsView);

                final EditText userInput = (EditText) promptsView
                        .findViewById(R.id.birthday_offer_input);

                userInput.setText(birthdayOffer.getOffer_description());

                alertDialogBuilder
                        .setPositiveButton(getString(R.string.update_offer),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
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
                                            mApiClient.getLoystarApi().updateBirthdayOffer(String.valueOf(birthdayOffer.getId()), requestBody).enqueue(new Callback<DBBirthdayOffer>() {
                                                @Override
                                                public void onResponse(Call<DBBirthdayOffer> call, Response<DBBirthdayOffer> response) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    if (response.isSuccessful()) {
                                                        DBBirthdayOffer dbBirthdayOffer = response.body();
                                                        databaseHelper.insertOrReplaceBirthdayOffer(dbBirthdayOffer);
                                                        mOfferText.setText(dbBirthdayOffer.getOffer_description());
                                                        Toast.makeText(getContext(), getString(R.string.birthday_offer_update_success), Toast.LENGTH_LONG).show();
                                                    }

                                                    else {
                                                        Toast.makeText(getContext(), getString(R.string.error_birthday_offer_update), Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<DBBirthdayOffer> call, Throwable t) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    Toast.makeText(getContext(), getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();
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

        mCreateOffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(getContext());
                View promptsView = li.inflate(R.layout.create_birthday_offer_layout, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getContext());

                alertDialogBuilder.setView(promptsView);

                final EditText userInput = (EditText) promptsView
                        .findViewById(R.id.birthday_offer_input);


                alertDialogBuilder
                        .setPositiveButton(getString(R.string.create_offer),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
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
                                            mApiClient.getLoystarApi().createBirthdayOffer(requestBody).enqueue(new Callback<DBBirthdayOffer>() {
                                                @Override
                                                public void onResponse(Call<DBBirthdayOffer> call, Response<DBBirthdayOffer> response) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    if (response.isSuccessful()) {
                                                        birthdayOffer = response.body();
                                                        databaseHelper.insertBirthdayOffer(birthdayOffer);

                                                        noBirthdayOfferView.setVisibility(View.GONE);
                                                        birthdayOfferView.setVisibility(View.VISIBLE);
                                                        mOfferText.setText(birthdayOffer.getOffer_description());

                                                        Toast.makeText(getContext(), getString(R.string.birthday_offer_create_success), Toast.LENGTH_LONG).show();
                                                    }

                                                    else {
                                                        Toast.makeText(getContext(), getString(R.string.error_birthday_offer_create), Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<DBBirthdayOffer> call, Throwable t) {
                                                    if (mProgressDialog.isShowing()) {
                                                        mProgressDialog.dismiss();
                                                    }

                                                    Toast.makeText(getContext(), getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();
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

    private void closeKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
