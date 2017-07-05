package co.loystar.loystarbusiness.activities;


import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.ConfirmMMPaymentResponse;
import co.loystar.loystarbusiness.api.pojos.MerchantSmsBalanceResponse;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.GeneralUtils;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TimeUtils;
import io.smooch.ui.ConversationActivity;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */

public class SettingsActivity extends AppCompatPreferenceActivity {
    public static final String TAG = SettingsActivity.class.getSimpleName();
    private static ApiClient mApiClient;
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            SessionManager sessionManager = new SessionManager(preference.getContext());
            DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
            DBMerchant merchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference.getKey().equals(preference.getContext().getString(R.string.pref_pay_subscription_key))) {
                if (merchant != null) {
                    if (merchant.getSubscription_expires_on() != null) {
                        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
                        Date currentDate = TimeUtils.getCurrentDateAndTime();
                        Date subscriptionExpiryDate = merchant.getSubscription_expires_on();
                        if (subscriptionExpiryDate.after(currentDate)) {
                            String tmt = "Your account is active and will expire on %s.";
                            String activeTxt = String.format(tmt, df.format(subscriptionExpiryDate.getTime()));
                            preference.setSummary(activeTxt);
                        } else {
                            String tmt = "Your account is inactive since %s. Click below to pay subscription and unlock the full features of Loystar.";
                            String inActiveTxt = String.format(tmt, df.format(subscriptionExpiryDate.getTime()));
                            preference.setSummary(inActiveTxt);

                        }
                    }
                }
            } else if (preference.getKey().equals(preference.getContext().getString(R.string.pref_app_version_key))) {
                preference.setSummary(BuildConfig.VERSION_NAME);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };



    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                if (onIsMultiPane()) {
                    navigateUpTo(new Intent(this, MerchantBackOffice.class));
                }
                else {
                    NavUtils.navigateUpFromSameTask(this);
                }
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onIsMultiPane() {
        return GeneralUtils.isXLargeTablet(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApiClient = new ApiClient(this);

        ListView listView = getListView();
        listView.setDivider(ContextCompat.getDrawable(this, R.drawable.line_divider));
        listView.setDividerHeight(2);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (onIsMultiPane()) {
            navigateUpTo(new Intent(this, MerchantBackOffice.class));
        }
        else {
            NavUtils.navigateUpFromSameTask(this);
        }
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || SettingsFragment.class.getName().equals(fragmentName);
    }


    @RuntimePermissions
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final SessionManager sessionManager = new SessionManager(getActivity());
            final DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();


            String settings = getArguments().getString("settings");
            if (getActivity().getString(R.string.settings_account).equals(settings)) {
                if (sessionManager.getMerchantCurrency().equals("GHS")) {

                    addPreferencesFromResource(R.xml.pref_account_settings);
                    setHasOptionsMenu(true);

                    Preference pay_subscription_preference = findPreference(getString(R.string.pref_pay_subscription_key));
                    pay_subscription_preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent subsIntent = new Intent(getActivity(), PaySubscription.class);
                            startActivity(subsIntent);
                            return true;
                        }
                    });

                    bindPreferenceSummaryToValue(pay_subscription_preference);

                    Preference pref_edit_account_pref = findPreference(getString(R.string.pref_edit_account_key));
                    pref_edit_account_pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent editAccountIntent = new Intent(getActivity(), EditAccount.class);
                            startActivity(editAccountIntent);
                            return true;
                        }
                    });


                    Preference pref_check_sms_bal = findPreference(getString(R.string.pref_check_sms_bal_key));
                    pref_check_sms_bal.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            CheckSmsBalance(getActivity());
                            return true;
                        }
                    });


                    Preference pref_confirm_mm_payment = findPreference(getString(R.string.pref_confirm_mm_payment_key));
                    pref_confirm_mm_payment.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            ConfirmMobileMoneyPayment(getActivity());
                            return true;
                        }
                    });

                    Preference pref_sign_out = findPreference(getString(R.string.pref_sign_out_key));
                    pref_sign_out.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Are you sure you want to sign out?")
                                    .setPositiveButton(getString(R.string.logout), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            sessionManager.logoutMerchant();

                                        }
                                    })
                                    .setNeutralButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                            return true;
                        }
                    });
                }
                else {
                    addPreferencesFromResource(R.xml.acc_settings_without_confirm_payment);
                    setHasOptionsMenu(true);

                    Preference pay_subscription_preference = findPreference(getString(R.string.pref_pay_subscription_key));
                    pay_subscription_preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent subsIntent = new Intent(getActivity(), PaySubscription.class);
                            startActivity(subsIntent);
                            return true;
                        }
                    });

                    bindPreferenceSummaryToValue(pay_subscription_preference);

                    Preference pref_edit_account_pref = findPreference(getString(R.string.pref_edit_account_key));
                    pref_edit_account_pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent editAccountIntent = new Intent(getActivity(), EditAccount.class);
                            startActivity(editAccountIntent);
                            return true;
                        }
                    });


                    Preference pref_check_sms_bal = findPreference(getString(R.string.pref_check_sms_bal_key));
                    pref_check_sms_bal.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            CheckSmsBalance(getActivity());
                            return true;
                        }
                    });


                    Preference pref_sign_out = findPreference(getString(R.string.pref_sign_out_key));
                    pref_sign_out.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Are you sure you want to sign out?")
                                    .setPositiveButton(getString(R.string.logout), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            sessionManager.logoutMerchant();

                                        }
                                    })
                                    .setNeutralButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                            return true;
                        }
                    });
                }

            } else if (getActivity().getString(R.string.settings_general).equals(settings)) {
                addPreferencesFromResource(R.xml.pref_general_settings);
                setHasOptionsMenu(true);
                bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_app_version_key)));

                Preference pref_privacy_policy = findPreference(getString(R.string.pref_privacy_policy_key));
                pref_privacy_policy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://loystar.co/loystar-privacy-policy/"));
                        startActivity(browserIntent);
                        return true;
                    }
                });

            }
            else if (getActivity().getString(R.string.pref_support_key).equals(settings)) {
                if (!GeneralUtils.isXLargeTablet(getActivity())) {
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    startActivity(intent);
                }

            }else if (getActivity().getString(R.string.settings_pos).equals(settings)) {
                addPreferencesFromResource(R.xml.pref_pos);
                setHasOptionsMenu(true);

                final Preference turn_on_pos_pref = findPreference(getString(R.string.pref_turn_on_pos_key));
                final SharedPreferences tSharedPref = turn_on_pos_pref.getSharedPreferences();
                boolean isTurnedOn = tSharedPref.getBoolean(turn_on_pos_pref.getKey(), false);
                if (!isTurnedOn) {
                    turn_on_pos_pref.setSummary(getString(R.string.pos_turned_off_explanation));
                } else {
                    turn_on_pos_pref.setSummary(getString(R.string.pos_turned_on_explanation));
                }

                turn_on_pos_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(final Preference preference, Object newValue) {
                        boolean isTurnedOn = (Boolean) newValue;
                        final DBMerchant dbMerchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());

                        if (!isTurnedOn) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Are you sure?")
                                    .setMessage("Loystar won't capture product information when recording sales.")
                                    .setPositiveButton(getString(R.string.turn_off), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            TwoStatePreference statePreference = (TwoStatePreference) turn_on_pos_pref;
                                            statePreference.setChecked(false);
                                            dbMerchant.setUpdate_required(true);
                                            dbMerchant.setTurn_on_point_of_sale(false);
                                            databaseHelper.updateMerchant(dbMerchant);

                                            SettingsFragmentPermissionsDispatcher.syncDataWithCheck(SettingsFragment.this, sessionManager.getMerchantEmail());
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        } else {
                            dbMerchant.setUpdate_required(true);
                            dbMerchant.setTurn_on_point_of_sale(true);
                            databaseHelper.updateMerchant(dbMerchant);
                            Toast.makeText(getActivity(), getActivity().getString(R.string.pos_turn_on_notice), Toast.LENGTH_LONG).show();
                            turn_on_pos_pref.setSummary(getString(R.string.pos_turned_on_explanation));

                            SettingsFragmentPermissionsDispatcher.syncDataWithCheck(SettingsFragment.this, sessionManager.getMerchantEmail());
                        }
                        return isTurnedOn;
                    }
                });

                Preference pref_my_products = findPreference(getString(R.string.pref_my_products_key));
                pref_my_products.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent ProductsViewIntent = new Intent(getActivity(), ProductsActivity.class);
                        startActivity(ProductsViewIntent);
                        return true;
                    }
                });

                Preference pref_product_categories = findPreference(getString(R.string.pref_product_categories_key));
                pref_product_categories.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent categoriesIntent = new Intent(getActivity(), ProductCategoriesActivity.class);
                        startActivity(categoriesIntent);
                        return true;
                    }
                });
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            SettingsFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
        }

        @SuppressWarnings("MissingPermission")
        @NeedsPermission(Manifest.permission.GET_ACCOUNTS)
        public void syncData(String merchantEmail) {
            Account account = null;
            AccountManager accountManager = AccountManager.get(getActivity());
            Account[] accounts = accountManager.getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
            for (Account acc : accounts) {
                if (acc.name.equals(merchantEmail)) {
                    account = acc;
                }
            }
            if (account != null) {
                SyncAdapter.syncImmediately(account);
            }
        }

        @OnShowRationale(Manifest.permission.GET_ACCOUNTS)
        void showRationaleForGetAccounts(final PermissionRequest request) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_get_accounts_rationale)
                    .setPositiveButton(R.string.buttonc_allow, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            request.proceed();
                        }
                    })
                    .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            request.cancel();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

        @OnPermissionDenied(Manifest.permission.GET_ACCOUNTS)
        void showDeniedForGetAccounts() {
            Toast.makeText(getActivity(), R.string.permission_accounts_denied, Toast.LENGTH_SHORT).show();
        }

        @OnNeverAskAgain(Manifest.permission.GET_ACCOUNTS)
        void showNeverAskForGetAccounts() {
            Toast.makeText(getActivity(), R.string.permission_accounts_neverask, Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                if (GeneralUtils.isXLargeTablet(getActivity())) {
                    startActivity(new Intent(getActivity(), MerchantBackOffice.class));
                }
                else {
                    startActivity(new Intent(getActivity(), SettingsActivity.class));
                }
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        super.onHeaderClick(header, position);
        if (header.id == R.id.birthday_messages_and_offers) {
            Intent birthdayIntent = new Intent(this, BirthdayOffersAndMessagingActivity.class);
            birthdayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(birthdayIntent);
        }
        else if (header.id == R.id.pref_loyalty_programs) {
            Intent intent = new Intent(this, LoyaltyProgramsListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else if (header.id == R.id.loystar_support_pref_header) {
            ConversationActivity.show(this);
        }
    }

    private static void ConfirmMobileMoneyPayment(final Context mContext) {
        SessionManager sessionManager = new SessionManager(mContext);
        final DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
        final DBMerchant merchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());
        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage("Please wait confirming payment...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        mApiClient.getLoystarApi().confirmMobileMoneyPayment().enqueue(new Callback<ConfirmMMPaymentResponse>() {
            @Override
            public void onResponse(Call<ConfirmMMPaymentResponse> call, Response<ConfirmMMPaymentResponse> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (response.isSuccessful()) {
                    ConfirmMMPaymentResponse confirmMMPaymentResponse = response.body();
                    String status = confirmMMPaymentResponse.getStatus();
                    Log.e("REQ", "CODE: " + status);
                    switch (status) {
                        case "success":
                            merchant.setSubscription_expires_on(confirmMMPaymentResponse.getSubscriptionExpiresOn());
                            merchant.setSubscription_plan(confirmMMPaymentResponse.getSubscriptionPlan());
                            databaseHelper.updateMerchant(merchant);
                            showPaymentConfirmationDialog(mContext, "success");
                            break;
                        case "payment already processed":
                            showPaymentConfirmationDialog(mContext, "already_processed");
                            break;
                        default:
                            showPaymentConfirmationDialog(mContext, "failed");
                            break;
                    }
                } else {
                    showPaymentConfirmationDialog(mContext, "failed");
                }
            }

            @Override
            public void onFailure(Call<ConfirmMMPaymentResponse> call, Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                showPaymentConfirmationDialog(mContext, "failed");
                Toast.makeText(mContext, mContext.getString(R.string.something_went_wrong), Toast.LENGTH_LONG).show();
                //Crashlytics.log(2, TAG, t.getMessage());
            }
        });
    }

    private static void showPaymentConfirmationDialog(Context context, String type) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Payment Status");
        alertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        switch (type) {
            case "success":
                alertDialogBuilder.setMessage(context.getString(R.string.transaction_success));
                AlertDialog scDialog = alertDialogBuilder.create();
                scDialog.show();
                break;
            case "already_processed":
                alertDialogBuilder.setMessage(context.getString(R.string.transaction_already_processed));
                AlertDialog prDialog = alertDialogBuilder.create();
                prDialog.show();
                break;
            case "failed":
                alertDialogBuilder.setMessage(context.getString(R.string.transaction_failed));
                AlertDialog failedDialog = alertDialogBuilder.create();
                failedDialog.show();
                break;
        }
    }

    private static void CheckSmsBalance(final Context mContext) {
        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage("Please wait! Checking sms balance...");
        progressDialog.show();

        mApiClient.getLoystarApi().getSmsBalance().enqueue(new Callback<MerchantSmsBalanceResponse>() {
            @Override
            public void onResponse(Call<MerchantSmsBalanceResponse> call, Response<MerchantSmsBalanceResponse> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (response.isSuccessful()) {
                    MerchantSmsBalanceResponse smsBalanceResponse = response.body();
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

                    String tmt = "Your Total SMS Balance is: %s ";
                    String message_text = String.format(tmt, smsBalanceResponse.getBalance());
                    alertDialogBuilder
                            .setMessage(message_text)
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                } else {
                    Toast.makeText(mContext, mContext.getString(R.string.something_went_wrong), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<MerchantSmsBalanceResponse> call, Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(mContext, mContext.getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();
                //Crashlytics.log(2, TAG, t.getMessage());
            }
        });
    }
}
