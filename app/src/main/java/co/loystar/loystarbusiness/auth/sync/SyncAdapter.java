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
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.BirthdayOffer;
import co.loystar.loystarbusiness.models.databinders.BirthdayOfferPresetSms;
import co.loystar.loystarbusiness.models.databinders.Customer;
import co.loystar.loystarbusiness.models.databinders.LoyaltyProgram;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.databinders.MerchantWrapper;
import co.loystar.loystarbusiness.models.databinders.Product;
import co.loystar.loystarbusiness.models.databinders.ProductCategory;
import co.loystar.loystarbusiness.models.databinders.Subscription;
import co.loystar.loystarbusiness.models.databinders.Transaction;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.models.entities.SubscriptionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.SalesTransactionsSyncObserver;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
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

    SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
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
            Intent intent = new Intent(Constants.SYNC_STARTED);
            getContext().sendBroadcast(intent);
            syncMerchant();
            syncCustomers();
            syncTransactions();
            syncProductCategories();
            syncProducts();
            syncLoyaltyPrograms();
            syncMerchantSubscription();
            syncMerchantBirthdayOffer();
            syncMerchantBirthdayOfferPresetSms();

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
                        merchantEntity.isPosTurnedOn()
                ).enqueue(new Callback<MerchantWrapper>() {
                    @Override
                    public void onResponse(Call<MerchantWrapper> call, Response<MerchantWrapper> response) {
                        if (response.isSuccessful()) {
                            merchantEntity.setUpdateRequired(false);
                            mDatabaseManager.updateMerchant(merchantEntity);
                        } else {
                            if (response.code() == 401) {
                                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<MerchantWrapper> call, Throwable t) {

                    }
                });
            }
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
                            mDatabaseManager.updateMerchant(merchant);
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
                        } else if (response.code() == 404) {
                            BirthdayOfferEntity existingOffer = merchantEntity.getBirthdayOffer();
                            if (existingOffer != null) {
                                merchantEntity.setBirthdayOffer(null);
                                mDatabaseManager.updateMerchant(merchantEntity);
                            }
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

        @Override
        public void syncCustomers() {
            try {
                String timeStamp = mDatabaseManager.getMerchantCustomersLastRecordDate(merchantEntity);
                JSONObject jsonObjectData = new JSONObject();
                if (timeStamp == null) {
                    jsonObjectData.put("time_stamp", 0);
                } else {
                    jsonObjectData.put("time_stamp", timeStamp);
                }
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                Call<ArrayList<Customer>> call = mApiClient.getLoystarApi(false).getLatestMerchantCustomers(requestBody);
                Response<ArrayList<Customer>> response = call.execute();

                if (response.isSuccessful()) {
                    ArrayList<Customer> customers = response.body();
                    for (Customer customer: customers) {
                        if (customer.isDeleted() != null && customer.isDeleted()) {
                            CustomerEntity existingRecord = mDatabaseManager.getCustomerById(customer.getId());
                            if (existingRecord != null) {
                                mDatabaseManager.deleteCustomer(existingRecord);
                            }
                        } else {
                            CustomerEntity customerEntity = new CustomerEntity();
                            customerEntity.setId(customer.getId());
                            customerEntity.setEmail(customer.getEmail());
                            customerEntity.setFirstName(customer.getFirst_name());
                            customerEntity.setDeleted(false);
                            customerEntity.setLastName(customer.getLast_name());
                            customerEntity.setSex(customer.getSex());
                            customerEntity.setDateOfBirth(customer.getDate_of_birth());
                            customerEntity.setPhoneNumber(customer.getPhone_number());
                            customerEntity.setUserId(customer.getUser_id());
                            customerEntity.setCreatedAt(new Timestamp(customer.getCreated_at().getMillis()));
                            customerEntity.setUpdatedAt(new Timestamp(customer.getUpdated_at().getMillis()));
                            customerEntity.setOwner(merchantEntity);

                            mDatabaseManager.insertNewCustomer(customerEntity);
                        }
                    }

                }  else {
                    if (response.code() == 401) {
                        mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            /* sync customers marked for deletion*/
            List<CustomerEntity> customersMarkedForDeletion = mDatabaseManager.getCustomersMarkedForDeletion(merchantEntity);
            for (final CustomerEntity customerEntity: customersMarkedForDeletion) {
                mApiClient.getLoystarApi(false).setCustomerDeleteFlagToTrue(String.valueOf(customerEntity.getId())).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            mDatabaseManager.deleteCustomer(customerEntity);
                        } else {
                            if (response.code() == 401) {
                                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });
            }
        }

        @Override
        public void syncTransactions() {
            try {
                String timeStamp = mDatabaseManager.getMerchantTransactionsLastRecordDate(merchantEntity);
                JSONObject jsonObjectData = new JSONObject();
                if (timeStamp == null) {
                    jsonObjectData.put("time_stamp", 0);
                } else {
                    jsonObjectData.put("time_stamp", timeStamp);
                }
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                Call<ArrayList<Transaction>> call = mApiClient.getLoystarApi(false).getLatestTransactions(requestBody);
                Response<ArrayList<Transaction>> response = call.execute();

                if (response.isSuccessful()) {
                    ArrayList<Transaction> transactions = response.body();
                    if (transactions != null) {
                        for (Transaction transaction: transactions) {
                            CustomerEntity customerEntity = mDatabaseManager.getCustomerById(transaction.getCustomer_id());
                            SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
                            transactionEntity.setId(transaction.getId());
                            transactionEntity.setAmount(transaction.getAmount());
                            transactionEntity.setMerchantLoyaltyProgramId(transaction.getMerchant_loyalty_program_id());
                            transactionEntity.setPoints(transaction.getPoints());
                            transactionEntity.setStamps(transaction.getStamps());
                            transactionEntity.setSynced(true);
                            transactionEntity.setCreatedAt(new Timestamp(transaction.getCreated_at().getMillis()));
                            transactionEntity.setProductId(transaction.getProduct_id());
                            transactionEntity.setProgramType(transaction.getProgram_type());
                            transactionEntity.setUserId(transaction.getUser_id());

                            transactionEntity.setMerchant(merchantEntity);
                            if (customerEntity != null) {
                                transactionEntity.setCustomer(customerEntity);
                            }
                            mDatabaseManager.insertNewSalesTransaction(transactionEntity);
                        }
                        Intent i = new Intent(Constants.SALES_TRANSACTIONS_SYNC_FINISHED);
                        getContext().sendBroadcast(i);

                    }
                } else {
                    if (response.code() == 401) {
                        mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            List<SalesTransactionEntity> unsyncedTransactions = mDatabaseManager.getUnsyncedSalesTransactions(merchantEntity);
            if (!unsyncedTransactions.isEmpty()) {
                // Upload transactions that have not been synced with the server
                for (final SalesTransactionEntity transactionEntity : unsyncedTransactions) {
                    LoyaltyProgramEntity programEntity = mDatabaseManager.getLoyaltyProgramById(transactionEntity.getMerchantLoyaltyProgramId());
                    final CustomerEntity customer = mDatabaseManager.getCustomerById(transactionEntity.getCustomer().getId());
                    if (customer != null) {

                        try {

                            JSONObject jsonObjectData = new JSONObject();
                            JSONObject requestData = new JSONObject();

                            jsonObjectData.put("merchant_id", merchantEntity.getId());
                            jsonObjectData.put("amount", transactionEntity.getAmount());

                            jsonObjectData.put("product_id", transactionEntity.getProductId());

                            if (programEntity != null) {

                                jsonObjectData.put("merchant_loyalty_program_id", transactionEntity.getMerchantLoyaltyProgramId());
                                jsonObjectData.put("program_type", transactionEntity.getProgramType());
                                int versionCode = BuildConfig.VERSION_CODE;
                                jsonObjectData.put("android_app_version_code", versionCode);


                                if (programEntity.getProgramType().equals(getContext().getString(R.string.simple_points))) {
                                    jsonObjectData.put("points", transactionEntity.getPoints());
                                }
                                else if (programEntity.getProgramType().equals(getContext().getString(R.string.stamps_program))) {
                                    jsonObjectData.put("stamps", transactionEntity.getStamps());
                                }

                                requestData.put("data", jsonObjectData);

                                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                                mApiClient.getLoystarApi(false).recordSales(requestBody, String.valueOf(customer.getId())).enqueue(new Callback<Transaction>() {
                                    @Override
                                    public void onResponse(Call<Transaction> call, Response<Transaction> response) {
                                        if (response.isSuccessful()) {
                                            mDatabaseManager.deleteSalesTransaction(transactionEntity);
                                            Transaction transaction = response.body();

                                            SalesTransactionEntity transactionEntity = new SalesTransactionEntity();
                                            transactionEntity.setId(transaction.getId());
                                            transactionEntity.setAmount(transaction.getAmount());
                                            transactionEntity.setMerchantLoyaltyProgramId(transaction.getMerchant_loyalty_program_id());
                                            transactionEntity.setPoints(transaction.getPoints());
                                            transactionEntity.setStamps(transaction.getStamps());
                                            transactionEntity.setSynced(true);
                                            transactionEntity.setCreatedAt(new Timestamp(transaction.getCreated_at().getMillis()));
                                            transactionEntity.setProductId(transaction.getProduct_id());
                                            transactionEntity.setProgramType(transaction.getProgram_type());
                                            transactionEntity.setUserId(transaction.getUser_id());

                                            transactionEntity.setMerchant(merchantEntity);
                                            transactionEntity.setCustomer(customer);
                                            mDatabaseManager.insertNewSalesTransaction(transactionEntity);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Transaction> call, Throwable t) {
                                        t.printStackTrace();
                                    }
                                });

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
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
                } else {
                    if (response.code() == 401) {
                        mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            /* sync product categories marked for deletion */
            List<ProductCategoryEntity> productCategoriesMarkedForDeletion = mDatabaseManager.getProductCategoriesMarkedForDeletion(merchantEntity);
            for (final ProductCategoryEntity productCategoryEntity: productCategoriesMarkedForDeletion) {
                mApiClient.getLoystarApi(false).setMerchantProductCategoryDeleteFlagToTrue(String.valueOf(productCategoryEntity.getId())).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            mDatabaseManager.deleteProductCategory(productCategoryEntity);
                        } else {
                            if (response.code() == 401) {
                                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });
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
                            productEntity.setOwner(merchantEntity);

                            mDatabaseManager.insertNewProduct(productEntity);
                        }
                    }
                } else {
                    if (response.code() == 401) {
                        mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
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
                mApiClient.getLoystarApi(false).setProductDeleteFlagToTrue(String.valueOf(productEntity.getId())).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            mDatabaseManager.deleteProduct(productEntity);
                        } else {
                            if (response.code() == 401) {
                                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

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
                } else {
                    if (response.code() == 401) {
                        mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
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
                mApiClient.getLoystarApi(false).setMerchantLoyaltyProgramDeleteFlagToTrue(String.valueOf(loyaltyProgramEntity.getId())).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            mDatabaseManager.deleteLoyaltyProgram(loyaltyProgramEntity);
                        } else {
                            if (response.code() == 401) {
                                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, mAuthToken);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

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
        ContentResolver.requestSync(AccountGeneral.getAccount(context, accountName), AccountGeneral.AUTHORITY, b);
    }
}
