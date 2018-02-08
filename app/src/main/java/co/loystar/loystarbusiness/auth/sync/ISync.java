package co.loystar.loystarbusiness.auth.sync;

/**
 * Created by ordgen on 11/1/17.
 */

public interface ISync {
    void syncMerchant();
    void syncMerchantSubscription();
    void syncMerchantBirthdayOffer();
    void syncMerchantBirthdayOfferPresetSms();
    void syncCustomers();
    void syncProductCategories();
    void syncProducts();
    void syncLoyaltyPrograms();
    void syncSalesOrders();
    void syncSales();
}
