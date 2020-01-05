package co.loystar.loystarbusiness.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceHistoryEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Models;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesOrderEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import io.requery.reactivex.ReactiveSupport;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;

/**
 * Created by ordgen on 11/1/17.
 */

public class DatabaseManager implements IDatabaseManager{
    private static final int DATABASE_VERSION = 14;
    private static DatabaseManager mInstance;
    private ReactiveEntityStore<Persistable> mDataStore;
    private StdDateFormat mDateFormat;

    private DatabaseManager(@NonNull ReactiveEntityStore<Persistable> data) {
        mDataStore = data;
        mDateFormat = new StdDateFormat();
    }

    public static @NonNull ReactiveEntityStore<Persistable> getDataStore(@NonNull Context context) {
        // override onUpgrade to handle migrating to a new version
        DatabaseSource source = new DatabaseSource(context, Models.DEFAULT, DATABASE_VERSION);

        Configuration configuration = source.getConfiguration();
        return ReactiveSupport.toReactiveStore(new EntityDataStore<Persistable>(configuration));
    }

    public static @NonNull DatabaseManager getInstance(@NonNull Context context) {
        if (mInstance == null) {
            mInstance = new DatabaseManager(getDataStore(context));
        }

        return mInstance;
    }

    @Nullable
    @Override
    public MerchantEntity getMerchant(int merchantId) {
        return mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
    }

