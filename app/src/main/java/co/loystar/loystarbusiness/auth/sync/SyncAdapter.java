package co.loystar.loystarbusiness.auth.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.BirthdayOffer;
import co.loystar.loystarbusiness.models.databinders.BirthdayOfferPresetSms;
import co.loystar.loystarbusiness.models.databinders.Customer;
import co.loystar.loystarbusiness.models.databinders.Invoice;
import co.loystar.loystarbusiness.models.databinders.InvoicePaymentHistoriesItem;
import co.loystar.loystarbusiness.models.databinders.ItemsItem;
import co.loystar.loystarbusiness.models.databinders.LoyaltyProgram;
import co.loystar.loystarbusiness.models.databinders.MerchantWrapper;
import co.loystar.loystarbusiness.models.databinders.OrderItem;
import co.loystar.loystarbusiness.models.databinders.Product;
import co.loystar.loystarbusiness.models.databinders.ProductCategory;
import co.loystar.loystarbusiness.models.databinders.Sale;
import co.loystar.loystarbusiness.models.databinders.SalesOrder;
import co.loystar.loystarbusiness.models.databinders.Subscription;
import co.loystar.loystarbusiness.models.databinders.Transaction;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceHistoryEntity;
import co.loystar.loystarbusiness.models.entities.InvoiceTransactionEntity;
import co.loystar.loystarbusiness.models.entities.ItemsItemEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.OrderItemEntity;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SaleEntity;
import co.loystar.loystarbusiness.models.entities.SalesOrderEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.models.entities.SubscriptionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import io.requery.Persistable;
import io.requery.query.Tuple;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Created by ordgen on 11/1/17.
 */

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final AccountManager mAccountManager;
    private ApiClient mApiClient;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;
    private MerchantEntity merchantEntity;
    private Context mContext;
    private ReactiveEntityStore<Persistable> mDataStore;
    private SharedPreferences mSharedPreferences;

    SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
        mApiClient = new ApiClient(context);
        mDatabaseManager = DatabaseManager.getInstance(context);
        mSessionManager = new SessionManager(context);
        mDataStore = DatabaseManager.getDataStore(context);
        mSharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
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
            String mAuthToken = mAccountManager.blockingGetAuthToken(account, AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS, true);
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
            Intent intent = new Intent(Constants.SYNC_STARTED);
            getContext().sendBroadcast(intent);

            syncMerchant();
            syncCustomers();
            syncProductCategories();
            syncLoyaltyPrograms();
            syncProducts();
            syncMerchantSubscription();
            syncMerchantBirthdayOffer();
            syncMerchantBirthdayOfferPresetSms();
            syncInvoices();

            Intent i = new Intent(Constants.SYNC_FINISHED);
            getContext().sendBroadcast(i);
        }

        @Override
        public void syncMerchant() {
            if (merchantEntity.isUpdateRequired()) {
                mApiClient.getLoystarApi(false).updateMerchant(
                        merchantEntity.getFirstName(),
                        merchantEntity.getLastName(),
                        merchantEntity.getEmail(),
                        merchantEntity.getBusinessName(),
                        merchantEntity.getContactNumber(),
                        merchantEntity.getBusinessType(),
                        merchantEntity.getCurrency(),
                        merchantEntity.isPosTurnedOn(),
                        merchantEntity.getSyncFrequency(),
                        merchantEntity.isBluetoothPrintEnabled(),merchantEntity.getAddressLine1(),merchantEntity.getAddressLine2()
                ).enqueue(new Callback<MerchantWrapper>() {
                    @Override
                    public void onResponse(@NonNull Call<MerchantWrapper> call, @NonNull Response<MerchantWrapper> response) {
                        if (response.isSuccessful()) {
                            merchantEntity.setUpdateRequired(false);
                            mDatabaseManager.updateMerchant(merchantEntity);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MerchantWrapper> call, @NonNull Throwable t) {

                    }
                });
            }
        }

        @Override
        public void syncCustomers() {
            /*
            * Fetch total sales on server
            * */
            mApiClient.getLoystarApi(false)
                .getCustomers(1, 1500)
                .flatMapIterable(arrayListResponse -> {
                    ArrayList<Customer> customers = arrayListResponse.body();

                    int getTotal = customers.size();
                        /*
                         * Save total customers figure so we can track if we have full customers data locally
                         * */
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putInt(Constants.TOTAL_CUSTOMERS_ON_SERVER, getTotal);
                        editor.apply();
                    if (customers == null || customers.isEmpty()) {
                        // since sales can be created without customer record
                        // without customers we should still sync sales
                        syncSales();
                        return Collections.emptyList();
                    } else {
                        return new ArrayList<>(customers.subList(0, 1));
                    }
                }).subscribe(customer -> {
                int totalCustomersOnServer = mSharedPreferences.getInt(Constants.TOTAL_CUSTOMERS_ON_SERVER, 0);
                double getNumberOfTrips = Math.floor((double) totalCustomersOnServer / 1500);
                int numberOfTrips = (int) getNumberOfTrips + 1;

                Timber.e("TOTAL_CUSTOMERS_ON_SERVER: %s", totalCustomersOnServer);

                for (int i = 0; i < numberOfTrips; i ++) {
                    Integer totalCustomersLocally = mDataStore.count(CustomerEntity.class).get().value();
                    if (totalCustomersLocally != null) {
                        if (totalCustomersLocally == totalCustomersOnServer) {
                            if (i + 1 == numberOfTrips) {
                                syncSales();
                            }
                            continue;
                        }
                        Timber.e("TOTAL_CUSTOMERS_LOCALLY: %s", totalCustomersLocally);
                        double page = Math.floor((double) totalCustomersLocally / 1500);

                        mApiClient.getLoystarApi(false).getCustomers((int) page + 1, 1500)
                            .flatMapIterable(response -> {
                                ArrayList<Customer> customers = response.body();
                                if (customers == null || customers.isEmpty()) {
                                    return Collections.emptyList();
                                } else {
                                    return customers;
                                }
                            }).subscribe(newCustomer -> {
                            if (newCustomer.isDeleted() != null && newCustomer.isDeleted()) {
                                CustomerEntity existingRecord = mDatabaseManager.getCustomerById(newCustomer.getId());
                                if (existingRecord != null) {
                                    mDataStore.delete(existingRecord).subscribe(/*no-op*/);
                                }
                            } else {
                                CustomerEntity customerEntity = new CustomerEntity();
                                customerEntity.setId(newCustomer.getId());
                                if (newCustomer.getEmail() != null && !newCustomer.getEmail().contains("yopmail.com")) {
                                    customerEntity.setEmail(newCustomer.getEmail());
                                }
                                customerEntity.setFirstName(newCustomer.getFirst_name());
                                customerEntity.setDeleted(false);
                                customerEntity.setLastName(newCustomer.getLast_name());
                                customerEntity.setSex(newCustomer.getSex());
                                customerEntity.setDateOfBirth(newCustomer.getDate_of_birth());
                                customerEntity.setPhoneNumber(newCustomer.getPhone_number());
                                customerEntity.setUserId(newCustomer.getUser_id());
                                customerEntity.setCreatedAt(new Timestamp(newCustomer.getCreated_at().getMillis()));
                                customerEntity.setUpdatedAt(new Timestamp(newCustomer.getUpdated_at().getMillis()));
                                customerEntity.setOwner(merchantEntity);

                                CustomerEntity oldEntity = mDataStore.select(CustomerEntity.class).where(CustomerEntity.PHONE_NUMBER.eq(newCustomer.getPhone_number())).get().firstOrNull();
                                if (oldEntity == null) {
                                    mDataStore.upsert(customerEntity).subscribe(/*np-op*/);
                                } else {
                                    Timber.e("CustomerEntity: %s", oldEntity.getId());
                                }
                            }
                        }, Timber::e);
                    }
                    if (i + 1 == numberOfTrips) {
                        syncSales();
                    }
                }
            }, Timber::e);

            /* sync customers marked for deletion*/
            List<CustomerEntity> customersMarkedForDeletion = mDatabaseManager.getCustomersMarkedForDeletion(merchantEntity);
            for (final CustomerEntity customerEntity: customersMarkedForDeletion) {
                mApiClient.getLoystarApi(false).setCustomerDeleteFlagToTrue(customerEntity.getId()).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            mDatabaseManager.deleteCustomer(customerEntity);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

                    }
                });
            }
        }

        @Override
        public void syncSales() {
            mApiClient.getLoystarApi(false).getSales(1, 500)
                .flatMapIterable(arrayListResponse -> {
                    String getTotal = arrayListResponse.headers().get("Total");
                    if (!TextUtils.isEmpty(getTotal)) {
                         /*
                        * Save total customers figure so we can track if we have full customers data locally
                        * */
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putInt(Constants.TOTAL_SALES_ON_SERVER, Integer.parseInt(getTotal));
                        editor.apply();
                    }

                    ArrayList<Sale> sales = arrayListResponse.body();
                    if (sales == null || sales.isEmpty()) {
                        // since we do not have to have existing sales
                        // before uploading new one
                        uploadNewSales();
                        return Collections.emptyList();
                    } else {
                        return new ArrayList<>(sales.subList(0, 1));
                    }
                }).subscribe(sale -> {
                int totalSalesOnServer = mSharedPreferences.getInt(Constants.TOTAL_SALES_ON_SERVER, 0);
                double getNumberOfTrips = Math.floor((double) totalSalesOnServer /500);
                int numberOfTrips = (int) getNumberOfTrips + 1;

                for (int i = 0; i < numberOfTrips; i ++) {
                    Integer totalSalesLocally = mDataStore.count(SaleEntity.class)
                            .where(SaleEntity.SYNCED.eq(true)).get().value();
                    if (totalSalesLocally != null) {
                        if (totalSalesLocally == totalSalesOnServer) {
                            if (i + 1 == numberOfTrips) {
                                uploadNewSales();
                                Intent intent = new Intent(Constants.SALES_TRANSACTIONS_SYNC_FINISHED);
                                getContext().sendBroadcast(intent);
                            }
                            continue;
                        }
                        Timber.e("TOTAL_SALES_LOCALLY: %s", totalSalesLocally);
                        double page = Math.floor((double) totalSalesLocally / 500);

                        mApiClient.getLoystarApi(false)
                            .getSales((int) page + 1, 500)
                            .flatMapIterable(arrayListResponse -> {
                                ArrayList<Sale> sales = arrayListResponse.body();
                                if (sales == null || sales.isEmpty()) {
                                    return Collections.emptyList();
                                } else {
                                    return sales;
                                }
                            })
                            .subscribe(newSale -> {
                                SaleEntity newSaleEntity = new SaleEntity();
                                newSaleEntity.setId(newSale.getId());
                                newSaleEntity.setCreatedAt(new Timestamp(newSale.getCreated_at().getMillis()));
                                newSaleEntity.setUpdatedAt(new Timestamp(newSale.getUpdated_at().getMillis()));
                                newSaleEntity.setMerchant(merchantEntity);
                                newSaleEntity.setPayedWithCard(newSale.isPaid_with_card());
                                newSaleEntity.setPayedWithCash(newSale.isPaid_with_cash());
                                newSaleEntity.setPayedWithMobile(newSale.isPaid_with_mobile());
                                newSaleEntity.setTotal(newSale.getTotal());
                                newSaleEntity.setSynced(true);

                                CustomerEntity customerEntity = mDatabaseManager.getCustomerByUserId(newSale.getUser_id());
                                newSaleEntity.setCustomer(customerEntity);


                                mDataStore.upsert(newSaleEntity).subscribe(saleEntity -> {
                                    for (Transaction transaction: newSale.getTransactions()) {
                                        SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
                                        transactionEntity.setId(transaction.getId());
                                        transactionEntity.setAmount(transaction.getAmount());
                                        transactionEntity.setMerchantLoyaltyProgramId(transaction.getMerchant_loyalty_program_id());
                                        transactionEntity.setPoints(transaction.getPoints());
                                        transactionEntity.setStamps(transaction.getStamps());
                                        transactionEntity.setCreatedAt(new Timestamp(transaction.getCreated_at().getMillis()));
                                        transactionEntity.setProductId(transaction.getProduct_id());
                                        transactionEntity.setSynced(true);
                                        transactionEntity.setProgramType(transaction.getProgram_type());
                                        transactionEntity.setUserId(transaction.getUser_id());

                                        transactionEntity.setSale(saleEntity);
                                        transactionEntity.setMerchant(merchantEntity);
                                        transactionEntity.setCustomer(saleEntity.getCustomer());

                                        mDataStore.upsert(transactionEntity).subscribe(/*no-op*/);
                                    }
                                });
                            }, Timber::e);
                    }
                    if (i + 1 == numberOfTrips) {
                        uploadNewSales();
                        Intent intent = new Intent(Constants.SALES_TRANSACTIONS_SYNC_FINISHED);
                        getContext().sendBroadcast(intent);
                    }
                }

            }, Timber::e);
        }

        @Override
        public void uploadNewSales() {
            SharedPreferences sharedPreferences =
                    mContext.getSharedPreferences(mContext.getString(R.string.preference_file_key), 0);
            String deviceId = sharedPreferences
                    .getString(Constants.FIREBASE_REGISTRATION_TOKEN, "");

            for (SaleEntity saleEntity: mDatabaseManager.getUnsyncedSaleEnties(merchantEntity)) {
                try {
                    JSONObject jsonObjectData = new JSONObject();
                    if (saleEntity.getCustomer() != null) {
                        jsonObjectData.put("user_id", saleEntity.getCustomer().getUserId());
                    }
                    jsonObjectData.put("device_id", deviceId);
                    jsonObjectData.put("is_paid_with_cash", saleEntity.isPayedWithCash());
                    jsonObjectData.put("is_paid_with_card", saleEntity.isPayedWithCard());
                    jsonObjectData.put("is_paid_with_mobile", saleEntity.isPayedWithMobile());

                    JSONArray jsonArray = new JSONArray();

                    for (SalesTransactionEntity transactionEntity: saleEntity.getTransactions()) {
                        LoyaltyProgramEntity programEntity = mDatabaseManager
                                .getLoyaltyProgramById(
                                        transactionEntity.getMerchantLoyaltyProgramId());
                        if (programEntity != null) {
                            JSONObject jsonObject = new JSONObject();

                            if (transactionEntity.getUserId() > 0) {
                                jsonObject.put("user_id", transactionEntity.getUserId());
                            }
                            jsonObject.put("merchant_id", merchantEntity.getId());
                            jsonObject.put("amount", transactionEntity.getAmount());

                            if (transactionEntity.getProductId() > 0) {
                                jsonObject.put("product_id", transactionEntity.getProductId());
                            }

                            jsonObject.put("merchant_loyalty_program_id",
                                    transactionEntity.getMerchantLoyaltyProgramId());
                            jsonObject.put("program_type", transactionEntity.getProgramType());
                            if (programEntity.getProgramType().equals(getContext().getString(R.string.simple_points))) {
                                jsonObject.put("points", transactionEntity.getPoints());
                            }
                            else if (programEntity.getProgramType().equals(getContext().getString(R.string.stamps_program))) {
                                jsonObject.put("stamps", transactionEntity.getStamps());
                            }

                            jsonArray.put(jsonObject);
                        }
                    }

                    jsonObjectData.put("transactions", jsonArray);
                    JSONObject requestData = new JSONObject();
                    requestData.put("sale", jsonObjectData);

                    RequestBody requestBody = RequestBody
                            .create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                    mApiClient.getLoystarApi(false)
                            .createSale(requestBody).enqueue(new Callback<Sale>() {
                        @Override
                        public void onResponse(@NonNull Call<Sale> call, @NonNull Response<Sale> response) {
                            if (response.isSuccessful()) {
                                Sale sale = response.body();
                                if (sale != null) {
                                    // lets delete old records
                                    for (int i = 0; i < saleEntity.getTransactions().size(); i ++) {
                                        // delete transaction
                                        mDataStore.delete(saleEntity.getTransactions().get(i)).subscribe();
                                        if (i + 1 == saleEntity.getTransactions().size()) {
                                            // delete saleEntity
                                            // calling mDataStore.delete(saleEntity) here
                                            // throws an Exception
                                            String query = "DELETE FROM Sale WHERE ROWID=" + saleEntity.getId();
                                            ReactiveResult<Tuple> result = mDataStore.raw(query);
                                            if (result != null && result.first() != null) {
                                                try {
                                                    Integer deletedEntity = result.first().get(0);
                                                    Timber.e("DELETED SALE: %s", deletedEntity);
                                                } catch (ClassCastException e) {
                                                    try {
                                                        Long deletedEntity = result.first().get(0);
                                                        Timber.e("DELETED SALE: %s", deletedEntity);
                                                    } catch (ClassCastException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    CustomerEntity customerEntity = mDatabaseManager.getCustomerByUserId(sale.getUser_id());
                                    SaleEntity newSaleEntity = new SaleEntity();
                                    newSaleEntity.setId(sale.getId());
                                    newSaleEntity.setCreatedAt(new Timestamp(sale.getCreated_at().getMillis()));
                                    newSaleEntity.setUpdatedAt(new Timestamp(sale.getUpdated_at().getMillis()));
                                    newSaleEntity.setMerchant(merchantEntity);
                                    newSaleEntity.setPayedWithCard(sale.isPaid_with_card());
                                    newSaleEntity.setPayedWithCash(sale.isPaid_with_cash());
                                    newSaleEntity.setPayedWithMobile(sale.isPaid_with_mobile());
                                    newSaleEntity.setTotal(sale.getTotal());
                                    newSaleEntity.setSynced(true);
                                    newSaleEntity.setCustomer(customerEntity);

                                    mDataStore.upsert(newSaleEntity).subscribe(saleEntity -> {
                                        for (Transaction transaction: sale.getTransactions()) {
                                            SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
                                            transactionEntity.setId(transaction.getId());
                                            transactionEntity.setAmount(transaction.getAmount());
                                            transactionEntity.setMerchantLoyaltyProgramId(transaction.getMerchant_loyalty_program_id());
                                            transactionEntity.setPoints(transaction.getPoints());
                                            transactionEntity.setStamps(transaction.getStamps());
                                            transactionEntity.setCreatedAt(new Timestamp(transaction.getCreated_at().getMillis()));
                                            transactionEntity.setProductId(transaction.getProduct_id());
                                            transactionEntity.setProgramType(transaction.getProgram_type());
                                            transactionEntity.setUserId(transaction.getUser_id());

                                            transactionEntity.setSale(saleEntity);
                                            transactionEntity.setMerchant(merchantEntity);
                                            transactionEntity.setCustomer(saleEntity.getCustomer());

                                            mDataStore.upsert(transactionEntity).subscribe(/*no-op*/);
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Sale> call, @NonNull Throwable t) {
                            Timber.e(t);
                        }
                    });

                } catch (JSONException e) {
                    Timber.e(e);
                }
            }
            syncSalesOrders();
        }

        @Override
        public void syncSalesOrders() {
            mApiClient.getLoystarApi(false).getOrders(1, 500)
                .flatMapIterable(arrayListResponse -> {
                    String getTotal = arrayListResponse.headers().get("Total");
                    if (!TextUtils.isEmpty(getTotal)) {
                        // Save total customers figure so we can track if we have full customers data locally
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putInt(Constants.TOTAL_ORDERS_ON_SERVER, Integer.parseInt(getTotal));
                        editor.apply();
                    }

                    ArrayList<SalesOrder> salesOrders = arrayListResponse.body();
                    if (salesOrders == null || salesOrders.isEmpty()) {
                        return Collections.emptyList();
                    } else {
                        return new ArrayList<>(salesOrders.subList(0, 1));
                    }
            }).subscribe(s -> {
                int totalOrdersOnServer = mSharedPreferences.getInt(Constants.TOTAL_ORDERS_ON_SERVER, 0);
                double getNumberOfTrips = Math.floor((double) totalOrdersOnServer / 500);
                int numberOfTrips = (int) getNumberOfTrips + 1;

                for (int i = 0; i < numberOfTrips; i ++) {
                    Integer totalOrdersLocally = mDataStore.count(SalesOrderEntity.class).get().value();
                    if (totalOrdersLocally != null) {
                        if (totalOrdersLocally == totalOrdersOnServer) {
                            if (i + 1 == numberOfTrips) {
                                updateSalesOrders();
                            }
                            continue;
                        }
                        Timber.e("TOTAL_ORDERS_LOCALLY: %s", totalOrdersLocally);
                        double page = Math.floor((double) totalOrdersLocally / 500);

                        mApiClient.getLoystarApi(false)
                            .getOrders((int) page + 1, 500)
                            .flatMapIterable(arrayListResponse -> {
                                Timber.e("RES: %s", arrayListResponse.isSuccessful());
                                ArrayList<SalesOrder> salesOrders = arrayListResponse.body();
                                if (salesOrders == null || salesOrders.isEmpty()) {
                                    return Collections.emptyList();
                                } else {
                                    return salesOrders;
                                }
                            }).subscribe(salesOrder -> {
                            CustomerEntity customerEntity = mDatabaseManager.getCustomerByUserId(salesOrder.getUser_id());
                            Timber.e("CUSTOMER: %s", customerEntity);
                            if (customerEntity != null) {
                                SalesOrderEntity salesOrderEntity = new SalesOrderEntity();
                                salesOrderEntity.setMerchant(merchantEntity);
                                salesOrderEntity.setId(salesOrder.getId());
                                salesOrderEntity.setStatus(salesOrder.getStatus());
                                salesOrderEntity.setUpdateRequired(false);
                                salesOrderEntity.setTotal(salesOrder.getTotal());
                                salesOrderEntity.setCreatedAt(new Timestamp(salesOrder.getCreated_at().getMillis()));
                                salesOrderEntity.setUpdatedAt(new Timestamp(salesOrder.getUpdated_at().getMillis()));
                                salesOrderEntity.setCustomer(customerEntity);

                                ArrayList<OrderItemEntity> orderItemEntities = new ArrayList<>();
                                for (OrderItem orderItem: salesOrder.getOrder_items()) {
                                    OrderItemEntity orderItemEntity = new OrderItemEntity();
                                    orderItemEntity.setCreatedAt(new Timestamp(orderItem.getCreated_at().getMillis()));
                                    orderItemEntity.setUpdatedAt(new Timestamp(orderItem.getUpdated_at().getMillis()));
                                    orderItemEntity.setId(orderItem.getId());
                                    orderItemEntity.setQuantity(orderItem.getQuantity());
                                    orderItemEntity.setUnitPrice(orderItem.getUnit_price());
                                    orderItemEntity.setTotalPrice(orderItem.getTotal_price());
                                    ProductEntity productEntity = mDataStore.findByKey(ProductEntity.class, orderItem.getProduct_id()).blockingGet();
                                    if (productEntity != null) {
                                        orderItemEntity.setProduct(productEntity);
                                        orderItemEntities.add(orderItemEntity);
                                    }
                                }

                                if (!orderItemEntities.isEmpty()) {
                                    mDataStore.upsert(salesOrderEntity).subscribe(orderEntity -> {
                                        for (OrderItemEntity orderItemEntity: orderItemEntities) {
                                            orderItemEntity.setSalesOrder(orderEntity);
                                            mDataStore.upsert(orderItemEntity).subscribe(/*no-op*/);
                                        }
                                    });
                                }
                            }
                        }, Timber::e);
                    }
                    if (i + 1 == numberOfTrips) {
                        updateSalesOrders();
                    }
                }

            }, Timber::e);
        }

        @Override
        public void updateSalesOrders() {
            List<SalesOrderEntity> updateRequiredSalesOrders = mDatabaseManager.getUpdateRequiredSalesOrders(merchantEntity);
            try {
                for (SalesOrderEntity salesOrderEntity: updateRequiredSalesOrders) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", salesOrderEntity.getStatus());

                    JSONObject requestData = new JSONObject();
                    requestData.put("order", jsonObject);

                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                    mApiClient.getLoystarApi(false).updateMerchantOrder(salesOrderEntity.getId(), requestBody).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                salesOrderEntity.setUpdateRequired(false);
                                mDataStore.update(salesOrderEntity).subscribe(/*no-op*/);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

                        }
                    });
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public void syncInvoices() {
            mApiClient.getLoystarApi(false)
                    .getInvoices(1, 500)
                    .flatMapIterable(arrayListResponse -> {
                        ArrayList<Invoice> invoices = arrayListResponse.body();
                        int getTotal = invoices.size();

                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putInt(Constants.TOTAL_INVOICES_ON_SERVER, getTotal);
                        editor.apply();

                        if(invoices == null || invoices.isEmpty()) {
                            uploadNewInvoices();
                            return Collections.emptyList();
                        } else {
                            return new ArrayList<>(invoices.subList(0, 1));
                        }
                    }).subscribe(invoice -> {

                int totalInvoicesOnServer = mSharedPreferences.getInt(Constants.TOTAL_INVOICES_ON_SERVER, 0);
                        double getNumberOfTrips = Math.floor((double) totalInvoicesOnServer/500);
                        int numberOfTrips = (int) getNumberOfTrips + 1;

                        Timber.e("TOTAL_INVOICES_ON_SERVER: %S", totalInvoicesOnServer);

                        for (int i = 0; i < numberOfTrips; i++) {
                            Integer totalInvoicesLocally =
                                    mDataStore.count(InvoiceEntity.class)
                                    .where(InvoiceEntity.SYNCED.eq(true)).get().value();
                            if (totalInvoicesLocally != null) {
                                if (totalInvoicesLocally == totalInvoicesOnServer) {
                                    if (i + 1 == numberOfTrips) {
                                        uploadNewInvoices();
                                    }
                                    continue;
                                }
                                Timber.e("TOTAL_INVOICES_ON_LOCALLY: %S", totalInvoicesLocally);
                                double page = Math.floor((double) totalInvoicesLocally / 500);

                                mApiClient.getLoystarApi(false)
                                        .getInvoices((int) page + 1, 1500)
                                        .flatMapIterable(response -> {
                                            ArrayList<Invoice> invoices = response.body();
                                            if (invoices == null || invoices.isEmpty()) {
                                                return Collections.emptyList();
                                            } else {
                                                return invoices;
                                            }
                                        }).subscribe(newInvoice -> {
                                    InvoiceEntity invoiceEntity = new InvoiceEntity();
                                    invoiceEntity.setId(newInvoice.getId());
                                    invoiceEntity.setStatus(newInvoice.getStatus());
                                    invoiceEntity.setNumber(newInvoice.getNumber());
                                    invoiceEntity.setSubTotal(newInvoice.getSubtotal());
                                    invoiceEntity.setDueDate(newInvoice.getDueDate());
                                    invoiceEntity.setPaidAmount(newInvoice.getPaidAmount());
                                    invoiceEntity.setCreatedAt(new Timestamp(newInvoice.getCreatedAt().getMillis()));
                                    invoiceEntity.setUpdatedAt(new Timestamp(newInvoice.getUpdatedAt().getMillis()));
                                    invoiceEntity.setOwner(merchantEntity);
                                    invoiceEntity.setSynced(true);
                                    invoiceEntity.setAmount(newInvoice.getSubtotal());
                                    invoiceEntity.setPaymentMessage(newInvoice.getPaymentMessage());
                                    CustomerEntity customerEntity = mDataStore.findByKey(CustomerEntity.class,
                                            newInvoice.getCustomer().getId()).blockingGet();
                                    invoiceEntity.setCustomer(customerEntity);

                                    mDataStore.upsert(invoiceEntity).subscribe(newInvoiceEntity -> {
                                        for (InvoicePaymentHistoriesItem item : newInvoice.getInvoicePaymentHistories()) {
                                            InvoiceHistoryEntity historyEntity = new InvoiceHistoryEntity();
                                            historyEntity.setId(item.getId());
                                            historyEntity.setInvoice(newInvoiceEntity);
                                            historyEntity.setPaidAmount(item.getPaidAmount());
                                            historyEntity.setPaidAt(item.getPaidAt());
                                            mDataStore.upsert(historyEntity).subscribe();
                                        }
                                        for (ItemsItem itemsItem : newInvoice.getItems()) {
                                            InvoiceTransactionEntity transactionEntity = new InvoiceTransactionEntity();
                                            transactionEntity.setId(newInvoice.getId());
                                            transactionEntity.setUserId(newInvoice.getCustomer().getUser_id());
                                            transactionEntity.setCustomer(customerEntity);
                                            transactionEntity.setMerchantLoyaltyProgramId(
                                                    itemsItem.getProduct().getMerchant_loyalty_program_id());
                                            transactionEntity.setAmount(Double.valueOf(newInvoice.getSubtotal()).intValue());
                                            transactionEntity.setMerchant(merchantEntity);
//                                            transactionEntity.setCreatedAt(newInvoice.getCreatedAt());
                                            transactionEntity.setProductId(itemsItem.getProduct().getId());
                                            transactionEntity.setInvoice(newInvoiceEntity);

//                                            ItemsItemEntity itemsItemEntity = new ItemsItemEntity();
//                                            itemsItemEntity.setAmount(itemsItem.getAmount());
//                                            itemsItemEntity.setSynced(true);
//                                            itemsItemEntity.setInvoice(newInvoiceEntity);
                                            mDataStore.upsert(transactionEntity).subscribe(


                                            );

                                        }
                                    });
                                }, Timber::e);
                            }
                            if (i + 1 == numberOfTrips) {
                                uploadNewInvoices();
                            }
                        }
            },Timber::e);
        }

        @Override
        public void uploadNewInvoices() {
            for(InvoiceEntity invoiceEntity: mDatabaseManager.getUnsyncedInvoiceEntities(merchantEntity)) {
                try {
                        JSONObject jsonObjectData = new JSONObject();
                        if (invoiceEntity.getCustomer() != null) {
                            jsonObjectData.put("user_id", invoiceEntity.getCustomer().getUserId());

                        }
                        if (invoiceEntity.getStatus() != null) {
                            jsonObjectData.put("status", invoiceEntity.getStatus());
                        }
                        if (invoiceEntity.getPaymentMethod() != null) {
                            jsonObjectData.put("payment_method", invoiceEntity.getPaymentMethod());
                        }
                        if (invoiceEntity.getPaymentMessage() != null) {
                            jsonObjectData.put("payment_message", invoiceEntity.getPaymentMessage());
                        }
                        jsonObjectData.put("due_date", invoiceEntity.getDueDate());
                        if (invoiceEntity.getPaidAmount() != null) {
                            if (Double.valueOf(invoiceEntity.getPaidAmount()) >= Double.valueOf(invoiceEntity.getAmount())) {
                                jsonObjectData.put("paid_amount", invoiceEntity.getPaidAmount());
                                jsonObjectData.put("status", "paid");
                            } else if ( Double.valueOf(invoiceEntity.getPaidAmount()) < Double.valueOf(invoiceEntity.getAmount())) {
                                jsonObjectData.put("paid_amount", invoiceEntity.getPaidAmount());
                                jsonObjectData.put("status", "partial");
                            }else {
                                jsonObjectData.put("paid_amount", invoiceEntity.getPaidAmount());
                                jsonObjectData.put("status", "unpaid");
                            }
                        }

                        JSONArray jsonArray = new JSONArray();
                        for (InvoiceTransactionEntity transactionEntity: invoiceEntity.getTransactions()) {
                            LoyaltyProgramEntity programEntity =
                                    mDatabaseManager.getLoyaltyProgramById(
                                            transactionEntity.getMerchantLoyaltyProgramId());
                            if (programEntity != null) {
                                JSONObject jsonObject = new JSONObject();

                                if (transactionEntity.getUserId() > 0) {
                                    jsonObject.put("user_id", transactionEntity.getUserId());
                                }
                                jsonObject.put("merchant_id", merchantEntity.getId());
                                jsonObject.put("amount", transactionEntity.getAmount());

                                if (transactionEntity.getProductId() > 0) {
                                    jsonObject.put("product_id", transactionEntity.getProductId());
                                }
                                jsonObject.put("merchant_loyalty_program_id",
                                        transactionEntity.getMerchantLoyaltyProgramId());
                                jsonObject.put("program_type", transactionEntity.getProgramType());

                                if (programEntity.getProgramType().equals(getContext().getString(R.string.simple_points))) {
                                    jsonObject.put("points", transactionEntity.getPoints());
                                }
                                else if (programEntity.getProgramType().equals(getContext().getString(R.string.stamps_program))) {
                                    jsonObject.put("stamps", transactionEntity.getStamps());
                                }

                                jsonArray.put(jsonObject);
                            }
                        }
                        jsonObjectData.put("transactions", jsonArray);
                        JSONObject requestData = new JSONObject();
                        requestData.put("data", jsonObjectData);

                        RequestBody requestBody = RequestBody
                                .create(MediaType.parse(
                                        "application/json; charset=utf-8"), requestData.toString());

                        mApiClient
                                .getLoystarApi(false)
                                .createInvoice(requestBody).enqueue(new Callback<Invoice>() {
                            @Override
                            public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                                if (response.isSuccessful()) {
                                    Intent intentEnd = new Intent(Constants.CREATE_INVOICE_ENDED);
                                    Invoice invoice = response.body();
                                    intentEnd.putExtra("responseNumber", invoice.getInvoicePaymentHistories() + "");
                                    getContext().sendBroadcast(intentEnd);
                                    if (invoice != null) {
                                        for (int i=0; i < invoiceEntity.getTransactions().size(); i++) {
                                            mDataStore.delete(invoiceEntity.getTransactions().get(i)).subscribe();
                                            if (i +1 == invoiceEntity.getTransactions().size()) {
                                                String query = "DELETE FROM Invoice WHERE ROWID=" + invoiceEntity.getId();
                                                ReactiveResult<Tuple> result = mDataStore.raw(query);
                                                if (result != null && result.first() != null) {
                                                    try {
                                                        Integer deletedEntity = result.first().get(0);
                                                        Timber.e("DELETED INVOICE: %s", deletedEntity);
                                                    } catch (ClassCastException e) {
                                                        try {
                                                            Long deletedEntity = result.first().get(0);
                                                            Timber.e("DELETED INVOICE: %s", deletedEntity);
                                                        } catch (ClassCastException e1) {
                                                            e1.printStackTrace();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        CustomerEntity customerEntity = mDatabaseManager
                                                .getCustomerByUserId(invoice.getCustomer().getUser_id());
                                        InvoiceEntity newInvoiceEntity =  new InvoiceEntity();
                                        newInvoiceEntity.setId(invoice.getId());
                                        newInvoiceEntity.setAmount(invoice.getSubtotal());
                                        newInvoiceEntity.setSubTotal(invoice.getSubtotal());
                                        newInvoiceEntity.setCreatedAt(new Timestamp(invoice.getCreatedAt().getMillis()));
                                        newInvoiceEntity.setUpdatedAt(new Timestamp(invoice.getUpdatedAt().getMillis()));
                                        newInvoiceEntity.setCustomer(customerEntity);
                                        newInvoiceEntity.setSynced(true);
                                        newInvoiceEntity.setNumber(invoice.getNumber());
                                        newInvoiceEntity.setStatus(invoice.getStatus());
                                        newInvoiceEntity.setOwner(merchantEntity);
                                        newInvoiceEntity.setDueDate(invoice.getDueDate());
                                        newInvoiceEntity.setPaymentMessage(invoice.getPaymentMessage());

                                        mDataStore.upsert(newInvoiceEntity).subscribe(invoiceEntity -> {
                                            for (InvoicePaymentHistoriesItem item : invoice.getInvoicePaymentHistories()) {
                                                InvoiceHistoryEntity historyEntity = new InvoiceHistoryEntity();
                                                historyEntity.setId(item.getId());
                                                historyEntity.setInvoice(invoiceEntity);
                                                historyEntity.setPaidAmount(item.getPaidAmount());
                                                historyEntity.setPaidAt(item.getPaidAt());
                                                mDataStore.upsert(historyEntity).subscribe();
                                            }
                                            for (ItemsItem transaction: invoice.getItems()) {
                                                InvoiceTransactionEntity transactionEntity = new InvoiceTransactionEntity();
                                                transactionEntity.setId(invoice.getId());
                                                transactionEntity.setUserId(invoice.getCustomer().getUser_id());
                                                transactionEntity.setMerchantLoyaltyProgramId(
                                                        transaction.getProduct().getMerchant_loyalty_program_id());
                                                transactionEntity.setAmount(Double.valueOf(invoice.getSubtotal()).intValue());
                                                transactionEntity.setMerchant(merchantEntity);
                                                transactionEntity.setProductId(transaction.getProduct().getId());
                                                transactionEntity.setCustomer(customerEntity);
                                                transactionEntity.setInvoice(invoiceEntity);
                                                mDataStore.upsert(transactionEntity).subscribe(
                                                );
                                            }
                                        });
                                    }
                                } else {
                                    try {
                                        Intent intentEnd = new Intent(Constants.CREATE_INVOICE_ENDED);
                                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                                        String error = jObjError.getString("error");
                                        intentEnd.putExtra("errorbody", error);
                                        intentEnd.putExtra("responseCode", response.code());
                                        getContext().sendBroadcast(intentEnd);
                                    } catch (Exception exception) {
                                        Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<Invoice> call, Throwable t) {
                                Intent intentEnd = new Intent(Constants.CREATE_INVOICE_ENDED);
                                getContext().sendBroadcast(intentEnd);
                                Timber.e(t);
                            }
                        });
                } catch (JSONException e) {
                    Timber.e(e);
                }
            }
        }

        @Override
        public void syncMerchantSubscription() {
            mApiClient.getLoystarApi(false).getMerchantSubscription().enqueue(new Callback<Subscription>() {
                @Override
                public void onResponse(@NonNull Call<Subscription> call, @NonNull Response<Subscription> response) {
                    if (response.isSuccessful()) {
                        Subscription subscription = response.body();
                        if (subscription != null) {
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
                                mDatabaseManager.updateMerchant(merchant);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Subscription> call, @NonNull Throwable t) {}
            });
        }

        @Override
        public void syncMerchantBirthdayOffer() {
            mApiClient.getLoystarApi(false).getMerchantBirthdayOffer().enqueue(new Callback<BirthdayOffer>() {
                @Override
                public void onResponse(@NonNull Call<BirthdayOffer> call, @NonNull Response<BirthdayOffer> response) {
                    if (response.isSuccessful()) {
                        BirthdayOffer birthdayOffer = response.body();
                        if (birthdayOffer != null) {
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
                        }
                    } else if (response.code() == 404){
                        BirthdayOfferEntity existingOffer = merchantEntity.getBirthdayOffer();
                        if (existingOffer != null) {
                            merchantEntity.setBirthdayOffer(null);
                            mDatabaseManager.updateMerchant(merchantEntity);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BirthdayOffer> call, @NonNull Throwable t) {}
            });
        }

        @Override
        public void syncMerchantBirthdayOfferPresetSms() {
            mApiClient.getLoystarApi(false).getMerchantBirthdayPresetSms().enqueue(new Callback<BirthdayOfferPresetSms>() {
                @Override
                public void onResponse(@NonNull Call<BirthdayOfferPresetSms> call, @NonNull Response<BirthdayOfferPresetSms> response) {
                    if (response.isSuccessful()) {
                        BirthdayOfferPresetSms birthdayOfferPresetSms = response.body();
                        if (birthdayOfferPresetSms != null) {
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
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BirthdayOfferPresetSms> call, @NonNull Throwable t) {}
            });
        }

        @Override
        public void syncProductCategories() {
            try {
                String timeStamp = mDatabaseManager.getProductCategoriesLastRecordDate(merchantEntity);
                JSONObject jsonObjectData = new JSONObject();
                if (timeStamp == null) {
                    jsonObjectData.put("time_stamp", 0);
                } else {
                    jsonObjectData.put("time_stamp", timeStamp);
                }
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                Call<ArrayList<ProductCategory>> call = mApiClient.getLoystarApi(false).getLatestMerchantProductCategories(requestBody);
                Response<ArrayList<ProductCategory>> response = call.execute();

                if (response.isSuccessful()) {
                    ArrayList<ProductCategory> productCategories = response.body();
                    if (productCategories != null) {
                        for (ProductCategory productCategory: productCategories) {
                            if (productCategory.isDeleted() != null && productCategory.isDeleted()) {
                                ProductCategoryEntity existingProductCategory = mDatabaseManager.getProductCategoryById(productCategory.getId());
                                if (existingProductCategory != null) {
                                    mDatabaseManager.deleteProductCategory(existingProductCategory);
                                }
                            } else {
                                ProductCategoryEntity productCategoryEntity = new ProductCategoryEntity();
                                productCategoryEntity.setId(productCategory.getId());
                                productCategoryEntity.setDeleted(false);
                                productCategoryEntity.setName(productCategory.getName());
                                productCategoryEntity.setCreatedAt(new Timestamp(productCategory.getCreated_at().getMillis()));
                                productCategoryEntity.setUpdatedAt(new Timestamp(productCategory.getUpdated_at().getMillis()));
                                productCategoryEntity.setOwner(merchantEntity);

                                mDatabaseManager.insertNewProductCategory(productCategoryEntity);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void syncProducts() {
            try {
                String timeStamp = mDatabaseManager.getMerchantProductsLastRecordDate(merchantEntity);
                JSONObject jsonObjectData = new JSONObject();
                if (timeStamp == null) {
                    jsonObjectData.put("time_stamp", 0);
                } else {
                    jsonObjectData.put("time_stamp", timeStamp);
                }
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                Call<ArrayList<Product>> call = mApiClient.getLoystarApi(false).getLatestMerchantProducts(requestBody);
                Response<ArrayList<Product>> response = call.execute();

                if (response.isSuccessful()) {
                    ArrayList<Product> products = response.body();
                    if (products != null) {
                        for (Product product: products) {
                            if (product.isDeleted() != null && product.isDeleted()) {
                                ProductEntity existingProduct = mDatabaseManager.getProductById(product.getId());
                                if (existingProduct != null) {
                                    mDatabaseManager.deleteProduct(existingProduct);
                                }
                            } else {
                                ProductEntity productEntity = new ProductEntity();
                                productEntity.setId(product.getId());
                                productEntity.setName(product.getName());
                                productEntity.setPicture(product.getPicture());
                                productEntity.setPrice(product.getPrice());
                                productEntity.setCreatedAt(new Timestamp(product.getCreated_at().getMillis()));
                                productEntity.setUpdatedAt(new Timestamp(product.getUpdated_at().getMillis()));
                                productEntity.setDeleted(false);

                                ProductCategoryEntity productCategoryEntity = mDatabaseManager.getProductCategoryById(product.getMerchant_product_category_id());
                                if (productCategoryEntity != null) {
                                    productEntity.setCategory(productCategoryEntity);
                                }
                                LoyaltyProgramEntity programEntity = mDatabaseManager.getLoyaltyProgramById(product.getMerchant_loyalty_program_id());
                                if (programEntity != null) {
                                    productEntity.setLoyaltyProgram(programEntity);
                                }
                                productEntity.setOwner(merchantEntity);
                                mDatabaseManager.insertNewProduct(productEntity);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            /* sync products marked for deletion */
            List<ProductEntity> productsMarkedForDeletion = mDatabaseManager.getProductsMarkedForDeletion(merchantEntity);
            for (final ProductEntity productEntity: productsMarkedForDeletion) {
                mApiClient.getLoystarApi(false).setProductDeleteFlagToTrue(productEntity.getId()).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            mDatabaseManager.deleteProduct(productEntity);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

                    }
                });
            }
        }

        @Override
        public void syncLoyaltyPrograms() {
            try {
                String timeStamp = mDatabaseManager.getMerchantLoyaltyProgramsLastRecordDate(merchantEntity);
                JSONObject jsonObjectData = new JSONObject();
                if (timeStamp == null) {
                    jsonObjectData.put("time_stamp", 0);
                } else {
                    jsonObjectData.put("time_stamp", timeStamp);
                }
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                Call<ArrayList<LoyaltyProgram>> call = mApiClient.getLoystarApi(false).getMerchantLoyaltyPrograms(requestBody);
                Response<ArrayList<LoyaltyProgram>> response = call.execute();

                if (response.isSuccessful()) {
                    ArrayList<LoyaltyProgram> loyaltyPrograms = response.body();
                    if (loyaltyPrograms != null) {
                        for (LoyaltyProgram loyaltyProgram: loyaltyPrograms) {
                            if (loyaltyProgram.isDeleted() != null && loyaltyProgram.isDeleted()) {
                                LoyaltyProgramEntity existingProgram = mDatabaseManager.getLoyaltyProgramById(loyaltyProgram.getId());
                                if (existingProgram != null) {
                                    mDatabaseManager.deleteLoyaltyProgram(existingProgram);
                                }
                            } else {
                                LoyaltyProgramEntity loyaltyProgramEntity = new LoyaltyProgramEntity();
                                loyaltyProgramEntity.setId(loyaltyProgram.getId());
                                loyaltyProgramEntity.setName(loyaltyProgram.getName());
                                loyaltyProgramEntity.setProgramType(loyaltyProgram.getProgram_type());
                                loyaltyProgramEntity.setReward(loyaltyProgram.getReward());
                                loyaltyProgramEntity.setThreshold(loyaltyProgram.getThreshold());
                                loyaltyProgramEntity.setCreatedAt(new Timestamp(loyaltyProgram.getCreated_at().getMillis()));
                                loyaltyProgramEntity.setUpdatedAt(new Timestamp(loyaltyProgram.getUpdated_at().getMillis()));
                                loyaltyProgramEntity.setDeleted(false);

                                loyaltyProgramEntity.setOwner(merchantEntity);
                                mDatabaseManager.insertNewLoyaltyProgram(loyaltyProgramEntity);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            /* sync loyaltyPrograms marked for deletion */
            List<LoyaltyProgramEntity> loyaltyProgramsMarkedForDeletion = mDatabaseManager.getLoyaltyProgramsMarkedForDeletion(merchantEntity);
            for (final LoyaltyProgramEntity loyaltyProgramEntity: loyaltyProgramsMarkedForDeletion) {
                mApiClient.getLoystarApi(false).setMerchantLoyaltyProgramDeleteFlagToTrue(loyaltyProgramEntity.getId()).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            mDatabaseManager.deleteLoyaltyProgram(loyaltyProgramEntity);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

                    }
                });
            }
        }
    }

    /**
     * Manually force Android to perform a sync with our SyncAdapter.
     */
    public static void performSync(Context context, String accountName) {
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        Account account = AccountGeneral.getUserAccount(context, accountName);
        if (account == null) {
            return;
        }
        ContentResolver.requestSync(account, AccountGeneral.AUTHORITY, b);
    }
}
