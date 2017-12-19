package co.loystar.loystarbusiness.activities;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
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
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.SmsBalance;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import io.smooch.ui.ConversationActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        SessionManager sessionManager = new SessionManager(preference.getContext());
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(preference.getContext());
        MerchantEntity merchant = mDatabaseManager.getMerchant(sessionManager.getMerchantId());
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

        } else if (preference instanceof RingtonePreference) {
            // For ringtone preferences, look up the correct display value
            // using RingtoneManager.
            if (TextUtils.isEmpty(stringValue)) {
                // Empty values correspond to 'silent' (no ringtone).
                preference.setSummary(R.string.pref_ringtone_silent);

            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(
                        preference.getContext(), Uri.parse(stringValue));

                if (ringtone == null) {
                    // Clear the summary if there was a lookup error.
                    preference.setSummary(null);
                } else {
                    // Set the summary to reflect the new ringtone display
                    // name.
                    String name = ringtone.getTitle(preference.getContext());
                    preference.setSummary(name);
                }
            }

        } else if (preference.getKey().equals(preference.getContext().getString(R.string.pref_pay_subscription_key))) {
            if (merchant != null) {
                DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
                Date expiryDate = AccountGeneral.accountExpiry(preference.getContext());
                if (expiryDate == null) {
                    String inActiveTxt = "Your account has expired. Click to pay subscription and unlock the full features of Loystar.";
                    preference.setSummary(inActiveTxt);
                } else {
                    if (AccountGeneral.isAccountActive(preference.getContext())) {
                        String tmt = "Your account is active and will expire on %s.";
                        String activeTxt = String.format(
                                tmt,
                                df.format(expiryDate)
                        );
                        preference.setSummary(activeTxt);
                    } else {
                        String tmt = "Your account has expired since %s. Click to pay subscription and unlock the full features of Loystar.";
                        String inActiveTxt = String.format(tmt, df.format(expiryDate));
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
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        ListView listView = getListView();
        listView.setDivider(ContextCompat.getDrawable(this, R.drawable.line_divider));
        listView.setDividerHeight(2);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || ProductsAndServices.class.getName().equals(fragmentName)
                || AccountPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_app_version_key)));
            Preference prefPrivacyPolicy = findPreference(getString(R.string.pref_privacy_policy_key));
            prefPrivacyPolicy.setOnPreferenceClickListener(preference -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://loystar.co/loystar-privacy-policy/"));
                startActivity(browserIntent);
                return true;
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AccountPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_account);
            setHasOptionsMenu(true);

            final SessionManager sessionManager = new SessionManager(getActivity());

            Preference paySubscriptionPreference = findPreference(getString(R.string.pref_pay_subscription_key));
            paySubscriptionPreference.setOnPreferenceClickListener(preference -> {
                Intent subsIntent = new Intent(getActivity(), PaySubscriptionActivity.class);
                startActivity(subsIntent);
                return true;
            });

            bindPreferenceSummaryToValue(paySubscriptionPreference);

            Preference prefEditAccountPref = findPreference(getString(R.string.pref_edit_account_key));
            prefEditAccountPref.setOnPreferenceClickListener(preference -> {
                Intent editAccountIntent = new Intent(getActivity(), MyAccountProfileActivity.class);
                startActivity(editAccountIntent);
                return true;
            });


            Preference prefCheckSmsBal = findPreference(getString(R.string.pref_check_sms_bal_key));
            prefCheckSmsBal.setOnPreferenceClickListener(preference -> {
                checkSmsBalance(getActivity());
                return true;
            });

            Preference prefSignOut = findPreference(getString(R.string.pref_sign_out_key));
            prefSignOut.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Are you sure you want to sign out?")
                        .setPositiveButton(getString(R.string.sign_out), (dialog, which) -> sessionManager.signOutMerchant(getActivity()))
                        .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
            });

            Preference prefSupport = findPreference(getString(R.string.pref_support_key));
            prefSupport.setOnPreferenceClickListener(preference -> {
                ConversationActivity.show(getActivity());
                return true;
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class ProductsAndServices extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_products_and_services);
            setHasOptionsMenu(true);

            final Preference turnOnPosPref = findPreference(getString(R.string.pref_turn_on_pos_key));
            final SharedPreferences tSharedPref = turnOnPosPref.getSharedPreferences();
            boolean isTurnedOn = tSharedPref.getBoolean(turnOnPosPref.getKey(), false);
            if (!isTurnedOn) {
                turnOnPosPref.setSummary(getString(R.string.pos_turned_off_explanation));
            } else {
                turnOnPosPref.setSummary(getString(R.string.pos_turned_on_explanation));
            }

            turnOnPosPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isTurnedOn1 = (Boolean) newValue;
                SessionManager sessionManager = new SessionManager(getActivity());
                final DatabaseManager databaseManager = DatabaseManager.getInstance(getActivity());
                final MerchantEntity merchantEntity = databaseManager.getMerchant(sessionManager.getMerchantId());

                if (!isTurnedOn1) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Are you sure?")
                            .setMessage("Loystar won't capture product information when recording sales.")
                            .setPositiveButton(getString(R.string.turn_off), (dialog, which) -> {
                                TwoStatePreference statePreference = (TwoStatePreference) turnOnPosPref;
                                statePreference.setChecked(false);

                                if (merchantEntity != null) {
                                    merchantEntity.setPosTurnedOn(false);
                                    merchantEntity.setUpdateRequired(true);
                                    databaseManager.updateMerchant(merchantEntity);
                                    SyncAdapter.performSync(getActivity(), merchantEntity.getEmail());
                                }
                            })
                            .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    if (merchantEntity != null) {
                        merchantEntity.setPosTurnedOn(true);
                        merchantEntity.setUpdateRequired(true);
                        databaseManager.updateMerchant(merchantEntity);
                        SyncAdapter.performSync(getActivity(), merchantEntity.getEmail());
                    }
                    Toast.makeText(getActivity(), getActivity().getString(R.string.pos_turn_on_notice), Toast.LENGTH_LONG).show();
                    turnOnPosPref.setSummary(getString(R.string.pos_turned_on_explanation));
                }
                return isTurnedOn1;
            });

            Preference prefMyProducts = findPreference(getString(R.string.pref_my_products_key));
            prefMyProducts.setOnPreferenceClickListener(preference -> {
                Intent ProductsViewIntent = new Intent(getActivity(), ProductListActivity.class);
                startActivity(ProductsViewIntent);
                return true;
            });

            Preference prefProductCategories = findPreference(getString(R.string.pref_product_categories_key));
            prefProductCategories.setOnPreferenceClickListener(preference -> {
                Intent categoriesIntent = new Intent(getActivity(), ProductCategoryListActivity.class);
                startActivity(categoriesIntent);
                return true;
            });

            Preference offersAndMessaging = findPreference(getString(R.string.pref_birthday_messages_and_offers_key));
            offersAndMessaging.setOnPreferenceClickListener(preference -> {
                Intent offersAndMessagingIntent = new Intent(getActivity(), BirthdayOffersAndMessagingActivity.class);
                startActivity(offersAndMessagingIntent);
                return true;
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    private static void checkSmsBalance(final Context mContext) {
        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage("Please wait! Checking sms balance...");
        progressDialog.show();

        ApiClient mApiClient = new ApiClient(mContext);

        mApiClient.getLoystarApi(false).getSmsBalance().enqueue(new Callback<SmsBalance>() {
            @Override
            public void onResponse(@NonNull Call<SmsBalance> call, @NonNull Response<SmsBalance> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (response.isSuccessful()) {
                    SmsBalance smsBalance = response.body();
                    if (smsBalance == null) {
                        Toast.makeText(mContext, mContext.getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                    } else {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                        String tmt = "Your Total SMS Balance is: %s ";
                        String message_text = String.format(tmt, smsBalance.getBalance());
                        alertDialogBuilder
                                .setMessage(message_text)
                                .setCancelable(false)
                                .setPositiveButton("OK", (dialog, id) -> {
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                } else {
                    Toast.makeText(mContext, mContext.getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SmsBalance> call, @NonNull Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(mContext, mContext.getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
