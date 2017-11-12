package co.loystar.loystarbusiness.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.util.Collections;
import java.util.List;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Models;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.models.entities.SubscriptionEntity;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import io.requery.reactivex.ReactiveSupport;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;

/**
 * Created by ordgen on 11/1/17.
 */

public class DatabaseManager implements IDatabaseManager{
    private static final int DATABASE_VERSION = 1;
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

        if (BuildConfig.DEBUG) {
            // use this in development mode to drop and recreate the tables on every upgrade
            source.setTableCreationMode(TableCreationMode.DROP_CREATE);
        }

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
    public SubscriptionEntity getMerchantSubscription(int merchantId) {
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        return merchantEntity != null ? merchantEntity.getSubscription() : null;
    }

    @Nullable
    @Override
    public String getMerchantCustomersLastRecordDate(@NonNull MerchantEntity merchantEntity) {
        Result<CustomerEntity> customerEntities = mDataStore.select(CustomerEntity.class)
                .where(CustomerEntity.OWNER.eq(merchantEntity)).orderBy(CustomerEntity.UPDATED_AT.desc()).get();

        CustomerEntity customerEntity = customerEntities.firstOrNull();
        if (customerEntity != null) {
            return mDateFormat.format(customerEntity.getUpdatedAt());
        }
        return null;
    }

    @Nullable
    @Override
    public String getMerchantTransactionsLastRecordDate(@NonNull MerchantEntity merchantEntity) {
        Result<SalesTransactionEntity> transactions = mDataStore.select(SalesTransactionEntity.class)
                .where(SalesTransactionEntity.MERCHANT.eq(merchantEntity)).orderBy(SalesTransactionEntity.CREATED_AT.desc()).get();

        SalesTransactionEntity transactionEntity = transactions.firstOrNull();
        if (transactionEntity != null) {
            return mDateFormat.format(transactionEntity.getCreatedAt());
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
    public void deleteMerchantBirthdayOffer(@NonNull MerchantEntity merchantEntity) {
        if (merchantEntity.getBirthdayOffer() != null) {
            mDataStore.delete(merchantEntity.getBirthdayOffer())
                    .subscribe(/*no-op*/);
        }
    }

    @Override
    public void deleteCustomer(@NonNull CustomerEntity customerEntity) {
        mDataStore.delete(customerEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void deleteLoyaltyProgram(@NonNull LoyaltyProgramEntity loyaltyProgramEntity) {
        mDataStore.delete(loyaltyProgramEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void deleteProduct(@NonNull ProductEntity productEntity) {
        mDataStore.delete(productEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void deleteSalesTransaction(@NonNull SalesTransactionEntity salesTransactionEntity) {
        mDataStore.delete(salesTransactionEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void deleteProductCategory(@NonNull ProductCategoryEntity productCategoryEntity) {
        mDataStore.delete(productCategoryEntity)
                .subscribe(/*no-op*/);
    }

    @Override
    public void insertNewCustomer(@NonNull CustomerEntity customerEntity) {
        CustomerEntity existingCustomer = mDataStore.select(CustomerEntity.class)
                .where(CustomerEntity.PHONE_NUMBER.eq(customerEntity.getPhoneNumber()))
                .get()
                .firstOrNull();
        if (existingCustomer == null) {
            mDataStore.upsert(customerEntity)
                    .subscribe(/*no-op*/);
        }
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

    @Override
    public void updateProductCategory(@NonNull ProductCategoryEntity productCategoryEntity) {
        mDataStore.update(productCategoryEntity)
                .subscribe(/*no-op*/);
    }

    @NonNull
    @Override
    public List<SalesTransactionEntity> getUnsyncedSalesTransactions(@NonNull MerchantEntity merchantEntity) {
        Selection<ReactiveResult<SalesTransactionEntity>> query = mDataStore.select(SalesTransactionEntity.class);
        query.where(SalesTransactionEntity.SYNCED.eq(false));
        query.where(SalesTransactionEntity.MERCHANT.eq(merchantEntity));
        return query.get().toList();
    }

    @NonNull
    @Override
    public List<SalesTransactionEntity> getMerchantSalesTransactions(int merchantId) {
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
        /*if (merchantEntity == null) {

        } else {
            Selection<ReactiveResult<Tuple>> query = mDataStore.select(SalesTransactionEntity.class);
            query.where(SalesTransactionEntity.MERCHANT.eq(merchantEntity));
            query.where(SalesTransactionEntity.AMOUNT.sum().as("amount").notNull());
        }*/
        return merchantEntity != null ? merchantEntity.getSalesTransactions() : Collections.<SalesTransactionEntity>emptyList();
    }
}