    @Override
    public void insertNewMerchant(@NonNull MerchantEntity merchantEntity) {
        mDataStore.upsert(merchantEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void updateMerchant(@NonNull MerchantEntity merchantEntity) {
        mDataStore.update(merchantEntity)
                .subscribe(/*no-op*/);
    }

    @Nullable
    @Override
    public BirthdayOfferEntity getMerchantBirthdayOffer(int merchantId) {
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        return merchantEntity != null ? merchantEntity.getBirthdayOffer() : null;
    }

    @Nullable
    @Override
    public BirthdayOfferPresetSmsEntity getMerchantBirthdayOfferPresetSms(int merchantId) {
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        return merchantEntity != null ? merchantEntity.getBirthdayOfferPresetSms() : null;
    }

    @Nullable
    @Override
    public SalesTransactionEntity getCustomerLastTransaction(@NonNull MerchantEntity merchantEntity, @NonNull CustomerEntity customerEntity) {
        Selection<ReactiveResult<SalesTransactionEntity>> transactionsSelection = mDataStore.select(SalesTransactionEntity.class);
        transactionsSelection.where(SalesTransactionEntity.MERCHANT.eq(merchantEntity));
        transactionsSelection.where(SalesTransactionEntity.CUSTOMER.equal(customerEntity));

        return transactionsSelection.orderBy(SalesTransactionEntity.CREATED_AT.desc()).get().firstOrNull();
    }

    @Nullable
    @Override
    public SaleEntity getLastSaleRecord() {
        Integer lastSaleId = getLastSaleRecordId();
        if (lastSaleId != null) {
            return mDataStore.findByKey(SaleEntity.class, lastSaleId).blockingGet();
        }
        return null;
    }

    @Nullable
    @Override
    public String getMerchantLoyaltyProgramsLastRecordDate(@NonNull MerchantEntity merchantEntity) {
        Result<LoyaltyProgramEntity> loyaltyProgramEntities = mDataStore.select(LoyaltyProgramEntity.class)
                .where(LoyaltyProgramEntity.OWNER.eq(merchantEntity)).orderBy(LoyaltyProgramEntity.UPDATED_AT.desc()).get();

        LoyaltyProgramEntity loyaltyProgramEntity = loyaltyProgramEntities.firstOrNull();
        if (loyaltyProgramEntity != null) {
            return mDateFormat.format(loyaltyProgramEntity.getUpdatedAt());
        }
        return null;
    }

    @Nullable
    @Override
    public Integer getLastTransactionRecordId() {
        String transactionQuery = "SELECT ROWID from SalesTransaction order by ROWID DESC limit 1";
        Tuple lastTransactionTuple = mDataStore.raw(transactionQuery).firstOrNull();
        Integer lastTransactionId = null;
        if (lastTransactionTuple != null) {
            try {
                lastTransactionId = lastTransactionTuple.get(0);
            } catch (ClassCastException e) {
                FirebaseCrash.report(e);
                try {
                    Long id = lastTransactionTuple.get(0);
                    lastTransactionId = id.intValue();
                } catch (ClassCastException e1) {
                    e1.printStackTrace();
                    FirebaseCrash.report(e1);
                }
            }
        }
        return lastTransactionId;
    }

    @Nullable
    @Override
    public Integer getLastInvoiceTransactionRecordId() {
        String transactionQuery = "SELECT ROWID from InvoiceTransaction order by ROWID DESC limit 1";
        Tuple lastTransactionTuple = mDataStore.raw(transactionQuery).firstOrNull();
        Integer lastTransactionId = null;
        if (lastTransactionTuple != null) {
            try {
                lastTransactionId = lastTransactionTuple.get(0);
            } catch (ClassCastException e) {
                FirebaseCrash.report(e);
                try {
                    Long id = lastTransactionTuple.get(0);
                    lastTransactionId = id.intValue();
                } catch (ClassCastException e1) {
                    e1.printStackTrace();
                    FirebaseCrash.report(e1);
                }
            }
        }
        return lastTransactionId;
    }

    @Nullable
    @Override
    public Integer getLastSaleRecordId() {
        String saleQuery = "SELECT ROWID from Sale order by ROWID DESC limit 1";
        Tuple lastSaleTuple = mDataStore.raw(saleQuery).firstOrNull();
        Integer lastSaleId = null;
        if (lastSaleTuple != null) {
            try {
                lastSaleId = lastSaleTuple.get(0);
            } catch (ClassCastException e) {
                try {
                    Long id = lastSaleTuple.get(0);
                    lastSaleId = id.intValue();
                } catch (ClassCastException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return lastSaleId;
    }

    @Nullable
    @Override
    public InvoiceEntity getInvoiceById(int id) {
        return mDataStore.select(InvoiceEntity.class)
                .where(CustomerEntity.ID.eq(id))
                .get()
                .firstOrNull();
    }

    @Nullable
    @Override
    public Integer getLastInvoiceRecordId() {
        String invoiceQuery = "SELECT ROWID from Invoice order by ROWID DESC limit 1";
        Tuple lastInvoiceTuple = mDataStore.raw(invoiceQuery).firstOrNull();
        Integer lastInvoiceId = null;
        if(lastInvoiceTuple != null) {
            try {
                lastInvoiceId = lastInvoiceTuple.get(0);
            } catch (ClassCastException e) {
                try {
                    Long id = lastInvoiceTuple.get(0);
                    lastInvoiceId = id.intValue();
                } catch (ClassCastException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return lastInvoiceId;
    }

    @Nullable
    @Override
    public String getMerchantProductsLastRecordDate(@NonNull MerchantEntity merchantEntity) {
        Result<ProductEntity> productEntities = mDataStore.select(ProductEntity.class)
                .where(ProductEntity.OWNER.eq(merchantEntity)).orderBy(ProductEntity.UPDATED_AT.desc()).get();

        ProductEntity productEntity = productEntities.firstOrNull();
        if (productEntity != null) {
            return mDateFormat.format(productEntity.getUpdatedAt());
        }
        return null;
    }

    @Nullable
    @Override
    public String getProductCategoriesLastRecordDate(@NonNull MerchantEntity merchantEntity) {
        Result<ProductCategoryEntity> productCategoryEntities = mDataStore.select(ProductCategoryEntity.class)
                .where(ProductCategoryEntity.OWNER.eq(merchantEntity)).orderBy(ProductCategoryEntity.UPDATED_AT.desc()).get();

        ProductCategoryEntity productCategoryEntity = productCategoryEntities.firstOrNull();
        if (productCategoryEntity != null) {
            return mDateFormat.format(productCategoryEntity.getUpdatedAt());
        }
        return null;
    }

    @Nullable
    @Override
    public CustomerEntity getCustomerById(int customerId) {
        return mDataStore.select(CustomerEntity.class)
                .where(CustomerEntity.ID.eq(customerId))
                .get()
                .firstOrNull();
    }

    @Nullable
    @Override
    public LoyaltyProgramEntity getLoyaltyProgramById(int programId) {
        return mDataStore.select(LoyaltyProgramEntity.class)
                .where(LoyaltyProgramEntity.ID.eq(programId))
                .get()
                .firstOrNull();
    }

    @Nullable
    @Override
    public ProductEntity getProductById(int productId) {
        return mDataStore.select(ProductEntity.class)
                .where(ProductEntity.ID.eq(productId))
                .get()
                .firstOrNull();
    }

    @Nullable
    @Override
    public ProductCategoryEntity getProductCategoryById(int productCategoryId) {
        return mDataStore.select(ProductCategoryEntity.class)
                .where(ProductCategoryEntity.ID.eq(productCategoryId))
                .get()
                .firstOrNull();
    }

    @Override
    public void deleteCustomer(@NonNull CustomerEntity customerEntity) {
        customerEntity.setOwner(null);
        mDataStore.delete(customerEntity)
                .subscribe();
    }

    @Override
    public void deleteLoyaltyProgram(@NonNull LoyaltyProgramEntity loyaltyProgramEntity) {
        loyaltyProgramEntity.setOwner(null);
        mDataStore.delete(loyaltyProgramEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void deleteProduct(@NonNull ProductEntity productEntity) {
        // clear parent associations - possible fix for `Expected 1 row affected actual 0` bug
        productEntity.setLoyaltyProgram(null);
        productEntity.setCategory(null);
        productEntity.setOwner(null);
        mDataStore.delete(productEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void deleteProductCategory(@NonNull ProductCategoryEntity productCategoryEntity) {
        productCategoryEntity.setOwner(null);
        mDataStore.delete(productCategoryEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void insertNewProduct(@NonNull ProductEntity productEntity) {
        mDataStore.upsert(productEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void insertNewProductCategory(@NonNull ProductCategoryEntity productCategoryEntity) {
        mDataStore.upsert(productCategoryEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void insertNewLoyaltyProgram(@NonNull LoyaltyProgramEntity loyaltyProgramEntity) {
        mDataStore.upsert(loyaltyProgramEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void insertNewSalesTransaction(@NonNull SalesTransactionEntity salesTransactionEntity) {
        mDataStore.upsert(salesTransactionEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void updateCustomer(@NonNull CustomerEntity customerEntity) {
        mDataStore.update(customerEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void updateBirthdayOffer(@NonNull BirthdayOfferEntity birthdayOfferEntity) {
        mDataStore.update(birthdayOfferEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void updateBirthdayPresetSms(@NonNull BirthdayOfferPresetSmsEntity birthdayOfferPresetSmsEntity) {
        mDataStore.update(birthdayOfferPresetSmsEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void updateProduct(@NonNull ProductEntity productEntity) {
        mDataStore.update(productEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void updateLoyaltyProgram(@NonNull LoyaltyProgramEntity loyaltyProgramEntity) {
        mDataStore.update(loyaltyProgramEntity)
                .subscribe(/*no-op*/);
    }

    @NonNull
    @Override
    public List<SaleEntity> getUnsyncedSaleEnties(@NonNull MerchantEntity merchantEntity) {
        Selection<ReactiveResult<SaleEntity>> query = mDataStore.select(SaleEntity.class);
        query.where(SaleEntity.SYNCED.eq(false));
        query.where(SaleEntity.MERCHANT.eq(merchantEntity));
        return query.get().toList();
    }

    @NonNull
    @Override
    public List<InvoiceEntity> getUnsyncedInvoiceEntities(@NonNull MerchantEntity merchantEntity) {
        Selection<ReactiveResult<InvoiceEntity>> query = mDataStore.select(InvoiceEntity.class);
        query.where(InvoiceEntity.SYNCED.eq(false));
        query.where(InvoiceEntity.OWNER.eq(merchantEntity));
        return query.get().toList();
    }

    @Override
    public int getTotalCustomerStamps(int merchantId, int customerId) {
        int stamps = 0;
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        Selection<ReactiveResult<SalesTransactionEntity>> resultSelection = mDataStore.select(SalesTransactionEntity.class);
        resultSelection.where(SalesTransactionEntity.MERCHANT.eq(merchantEntity));
        resultSelection.where(SalesTransactionEntity.CUSTOMER_ID.eq(customerId));
        resultSelection.where(SalesTransactionEntity.STAMPS.notNull());
        for (SalesTransactionEntity transactionEntity: resultSelection.get().toList()) {
            stamps += transactionEntity.getStamps();
        }
        return stamps;
    }

    @Override
    public int getTotalCustomerPoints(int merchantId, int customerId) {
        int points = 0;
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();

        Selection<ReactiveResult<SalesTransactionEntity>> resultSelection = mDataStore.select(SalesTransactionEntity.class);
        resultSelection.where(SalesTransactionEntity.MERCHANT.eq(merchantEntity));
        resultSelection.where(SalesTransactionEntity.CUSTOMER_ID.eq(customerId));
        resultSelection.where(SalesTransactionEntity.POINTS.notNull());
        for (SalesTransactionEntity transactionEntity: resultSelection.get().toList()) {
            points += transactionEntity.getPoints();
        }

        return points;
    }

    @Override
    public int getTotalCustomerSpent(int merchantId, int customerId) {
        int spent = 0;
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        Selection<ReactiveResult<SalesTransactionEntity>> resultSelection = mDataStore.select(SalesTransactionEntity.class);
        resultSelection.where(SalesTransactionEntity.MERCHANT.eq(merchantEntity));
        resultSelection.where(SalesTransactionEntity.CUSTOMER_ID.eq(customerId));
        resultSelection.where(SalesTransactionEntity.AMOUNT.notNull());
        for (SalesTransactionEntity transactionEntity: resultSelection.get().toList()) {
            spent += transactionEntity.getAmount();
        }
        return spent;
    }

    @Override
    public int getTotalCustomerPointsForProgram(int programId, int customerId) {
        int points = 0;
        Selection<ReactiveResult<SalesTransactionEntity>> resultSelection = mDataStore.select(SalesTransactionEntity.class);
        resultSelection.where(SalesTransactionEntity.MERCHANT_LOYALTY_PROGRAM_ID.eq(programId));
        resultSelection.where(SalesTransactionEntity.CUSTOMER_ID.eq(customerId));
        resultSelection.where(SalesTransactionEntity.POINTS.notNull());
        for (SalesTransactionEntity transactionEntity: resultSelection.get().toList()) {
            points += transactionEntity.getPoints();
        }

        return points;
    }

    @Override
    public int getTotalCustomerStampsForProgram(int programId, int customerId) {
        int stamps = 0;
        Selection<ReactiveResult<SalesTransactionEntity>> resultSelection = mDataStore.select(SalesTransactionEntity.class);
        resultSelection.where(SalesTransactionEntity.MERCHANT_LOYALTY_PROGRAM_ID.eq(programId));
        resultSelection.where(SalesTransactionEntity.CUSTOMER_ID.eq(customerId));
        resultSelection.where(SalesTransactionEntity.STAMPS.notNull());
        for (SalesTransactionEntity transactionEntity: resultSelection.get().toList()) {
            stamps += transactionEntity.getStamps();
        }
        return stamps;
    }

    @NonNull
    @Override
    public List<CustomerEntity> getCustomersMarkedForDeletion(@NonNull MerchantEntity  merchantEntity) {
        Selection<ReactiveResult<CustomerEntity>> customersSelection = mDataStore.select(CustomerEntity.class);
        customersSelection.where(CustomerEntity.DELETED.equal(true));
        customersSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
        return customersSelection.get().toList();
    }

    @NonNull
    @Override
    public List<ProductEntity> getProductsMarkedForDeletion(@NonNull MerchantEntity  merchantEntity) {
        Selection<ReactiveResult<ProductEntity>> productsSelection = mDataStore.select(ProductEntity.class);
        productsSelection.where(ProductEntity.DELETED.equal(true));
        productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
        return productsSelection.get().toList();
    }

    @NonNull
    @Override
    public List<LoyaltyProgramEntity> getLoyaltyProgramsMarkedForDeletion(@NonNull MerchantEntity  merchantEntity) {
        Selection<ReactiveResult<LoyaltyProgramEntity>> programsSelection = mDataStore.select(LoyaltyProgramEntity.class);
        programsSelection.where(LoyaltyProgramEntity.DELETED.equal(true));
        programsSelection.where(LoyaltyProgramEntity.OWNER.eq(merchantEntity));
        return programsSelection.get().toList();
    }

    @Nullable
    @Override
    public MerchantEntity getMerchantByPhone(String phoneNumber) {
        return mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.CONTACT_NUMBER.eq(phoneNumber))
                .get()
                .firstOrNull();
    }

    @Nullable
    @Override
    public CustomerEntity getCustomerByPhone(String phoneNumber) {
        return mDataStore.select(CustomerEntity.class)
                .where(CustomerEntity.PHONE_NUMBER.eq(phoneNumber))
                .get()
                .firstOrNull();
    }

    @Override
    public List<CustomerEntity> searchCustomersByNameOrNumber(@NonNull String q, int merchantId) {
        List<CustomerEntity> customerEntityList;
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        String query = q.substring(0, 1).equals("0") ? q.substring(1) : q;
        String searchQuery = "%" + query.toLowerCase() + "%";
        if (TextUtilsHelper.isInteger(q)) {
            Selection<ReactiveResult<CustomerEntity>> phoneSelection = mDataStore.select(CustomerEntity.class);
            phoneSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
            phoneSelection.where(CustomerEntity.DELETED.notEqual(true));
            phoneSelection.where(CustomerEntity.PHONE_NUMBER.like(searchQuery));
            customerEntityList = phoneSelection.get().toList();
        } else {
            Selection<ReactiveResult<CustomerEntity>> nameSelection = mDataStore.select(CustomerEntity.class);
            nameSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
            nameSelection.where(CustomerEntity.DELETED.notEqual(true));
            nameSelection.where(CustomerEntity.FIRST_NAME.like(searchQuery));
            customerEntityList = nameSelection.get().toList();
        }
        return customerEntityList;
    }

    @Override
    public List<CustomerEntity> getMerchantCustomers(int merchantId) {
        List<CustomerEntity> customerEntityList = new ArrayList<>();
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        if (merchantEntity != null) {
            Selection<ReactiveResult<CustomerEntity>> customersSelection = mDataStore.select(CustomerEntity.class);
            customersSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
            customersSelection.where(CustomerEntity.DELETED.notEqual(true));
            customerEntityList = customersSelection.get().toList();
        }
        return customerEntityList;
    }

    @Override
    public List<LoyaltyProgramEntity> getMerchantLoyaltyPrograms(int merchantId) {
        List<LoyaltyProgramEntity> loyaltyProgramEntityList = new ArrayList<>();
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        if (merchantEntity != null) {
            Selection<ReactiveResult<LoyaltyProgramEntity>> programsSelection = mDataStore.select(LoyaltyProgramEntity.class);
            programsSelection.where(LoyaltyProgramEntity.OWNER.eq(merchantEntity));
            programsSelection.where(LoyaltyProgramEntity.DELETED.notEqual(true));
            loyaltyProgramEntityList = programsSelection.get().toList();
        }
        return loyaltyProgramEntityList;
    }

    @Override
    public List<ProductCategoryEntity> getMerchantProductCategories(int merchantId) {
        List<ProductCategoryEntity> productCategoryEntities = new ArrayList<>();
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        if (merchantEntity != null) {
            Selection<ReactiveResult<ProductCategoryEntity>> selection = mDataStore.select(ProductCategoryEntity.class);
            selection.where(ProductCategoryEntity.OWNER.eq(merchantEntity));
            selection.where(ProductCategoryEntity.DELETED.notEqual(true));
            productCategoryEntities = selection.get().toList();
        }
        return productCategoryEntities;
    }

    @Nullable
    @Override
    public CustomerEntity getCustomerByUserId(int userId) {
        return mDataStore.select(CustomerEntity.class)
            .where(CustomerEntity.USER_ID.eq(userId))
            .get()
            .firstOrNull();
    }

    @Nullable
    @Override
    public SalesOrderEntity getSalesOrderById(int salesOrderId) {
        return mDataStore.findByKey(SalesOrderEntity.class, salesOrderId).blockingGet();
    }

    @Override
    public List<SalesOrderEntity> getUpdateRequiredSalesOrders(@NonNull MerchantEntity merchantEntity) {
        Selection<ReactiveResult<SalesOrderEntity>> selection = mDataStore.select(SalesOrderEntity.class);
        selection.where(SalesOrderEntity.MERCHANT.eq(merchantEntity));
        selection.where(SalesOrderEntity.UPDATE_REQUIRED.eq(true));
        return selection.get().toList();
    }


}
