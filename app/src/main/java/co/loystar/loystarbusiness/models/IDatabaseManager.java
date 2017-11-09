package co.loystar.loystarbusiness.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SubscriptionEntity;

/**
 * Created by ordgen on 11/1/17.
 */

public interface IDatabaseManager {
    @Nullable
    MerchantEntity getMerchant(int merchantId);

    void addMerchant(@NonNull MerchantEntity merchantEntity);

    void updateMerchant(@NonNull MerchantEntity merchantEntity);

    @Nullable
    BirthdayOfferEntity getMerchantBirthdayOffer(int merchantId);

    @Nullable
    BirthdayOfferPresetSmsEntity getMerchantBirthdayOfferPresetSms(int merchantId);

    @Nullable
    SubscriptionEntity getMerchantSubscription(int merchantId);
}
