package co.loystar.loystarbusiness.auth.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

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
    private static final String TAG = AccountGeneral.class.getSimpleName();

    /**
     * Gets the current sync account for the app.
     * @return {@link Account}
     */
    public static Account getAccount(Context context, String accountName) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = null;
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        if (accounts.length == 0) {
            account = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
        } else {
            for (Account ac: accounts) {
                if (ac.name.equals(accountName)) {
                    account = ac;
                }
            }
            if (account == null) {
                account = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
            }
        }
        return account;
    }

    /**
     * Creates a sync account for a user.
     * @param c {@link Context}
     */
    public static void createSyncAccount(Context c, Account account) {
        // Flag to determine if this is a new account or not
        boolean created = false;

        AccountManager manager = AccountManager.get(c);

        // Attempt to explicitly create the account with no password or extra data
        if (manager.addAccountExplicitly(account, null, null)) {
            final String AUTHORITY = AccountGeneral.AUTHORITY;
            final long SYNC_FREQUENCY = 60 * 60; // 1 hour (seconds)

            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, AUTHORITY, 1);

            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);

            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), SYNC_FREQUENCY);

            created = true;
        }

        // Force a sync if the account was just created
        if (created) {
            SyncAdapter.performSync(c, account.name);
        }
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
