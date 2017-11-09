package co.loystar.loystarbusiness.auth.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.sql.Timestamp;

import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.BirthdayOffer;
import co.loystar.loystarbusiness.models.databinders.BirthdayOfferPresetSms;
import co.loystar.loystarbusiness.models.databinders.Subscription;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.Merchant;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SubscriptionEntity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 11/1/17.
 */

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final AccountManager mAccountManager;
    private static final String TAG = SyncAdapter.class.getSimpleName();
    private String mAuthToken;
    private ApiClient mApiClient;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;
    private MerchantEntity merchantEntity;
    private Context mContext;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
        mApiClient = new ApiClient(context);
        mDatabaseManager = DatabaseManager.getInstance(context);
        mSessionManager = new SessionManager(context);
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle bundle,
            String s,
            ContentProviderClient contentProviderClient,
            SyncResult syncResult
    ) {
        try {
            mAuthToken = mAccountManager.blockingGetAuthToken(account, AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS, true);
            merchantEntity = mDatabaseManager.getMerchant(mSessionManager.getMerchantId());
            if (merchantEntity == null) {
                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
            } else {
                new SyncNow().startAllSyncs();
            }
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
            e.printStackTrace();
        }
    }

    private class SyncNow implements ISync {
        void startAllSyncs() {
            syncMerchantSubscription();
            syncMerchantBirthdayOffer();
            syncMerchantBirthdayOfferPresetSms();
        }

        @Override
        public void syncMerchantSubscription() {
            mApiClient.getLoystarApi(false).getMerchantSubscription().enqueue(new Callback<Subscription>() {
                @Override
                public void onResponse(Call<Subscription> call, Response<Subscription> response) {
                    if (response.isSuccessful()) {
                        Subscription subscription = response.body();
                        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
                        subscriptionEntity.setId(subscription.getId());
                        subscriptionEntity.setExpiresOn(new Timestamp(subscription.getExpires_on().getMillis()));
                        subscriptionEntity.setCreatedAt(new Timestamp(subscription.getCreated_at().getMillis()));
                        subscriptionEntity.setUpdatedAt(new Timestamp(subscription.getUpdated_at().getMillis()));
                        subscriptionEntity.setPricingPlanId(subscription.getPricing_plan_id());
                        subscriptionEntity.setPlanName(subscription.getPlan_name());

                        MerchantEntity merchant = mDatabaseManager.getMerchant(subscription.getMerchant_id());
                        if (merchant != null) {
                            merchant.setSubscription(subscriptionEntity);
                            mDatabaseManager.addMerchant(merchant);
                        }
                    } else {
                        if (response.code() == 401) {
                            mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                        }
                    }
                }

                @Override
                public void onFailure(Call<Subscription> call, Throwable t) {}
            });
        }

        @Override
        public void syncMerchantBirthdayOffer() {
            mApiClient.getLoystarApi(false).getMerchantBirthdayOffer().enqueue(new Callback<BirthdayOffer>() {
                @Override
                public void onResponse(Call<BirthdayOffer> call, Response<BirthdayOffer> response) {
                    if (response.isSuccessful()) {
                        BirthdayOffer birthdayOffer = response.body();
                        BirthdayOfferEntity birthdayOfferEntity = new BirthdayOfferEntity();
                        birthdayOfferEntity.setId(birthdayOffer.getId());
                        birthdayOfferEntity.setOfferDescription(birthdayOffer.getOffer_description());
                        birthdayOfferEntity.setCreatedAt(new Timestamp(birthdayOffer.getCreated_at().getMillis()));
                        birthdayOfferEntity.setUpdatedAt(new Timestamp(birthdayOffer.getUpdated_at().getMillis()));

                        MerchantEntity merchantEntity = mDatabaseManager.getMerchant(birthdayOffer.getMerchant_id());
                        if (merchantEntity != null) {
                            merchantEntity.setBirthdayOffer(birthdayOfferEntity);
                            mDatabaseManager.updateMerchant(merchantEntity);
                        }
                    } else {
                        if (response.code() == 401) {
                            mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                        }
                    }
                }

                @Override
                public void onFailure(Call<BirthdayOffer> call, Throwable t) {}
            });
        }

        @Override
        public void syncMerchantBirthdayOfferPresetSms() {
            mApiClient.getLoystarApi(false).getMerchantBirthdayPresetSms().enqueue(new Callback<BirthdayOfferPresetSms>() {
                @Override
                public void onResponse(Call<BirthdayOfferPresetSms> call, Response<BirthdayOfferPresetSms> response) {
                    if (response.isSuccessful()) {
                        BirthdayOfferPresetSms birthdayOfferPresetSms = response.body();
                        BirthdayOfferPresetSmsEntity birthdayOfferPresetSmsEntity = new BirthdayOfferPresetSmsEntity();
                        birthdayOfferPresetSmsEntity.setId(birthdayOfferPresetSms.getId());
                        birthdayOfferPresetSmsEntity.setPresetSmsText(birthdayOfferPresetSms.getPreset_sms_text());
                        birthdayOfferPresetSmsEntity.setCreatedAt(new Timestamp(birthdayOfferPresetSms.getCreated_at().getMillis()));
                        birthdayOfferPresetSmsEntity.setUpdatedAt(new Timestamp(birthdayOfferPresetSms.getUpdated_at().getMillis()));

                        MerchantEntity merchantEntity = mDatabaseManager.getMerchant(birthdayOfferPresetSms.getMerchant_id());
                        if (merchantEntity != null) {
                            merchantEntity.setBirthdayOfferPresetSms(birthdayOfferPresetSmsEntity);
                            mDatabaseManager.updateMerchant(merchantEntity);
                        }
                    } else {
                        if (response.code() == 401) {
                            mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                        }
                    }
                }

                @Override
                public void onFailure(Call<BirthdayOfferPresetSms> call, Throwable t) {}
            });
        }
    }

    /**
     * Manually force Android to perform a sync with our SyncAdapter.
     */
    public static void performSync(Context context, String accountName) {
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(AccountGeneral.getAccount(context, accountName), AccountGeneral.AUTHORITY, b);
    }
}
