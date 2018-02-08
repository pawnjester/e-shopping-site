package co.loystar.loystarbusiness.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.OrderItemEntity;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesOrderEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.models.entities.SubscriptionEntity;
import co.loystar.loystarbusiness.models.entities.TransactionSms;
import co.loystar.loystarbusiness.models.entities.TransactionSmsEntity;

/**
 * Created by ordgen on 11/1/17.
 */

public interface IDatabaseManager {
    @Nullable
    MerchantEntity getMerchant(int merchantId);

    void insertNewMerchant(@NonNull MerchantEntity merchantEntity);

    void updateMerchant(@NonNull MerchantEntity merchantEntity);

    @Nullable
    BirthdayOfferEntity getMerchantBirthdayOffer(int merchantId);

    @Nullable
    BirthdayOfferPresetSmsEntity getMerchantBirthdayOfferPresetSms(int merchantId);

    @Nullable
    SubscriptionEntity getMerchantSubscription(int merchantId);

    @Nullable
    String getMerchantCustomersLastRecordDate(@NonNull MerchantEntity merchantEntity);

    @Nullable
    SalesTransactionEntity getCustomerLastTransaction(
            @NonNull MerchantEntity merchantEntity,
            @NonNull CustomerEntity customerEntity
    );

    @Nullable
    String getMerchantSalesLastRecordDate(@NonNull MerchantEntity merchantEntity);

    @Nullable
    String getMerchantLoyaltyProgramsLastRecordDate(@NonNull MerchantEntity merchantEntity);

    @Nullable
    Integer getLastTransactionRecordId();

    @Nullable
    Integer getLastSaleRecordId();

    @Nullable
    String getMerchantProductsLastRecordDate(@NonNull MerchantEntity merchantEntity);

    @Nullable
    String getProductCategoriesLastRecordDate(@NonNull MerchantEntity merchantEntity);

    @Nullable
    CustomerEntity getCustomerById(int customerId);

    @Nullable
    LoyaltyProgramEntity getLoyaltyProgramById(int programId);

    @Nullable
    ProductEntity getProductById(int productId);

    @Nullable
    ProductCategoryEntity getProductCategoryById(int productCategoryId);

    void deleteCustomer(@NonNull CustomerEntity customerEntity);

    void deleteLoyaltyProgram(@NonNull LoyaltyProgramEntity loyaltyProgramEntity);

    void deleteProduct(@NonNull ProductEntity productEntity);

    void deleteSalesTransaction(@NonNull SalesTransactionEntity salesTransactionEntity);

    void deleteProductCategory(@NonNull ProductCategoryEntity productCategoryEntity);

    void insertNewCustomer(@NonNull CustomerEntity customerEntity);

    void insertNewProduct(@NonNull ProductEntity productEntity);

    void insertNewProductCategory(@NonNull ProductCategoryEntity productCategoryEntity);

    void insertNewLoyaltyProgram(@NonNull LoyaltyProgramEntity loyaltyProgramEntity);

    void insertNewSalesTransaction(@NonNull SalesTransactionEntity salesTransactionEntity);

    void updateCustomer(@NonNull CustomerEntity customerEntity);

    void updateBirthdayOffer(@NonNull BirthdayOfferEntity birthdayOfferEntity);

    void updateBirthdayPresetSms(@NonNull BirthdayOfferPresetSmsEntity birthdayOfferPresetSmsEntity);

    void updateProduct (@NonNull ProductEntity productEntity);

    void updateLoyaltyProgram(@NonNull LoyaltyProgramEntity loyaltyProgramEntity);

    @NonNull
    List<SalesTransactionEntity> getUnsyncedSalesTransactions(@NonNull MerchantEntity  merchantEntity);

    @NonNull
    List<SaleEntity> getUnsyncedSaleEnties(@NonNull MerchantEntity  merchantEntity);

    int getTotalCustomerStamps(int merchantId, int customerId);

    int getTotalCustomerPoints(int merchantId, int customerId);

    int getTotalCustomerSpent(int merchantId, int customerId);

    int getTotalCustomerPointsForProgram(int programId, int customerId);

    int getTotalCustomerStampsForProgram(int programId, int customerId);

    @NonNull
    List<CustomerEntity> getCustomersMarkedForDeletion(@NonNull MerchantEntity  merchantEntity);

    @NonNull
    List<ProductEntity> getProductsMarkedForDeletion(@NonNull MerchantEntity  merchantEntity);

    @NonNull
    List<LoyaltyProgramEntity> getLoyaltyProgramsMarkedForDeletion(@NonNull MerchantEntity  merchantEntity);

    @Nullable
    MerchantEntity getMerchantByPhone(String phoneNumber);

    @Nullable
    CustomerEntity getCustomerByPhone(String phoneNumber);

    List<CustomerEntity> searchCustomersByNameOrNumber(@NonNull String q, int merchantId);

    List<CustomerEntity> getMerchantCustomers(int merchantId);

    List<LoyaltyProgramEntity> getMerchantLoyaltyPrograms(int merchantId);

    List<ProductCategoryEntity> getMerchantProductCategories(int merchantId);

    List<TransactionSmsEntity> getMerchantTransactionSms(int merchantId);

    void deleteTransactionSms(@NonNull TransactionSmsEntity transactionSmsEntity);

    void insertSalesOrder(@NonNull SalesOrderEntity salesOrderEntity);

    void insertOrderItem(@NonNull OrderItemEntity orderItemEntity);

    @Nullable
    String getSalesOrdersLastRecordDate(@NonNull MerchantEntity merchantEntity);

    @Nullable
    CustomerEntity getCustomerByUserId(int userId);

    @Nullable
    SalesOrderEntity getSalesOrderById(int salesOrderId);

    List<SalesOrderEntity> getUpdateRequiredSalesOrders(@NonNull MerchantEntity merchantEntity);
}
