package co.loystar.loystarbusiness.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncInfo;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.api.pojos.MerchantUpdateSuccessResponse;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBBirthdayOffer;
import co.loystar.loystarbusiness.models.db.DBBirthdayOfferPresetSMS;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.models.db.DBProductCategory;
import co.loystar.loystarbusiness.models.db.DBSubscription;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.TimeUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 7/4/17.
 */

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    // Global variables
    private static final String TAG = SyncAdapter.class.getCanonicalName();
    public static final String SYNC_STARTED = "SyncStarted";
    public static final String SYNC_FINISHED = "SyncFinished";
    public static final String AUTH_FAILURE = "AuthFailure";
    public static SimpleDateFormat ISO8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.UK);
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

    /* Interval at which to sync in seconds
     60 seconds (1min) * 180 = 3 hours*/
    private static final int SYNC_INTERVAL = 60 * 15;
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    private SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
    private ApiClient mApiClient = LoystarApplication.getInstance().getApiClient();
    private DBMerchant merchant;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    /*
   * Specify the code you want to run in the sync adapter. The entire
   * sync adapter runs in a background thread, so you don't have to set
   * up your own background processing.
   */
    @Override
    public void onPerformSync(
            Account account,
            Bundle bundle,
            String s,
            ContentProviderClient contentProviderClient,
            SyncResult syncResult) {

        Intent intent = new Intent(SYNC_STARTED);
        getContext().sendBroadcast(intent);

        merchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());

        if (merchant != null && sessionManager.isTokenValid()) {
            Log.e(TAG, "SYNC HAS STARTED WITH MERCHANT ID ========================> " + sessionManager.getMerchantId());
            Log.e(TAG, "SYNC HAS STARTED WITH MERCHANT TOKEN ========================> " + sessionManager.getAccessToken());
            Log.e(TAG, "SYNC HAS STARTED WITH MERCHANT CLIENT ID ========================> " + sessionManager.getClientKey());
            syncNow();
        }
        else {
            Intent authFailureIntent = new Intent(AUTH_FAILURE);
            getContext().sendBroadcast(authFailureIntent);
        }
    }

    private void syncNow() {
         /*=========================== sync product categories start ==============================================================>*/
        ArrayList<DBProductCategory> categoriesMarkedForDeletion = databaseHelper.getProductCategoriesMarkedForDeletion(merchant.getId());
        if (categoriesMarkedForDeletion.isEmpty()) {
             /*check server for latest product categories
            for each category that is returned if category is found locally we update it else we insert new category*/
            ArrayList<DBProductCategory> productCategories = databaseHelper.listMerchantProductCategories(merchant.getId());
            try {
                JSONObject jsonObjectData = new JSONObject();
                if (productCategories.size() > 0) {
                    Collections.sort(productCategories, new ProductCategoriesUpdatedTimeComparator());
                    jsonObjectData.put("time_stamp", ISO8601DateFormat.format(productCategories.get(productCategories.size() - 1).getUpdated_at()));
                }
                else {
                    jsonObjectData.put("time_stamp", 0);
                }
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                mApiClient.getLoystarApi().getLatestMerchantProductCategories(requestBody).enqueue(new Callback<ArrayList<DBProductCategory>>() {
                    @Override
                    public void onResponse(Call<ArrayList<DBProductCategory>> call, retrofit2.Response<ArrayList<DBProductCategory>> response) {
                        if (response.isSuccessful()) {
                            ArrayList<DBProductCategory> categories = response.body();
                            for (DBProductCategory category: categories) {
                                if (category.getDeleted() != null && category.getDeleted()) {
                                    DBProductCategory oldCategory = databaseHelper.getProductCategoryById(category.getId());
                                    if (oldCategory != null) {
                                        databaseHelper.deleteProductCategory(oldCategory);
                                    }
                                }
                                else {
                                    databaseHelper.insertOrReplaceProductCategory(category);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ArrayList<DBProductCategory>> call, Throwable t) {
                        //Crashlytics.log(2, TAG, t.getMessage());
                    }
                });
            } catch (JSONException e) {
                //Crashlytics.logException(e);
                e.printStackTrace();
            }
        }
        else {
            /*set merchant product category delete flag to true on server*/
            for (final DBProductCategory category: categoriesMarkedForDeletion) {
                mApiClient.getLoystarApi().setMerchantProductCategoryDeleteFlagToTrue(String.valueOf(category.getId())).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Long categoryId = category.getId();
                            if (databaseHelper.deleteProductCategory(category)) {
                                ArrayList<DBProduct> products = databaseHelper.getProductsByCategoryId(categoryId);
                                for (final DBProduct product: products) {
                                    mApiClient.getLoystarApi().setProductDeleteFlagToTrue(String.valueOf(product.getId())).enqueue(new Callback<ResponseBody>() {
                                        @Override
                                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                            if (response.isSuccessful()) {
                                                databaseHelper.deleteProduct(product);
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                                            //Crashlytics.log(2, TAG, t.getMessage());
                                        }
                                    });
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        //Crashlytics.log(2, TAG, t.getMessage());
                    }
                });
            }
        }

        /*=========================== sync product categories end ==============================================================>*/

        /*=========================== sync products start ==============================================================>*/
        ArrayList<DBProduct> productsMarkedForDeletion = databaseHelper.getProductsMarkedForDeletion(merchant.getId());

        if (productsMarkedForDeletion.isEmpty()) {
            /*check server for latest products
            for each product that is returned if product is found locally we update it else we insert new product*/
            try {
                ArrayList<DBProduct> products = databaseHelper.listMerchantProducts(merchant.getId());
                JSONObject jsonObjectData = new JSONObject();
                if (products.size() > 0) {
                    Collections.sort(products, new ProductsUpdatedTimeComparator());
                    jsonObjectData.put("time_stamp", ISO8601DateFormat.format(products.get(products.size() - 1).getUpdated_at()));
                }
                else {
                    jsonObjectData.put("time_stamp", 0);
                }
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                mApiClient.getLoystarApi().getLatestMerchantProducts(requestBody).enqueue(new Callback<ArrayList<DBProduct>>() {
                    @Override
                    public void onResponse(Call<ArrayList<DBProduct>> call, retrofit2.Response<ArrayList<DBProduct>> response) {
                        if (response.isSuccessful()) {
                            ArrayList<DBProduct> productArrayList = response.body();
                            for (DBProduct product: productArrayList) {
                                if (product.getDeleted() != null && product.getDeleted()) {
                                    DBProduct oldProduct = databaseHelper.getProductById(product.getId());
                                    if (oldProduct != null) {
                                        databaseHelper.deleteProduct(oldProduct);
                                    }
                                }
                                else {
                                    databaseHelper.insertOrReplaceProduct(product);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ArrayList<DBProduct>> call, Throwable t) {
                        //Crashlytics.log(2, TAG, t.getMessage());
                    }
                });


            } catch (JSONException e) {
                //Crashlytics.logException(e);
                e.printStackTrace();
            }
        }
        else {
            /*set product delete flag to true on server*/
            for (final DBProduct product: productsMarkedForDeletion) {
                mApiClient.getLoystarApi().setProductDeleteFlagToTrue(String.valueOf(product.getId())).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            databaseHelper.deleteProduct(product);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        //Crashlytics.log(2, TAG, t.getMessage());
                    }
                });
            }
        }

        /*=========================== sync products end ==============================================================>*/


        /*===================> sync transactions start =======================================================================================>*/
        ArrayList<DBTransaction> unsyncedTransactions = databaseHelper.getUnsyncedTransactions(merchant.getId());
        if (unsyncedTransactions.isEmpty()) {
             /*check server for latest transactions we don't have locally*/
            try {
                ArrayList<DBTransaction> transactions = databaseHelper.getSyncedTransactions(merchant.getId());
                JSONObject jsonObjectData = new JSONObject();
                if (transactions.isEmpty()) {
                    jsonObjectData.put("time_stamp", 0);
                }
                else {
                    Collections.sort(transactions, new TransactionsCreatedAtComparator());
                    jsonObjectData.put("time_stamp", ISO8601DateFormat.format(transactions.get(transactions.size() - 1).getCreated_at()));
                }

                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                mApiClient.getLoystarApi().getLatestTransactions(requestBody).enqueue(new Callback<ArrayList<DBTransaction>>() {
                    @Override
                    public void onResponse(Call<ArrayList<DBTransaction>> call, Response<ArrayList<DBTransaction>> response) {
                        if (response.isSuccessful()) {
                            ArrayList<DBTransaction> transactionArrayList = response.body();
                            for (DBTransaction transaction: transactionArrayList) {
                                databaseHelper.insertOrReplaceTransaction(transaction);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ArrayList<DBTransaction>> call, Throwable t) {
                        //Crashlytics.log(2, TAG, t.getMessage());
                    }
                });

            } catch (JSONException e) {
                //Crashlytics.logException(e);
                e.printStackTrace();
            }
        }
        else {
            /*Upload transactions that have not been synced with the server*/
            for (final DBTransaction dbTransaction : unsyncedTransactions) {
                Long userId = dbTransaction.getUser_id();
                DBMerchantLoyaltyProgram program = databaseHelper.getProgramById(dbTransaction.getMerchant_loyalty_program_id(), merchant.getId());
                final DBCustomer customer = databaseHelper.getCustomerByUserId(userId);
                if (customer != null) {

                    try {

                        JSONObject jsonObjectData = new JSONObject();
                        JSONObject requestData = new JSONObject();

                        jsonObjectData.put("merchant_id", merchant.getId().intValue());
                        jsonObjectData.put("local_db_created_at", ISO8601DateFormat.format(dbTransaction.getLocal_db_created_at()));
                        jsonObjectData.put("amount", dbTransaction.getAmount());
                        jsonObjectData.put("notes", dbTransaction.getNotes());
                        jsonObjectData.put("local_db_record_id", dbTransaction.getId().intValue());

                        if (dbTransaction.getProduct_id() != null) {
                            jsonObjectData.put("product_id", String.valueOf(dbTransaction.getProduct_id()));
                        }

                        if (dbTransaction.getSend_sms() != null && !dbTransaction.getSend_sms()) {
                            jsonObjectData.put("send_sms", false);
                        }

                        if (program != null) {

                            jsonObjectData.put("merchant_loyalty_program_id", dbTransaction.getMerchant_loyalty_program_id());
                            jsonObjectData.put("program_type", TextUtilsHelper.trimQuotes(program.getProgram_type()));
                            int versionCode = BuildConfig.VERSION_CODE;
                            jsonObjectData.put("android_app_version_code", versionCode);


                            if (program.getProgram_type().equals(getContext().getString(R.string.simple_points))) {
                                jsonObjectData.put("points", dbTransaction.getPoints());
                                jsonObjectData.put("total_points", databaseHelper.getTotalUserPointsForProgram(customer.getUser_id(), program.getId(), merchant.getId()));
                            }
                            else if (program.getProgram_type().equals(getContext().getString(R.string.stamps_program))) {
                                jsonObjectData.put("stamps", dbTransaction.getStamps());
                                jsonObjectData.put("total_stamps", databaseHelper.getTotalUserStampsForProgram(customer.getUser_id(), program.getId(), merchant.getId()));
                            }


                            requestData.put("data", jsonObjectData);

                            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                            mApiClient.getLoystarApi().recordSales(requestBody, String.valueOf(customer.getId())).enqueue(new Callback<DBTransaction>() {
                                @Override
                                public void onResponse(Call<DBTransaction> call, Response<DBTransaction> response) {
                                    if (response.isSuccessful()) {
                                        DBTransaction transaction = response.body();
                                        dbTransaction.setSynced(true);
                                        if (transaction != null) {
                                            dbTransaction.setCreated_at(transaction.getCreated_at());
                                        }
                                        else {
                                            dbTransaction.setCreated_at(TimeUtils.getCurrentDateAndTime());
                                        }
                                        databaseHelper.updateTransaction(dbTransaction);
                                    }
                                    else if (response.code() == 403) {
                                        dbTransaction.setSynced(true);
                                        databaseHelper.updateTransaction(dbTransaction);
                                    }
                                }

                                @Override
                                public void onFailure(Call<DBTransaction> call, Throwable t) {
                                    //Crashlytics.log(2, TAG, t.getMessage());
                                }
                            });

                        }
                    } catch (JSONException e) {
                        //Crashlytics.logException(e);
                        e.printStackTrace();
                    }
                }
            }
        }

        /*===================== sync transactions end ===============================================================>*/


         /*===================== sync loyalty programs start ===============================================================>*/
        ArrayList<DBMerchantLoyaltyProgram> programsMarkedForDeletion = databaseHelper.getProgramsMarkedForDeletion(merchant.getId());
        if (programsMarkedForDeletion.isEmpty()) {
             /*Check server for latest loyalty programs*/
            try {
                ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms = databaseHelper.listMerchantPrograms(merchant.getId());
                JSONObject jsonObjectData = new JSONObject();
                if (loyaltyPrograms.size() > 0) {
                    Collections.sort(loyaltyPrograms, new ProgramsUpdatedTimeComparator());
                    jsonObjectData.put("time_stamp", ISO8601DateFormat.format(loyaltyPrograms.get(loyaltyPrograms.size() - 1).getUpdated_at()));
                }
                else {
                    jsonObjectData.put("time_stamp", 0);
                }

                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                mApiClient.getLoystarApi().getMerchantLoyaltyPrograms(requestBody).enqueue(new Callback<ArrayList<DBMerchantLoyaltyProgram>>() {
                    @Override
                    public void onResponse(Call<ArrayList<DBMerchantLoyaltyProgram>> call, Response<ArrayList<DBMerchantLoyaltyProgram>> response) {
                        if (response.isSuccessful()) {
                            ArrayList<DBMerchantLoyaltyProgram> programArrayList = response.body();
                            for (DBMerchantLoyaltyProgram program: programArrayList) {
                                if (program.getDeleted() != null && program.getDeleted()) {
                                    DBMerchantLoyaltyProgram oldProgram = databaseHelper.getProgramById(program.getId(), merchant.getId());
                                    if (oldProgram != null) {
                                        databaseHelper.deleteLoyaltyProgram(oldProgram);
                                    }
                                }
                                else {
                                    databaseHelper.insertOrReplaceMerchantLoyaltyProgram(program);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ArrayList<DBMerchantLoyaltyProgram>> call, Throwable t) {
                        //Crashlytics.log(2, TAG, t.getMessage());
                    }
                });

            } catch (JSONException e) {
                //Crashlytics.logException(e);
            }
        }
        else {
            /*set merchant loyalty program delete flag to true on server*/
            for (final DBMerchantLoyaltyProgram program: programsMarkedForDeletion) {
                mApiClient.getLoystarApi().setMerchantLoyaltyProgramDeleteFlagToTrue(String.valueOf(program.getId())).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            databaseHelper.deleteLoyaltyProgram(program);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        //Crashlytics.log(2, TAG, t.getMessage());
                    }
                });
            }
        }
         /*===================== sync loyalty programs end =============================================>*/



         /*================================ sync customers start end ===================================================>*/
            /* check server for latest customers. For each customer that is returned if customer is
               found locally we update customer details or delete if delete flag is set on the server else we insert new customer*/
        try {
            ArrayList<DBCustomer> customers = databaseHelper.listMerchantCustomers(merchant.getId());
            JSONObject jsonObjectData = new JSONObject();
            if (customers.size() > 0) {
                Collections.sort(customers, new CustomersServerUpdatedTimeComparator());
                jsonObjectData.put("time_stamp", ISO8601DateFormat.format(customers.get(customers.size() - 1).getUpdated_at()));
            }
            else {
                jsonObjectData.put("time_stamp", 0);
            }
            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObjectData);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

            mApiClient.getLoystarApi().getLatestMerchantCustomers(requestBody).enqueue(new Callback<ArrayList<DBCustomer>>() {
                @Override
                public void onResponse(Call<ArrayList<DBCustomer>> call, retrofit2.Response<ArrayList<DBCustomer>> response) {
                    if (response.isSuccessful()) {
                        ArrayList<DBCustomer> dbCustomers = response.body();
                        for (DBCustomer customer: dbCustomers) {
                            DBCustomer oldCustomer = databaseHelper.getCustomerById(customer.getId());
                            if (oldCustomer == null) {
                                databaseHelper.insertCustomer(customer);
                            }
                            else {
                                if (customer.getDeleted() != null && customer.getDeleted()) {
                                    databaseHelper.deleteCustomer(oldCustomer);
                                }
                                else {
                                    databaseHelper.updateCustomer(customer);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<ArrayList<DBCustomer>> call, Throwable t) {
                    //Crashlytics.log(2, TAG, t.getMessage());
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            //Crashlytics.logException(e);
        }

        /*Delete customers marked for deletion on the server*/
        ArrayList<DBCustomer> customersMarkedForDeletion = databaseHelper.getCustomersMarkedForDeletion(merchant.getId());
        for (final DBCustomer customer: customersMarkedForDeletion) {
            mApiClient.getLoystarApi().setCustomerDeleteFlagToTrue(customer.getId().toString()).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        databaseHelper.deleteCustomer(customer);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    //Crashlytics.log(2, TAG, t.getMessage());
                }
            });
        }


         /*========================================= sync customers end =============================================>*/


        /*===================Poll for merchant current subscription START =======================*/
        mApiClient.getLoystarApi().getMerchantSubscription().enqueue(new Callback<DBSubscription>() {
            @Override
            public void onResponse(Call<DBSubscription> call, Response<DBSubscription> response) {
                if (response.isSuccessful()) {
                    DBSubscription subscription = response.body();
                    merchant.setSubscription_expires_on(subscription.getExpires_on());
                    merchant.setSubscription_plan(subscription.getPlan_name());
                    databaseHelper.updateMerchant(merchant);
                }
            }

            @Override
            public void onFailure(Call<DBSubscription> call, Throwable t) {
                //Crashlytics.log(2, TAG, t.getMessage());
            }
        });

        /*================= Poll for merchant current subscription END ===============================*/

        /*========================== Poll for  birthdayOffer START ====================================*/
        mApiClient.getLoystarApi().getMerchantBirthdayOffer().enqueue(new Callback<DBBirthdayOffer>() {
            @Override
            public void onResponse(Call<DBBirthdayOffer> call, Response<DBBirthdayOffer> response) {
                if (response.isSuccessful()) {
                    DBBirthdayOffer birthdayOffer = response.body();
                    databaseHelper.insertOrReplaceBirthdayOffer(birthdayOffer);
                }
                else if (response.code() == 404) {
                    databaseHelper.deleteBirthdayOffer(response.body());
                }
            }

            @Override
            public void onFailure(Call<DBBirthdayOffer> call, Throwable t) {
                //Crashlytics.log(2, TAG, t.getMessage());
            }
        });

        /*========================== Poll for  birthdayOffer END ====================================*/

        /*=============================== Poll for birthdayOfferPresetSMS START ================================*/
        mApiClient.getLoystarApi().getMerchantBirthdayPresetSms().enqueue(new Callback<DBBirthdayOfferPresetSMS>() {
            @Override
            public void onResponse(Call<DBBirthdayOfferPresetSMS> call, Response<DBBirthdayOfferPresetSMS> response) {
                if (response.isSuccessful()) {
                    databaseHelper.insertOrReplaceBirthdayOfferPresetSMS(response.body());
                }
                else if (response.code() == 404) {
                    databaseHelper.deleteBirthdayOfferPresetSMS(response.body());
                }
            }

            @Override
            public void onFailure(Call<DBBirthdayOfferPresetSMS> call, Throwable t) {
                //Crashlytics.log(2, TAG, t.getMessage());
            }
        });

        /*=============================== Poll for birthdayOfferPresetSMS END ================================*/

        /*============================ Update Merchant Details on Server START ========================================*/
        if (merchant.getUpdate_required() != null && merchant.getUpdate_required()) {
            mApiClient.getLoystarApi().updateMerchant(
                    TextUtilsHelper.trimQuotes(merchant.getFirst_name()),
                    TextUtilsHelper.trimQuotes(merchant.getLast_name()),
                    TextUtilsHelper.trimQuotes(merchant.getEmail()),
                    TextUtilsHelper.trimQuotes(merchant.getBusiness_name()),
                    TextUtilsHelper.trimQuotes(merchant.getContact_number()),
                    TextUtilsHelper.trimQuotes(merchant.getBusiness_type()),
                    TextUtilsHelper.trimQuotes(merchant.getCurrency()),
                    merchant.getTurn_on_point_of_sale()).enqueue(new Callback<MerchantUpdateSuccessResponse>() {
                @Override
                public void onResponse(Call<MerchantUpdateSuccessResponse> call, Response<MerchantUpdateSuccessResponse> response) {
                    if (response.isSuccessful()) {
                        merchant.setUpdate_required(false);
                        databaseHelper.updateMerchant(merchant);
                    }
                }

                @Override
                public void onFailure(Call<MerchantUpdateSuccessResponse> call, Throwable t) {
                    //Crashlytics.log(2, TAG, t.getMessage());
                }
            });
        }

        /*============================ Update Merchant Details on Server END ========================================*/

        Intent i = new Intent(SYNC_FINISHED);
        getContext().sendBroadcast(i);
    }

    public static boolean isSyncActive(Account account, String authority) {
        List<SyncInfo> currentSync = ContentResolver.getCurrentSyncs();
        return currentSync.size() > 0 && currentSync.get(0).account.equals(account) && currentSync.get(0).authority.equals(authority);
    }

    public static void syncImmediately(Account account) {
        if (!isSyncActive(account, AccountGeneral.AUTHORITY)) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(account, AccountGeneral.AUTHORITY, bundle);
        }
    }


    private static void configurePeriodicSync(Account account, int syncInterval, int flexTime) {
        String authority = AccountGeneral.AUTHORITY;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            /*we can enable inexact timers in our periodic sync*/
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority)
                    .setExtras(new Bundle())
                    .build();
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    public static void onAccountLogin(Context context, Account account) {
        SyncAdapter.configurePeriodicSync(account, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*Without calling setSyncAutomatically, our periodic sync will not be enabled*/
        ContentResolver.setSyncAutomatically(account, AccountGeneral.AUTHORITY, true);

        /*Finally, do a sync to get things started*/
        syncImmediately(account);
    }

    private class ProgramsUpdatedTimeComparator implements Comparator<DBMerchantLoyaltyProgram> {

        @Override
        public int compare(DBMerchantLoyaltyProgram t0, DBMerchantLoyaltyProgram t1) {
            return t0.getUpdated_at().compareTo(t1.getUpdated_at());
        }
    }

    private class CustomersServerUpdatedTimeComparator implements Comparator<DBCustomer> {
        @Override
        public int compare(DBCustomer o1, DBCustomer o2) {
            return o1.getUpdated_at().compareTo(o2.getUpdated_at());
        }
    }

    private class ProductCategoriesUpdatedTimeComparator implements Comparator<DBProductCategory> {
        @Override
        public int compare(DBProductCategory o1, DBProductCategory o2) {
            return o1.getUpdated_at().compareTo(o2.getUpdated_at());
        }
    }

    private class ProductsUpdatedTimeComparator implements Comparator<DBProduct> {
        @Override
        public int compare(DBProduct o1, DBProduct o2) {
            return o1.getUpdated_at().compareTo(o2.getUpdated_at());
        }
    }

    private class TransactionsCreatedAtComparator implements Comparator<DBTransaction> {
        @Override
        public int compare(DBTransaction o1, DBTransaction o2) {
            return o1.getCreated_at().compareTo(o2.getCreated_at());
        }
    }
}
