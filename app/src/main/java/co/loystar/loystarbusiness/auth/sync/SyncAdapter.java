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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.BirthdayOffer;
import co.loystar.loystarbusiness.models.databinders.BirthdayOfferPresetSms;
import co.loystar.loystarbusiness.models.databinders.Customer;
import co.loystar.loystarbusiness.models.databinders.Subscription;
import co.loystar.loystarbusiness.models.databinders.Transaction;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferEntity;
import co.loystar.loystarbusiness.models.entities.BirthdayOfferPresetSmsEntity;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SubscriptionEntity;
import co.loystar.loystarbusiness.models.entities.TransactionEntity;
import okhttp3.MediaType;
import okhttp3.RequestBody;
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
                            mDatabaseManager.insertNewMerchant(merchant);
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
                    for (Transaction transaction: transactions) {
                        CustomerEntity customerEntity = mDatabaseManager.getCustomerById(transaction.getCustomer_id());
                        TransactionEntity transactionEntity = new TransactionEntity();
                        transactionEntity.setId(transaction.getId());
                        transactionEntity.setAmount(transaction.getAmount());
                        transactionEntity.setMerchantLoyaltyProgramId(transaction.getMerchant_loyalty_program_id());
                        transactionEntity.setPoints(transaction.getPoints());
                        transactionEntity.setStamps(transaction.getStamps());
                        transactionEntity.setSynced(transaction.isSynced());
                        transactionEntity.setCreatedAt(new Timestamp(transaction.getCreated_at().getMillis()));
                        transactionEntity.setProductId(transaction.getProduct_id());
                        transactionEntity.setProgramType(transaction.getProgram_type());
                        transactionEntity.setUserId(transaction.getUser_id());

                        transactionEntity.setMerchant(merchantEntity);
                        if (customerEntity != null) {
                            transactionEntity.setCustomer(customerEntity);
                        }
                        mDatabaseManager.insertNewTransaction(transactionEntity);
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

            List<TransactionEntity> unsyncedTransactions = mDatabaseManager.getUnsyncedTransactions(merchantEntity);
            if (!unsyncedTransactions.isEmpty()) {
                /*Upload transactions that have not been synced with the server*/
                for (final TransactionEntity transactionEntity : unsyncedTransactions) {
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
                                            mDatabaseManager.deleteTransaction(transactionEntity);
                                            Transaction transaction = response.body();

                                            TransactionEntity transactionEntity = new TransactionEntity();
                                            transactionEntity.setId(transaction.getId());
                                            transactionEntity.setAmount(transaction.getAmount());
                                            transactionEntity.setMerchantLoyaltyProgramId(transaction.getMerchant_loyalty_program_id());
                                            transactionEntity.setPoints(transaction.getPoints());
                                            transactionEntity.setStamps(transaction.getStamps());
                                            transactionEntity.setSynced(transaction.isSynced());
                                            transactionEntity.setCreatedAt(new Timestamp(transaction.getCreated_at().getMillis()));
                                            transactionEntity.setProductId(transaction.getProduct_id());
                                            transactionEntity.setProgramType(transaction.getProgram_type());
                                            transactionEntity.setUserId(transaction.getUser_id());

                                            transactionEntity.setMerchant(merchantEntity);
                                            transactionEntity.setCustomer(customer);
                                            mDatabaseManager.insertNewTransaction(transactionEntity);
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
                    for (Transaction transaction: transactions) {

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
        }

        @Override
        public void syncProducts() {
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
                    for (Transaction transaction: transactions) {

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
        }

        @Override
        public void syncLoyaltyPrograms() {
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
                    for (Transaction transaction: transactions) {

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
