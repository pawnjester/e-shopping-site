package co.loystar.loystarbusiness.auth.sync;

/**
 * Created by ordgen on 11/1/17.
 */

public interface ISync {
    void syncMerchantSubscription();
    void syncMerchantBirthdayOffer();
    void syncMerchantBirthdayOfferPresetSms();
    void syncCustomers();
    void syncTransactions();
    void syncProductCategories();
    void syncProducts();
    void syncLoyaltyPrograms();
}
