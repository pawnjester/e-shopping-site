package co.loystar.loystarbusiness.auth.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.joda.time.DateTime;

import java.util.Date;

import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SubscriptionEntity;

/**
 * Created by ordgen on 11/1/17.
 */

public class AccountGeneral {
    static final String AUTHORITY = "co.loystar.loystarbusiness.provider";
    public static final String AUTH_TOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String ACCOUNT_TYPE = "co.loystar.loystarbusiness";
    static final String AUTH_TOKEN_TYPE_READ_ONLY = "Read only";
    static final String AUTH_TOKEN_TYPE_READ_ONLY_LABEL = "Read only access to a Loystar account";
    static final String AUTH_TOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to Loystar account";

    /**
     * Gets the current sync account for the app.
     * @return {@link Account}
     */
    @Nullable
    public static Account getUserAccount(Context context, String accountName) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        if (accounts.length == 0) {
            return null;
        }
        if (TextUtils.isEmpty(accountName)) {
            return accounts[0];
        }
        Account account = null;
        for (Account ac: accounts) {
            if (ac.name.equals(accountName)) {
                account = ac;
            }
        }
        return account;
    }

    /**
     * Gets the current sync account for the app.
     * @return {@link Account}
     */
    @Nullable
    public static Account addOrFindAccount(Context context, String accountName, String password) {
        if (TextUtils.isEmpty(accountName)) {
            return null;
        }
        AccountManager accountManager = AccountManager.get(context);
        Account account = null;
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        if (accounts.length == 0) {
            account = new Account(accountName, ACCOUNT_TYPE);
            accountManager.addAccountExplicitly(account, password, null);
        } else {
            for (Account ac: accounts) {
                if (ac.name.equals(accountName)) {
                    account = ac;
                }
            }
            if (account == null) {
                account = new Account(accountName, ACCOUNT_TYPE);
                accountManager.addAccountExplicitly(account, password, null);
            } else {
                accountManager.setPassword(account, password);
            }
        }
        return account;
    }

    /**
     * Sets a sync account for a user.
     * @param c {@link Context}
     * @param account {@link Account}
     */
    public static void SetSyncAccount(Context c, Account account) {
        final String AUTHORITY = AccountGeneral.AUTHORITY;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
        int syncFrequency = Integer.parseInt(sharedPreferences.getString("sync_frequency", "360"));
        final long SYNC_FREQUENCY = syncFrequency * 60;

        // Inform the system that this account supports sync
        ContentResolver.setIsSyncable(account, AUTHORITY, 1);

        // Inform the system that this account is eligible for auto sync when the network is up
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true);

        // Recommend a schedule for automatic synchronization. The system may modify this based
        // on other scheduled syncs and network utilization.
        ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), SYNC_FREQUENCY);

        // Force initial Sync
        SyncAdapter.performSync(c, account.name);
    }

    public static boolean isAccountActive(Context context) {
        boolean isActive = false;
        DatabaseManager databaseManager = DatabaseManager.getInstance(context);
        SessionManager sessionManager = new SessionManager(context);
        MerchantEntity merchantEntity = databaseManager.getMerchant(sessionManager.getMerchantId());
        if (merchantEntity != null) {
            SubscriptionEntity subscriptionEntity = merchantEntity.getSubscription();
            if (subscriptionEntity != null) {
                DateTime expiresOn = new DateTime(subscriptionEntity.getExpiresOn().getTime());
                isActive = expiresOn.isAfterNow();
            }
        }
        return isActive;
    }

    public static Date accountExpiry(Context context) {
        Date date = null;
        DatabaseManager databaseManager = DatabaseManager.getInstance(context);
        SessionManager sessionManager = new SessionManager(context);
        MerchantEntity merchantEntity = databaseManager.getMerchant(sessionManager.getMerchantId());
        if (merchantEntity != null) {
            SubscriptionEntity subscriptionEntity = merchantEntity.getSubscription();
            if (subscriptionEntity != null) {
                DateTime expiresOn = new DateTime(subscriptionEntity.getExpiresOn().getTime());
                date = expiresOn.toDate();
            }
        }
        return date;
    }

}
