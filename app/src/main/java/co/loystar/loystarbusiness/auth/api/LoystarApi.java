package co.loystar.loystarbusiness.auth.api;

import java.util.ArrayList;

import co.loystar.loystarbusiness.models.databinders.BirthdayOffer;
import co.loystar.loystarbusiness.models.databinders.BirthdayOfferPresetSms;
import co.loystar.loystarbusiness.models.databinders.Customer;
import co.loystar.loystarbusiness.models.databinders.LoyaltyProgram;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.databinders.Product;
import co.loystar.loystarbusiness.models.databinders.ProductCategory;
import co.loystar.loystarbusiness.models.databinders.Subscription;
import co.loystar.loystarbusiness.models.databinders.Transaction;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by ordgen on 11/1/17.
 */

public interface LoystarApi {
    @FormUrlEncoded
    @POST("auth/sign_in")
    Call<Merchant> signInMerchant(
            @Field("email") String email,
            @Field("password") String password);

    @GET("get_merchant_current_subscription")
    Call<Subscription> getMerchantSubscription();

    @GET("get_merchant_birthday_preset_sms")
    Call<BirthdayOfferPresetSms> getMerchantBirthdayPresetSms();

    @GET("get_merchant_birthday_offer")
    Call<BirthdayOffer> getMerchantBirthdayOffer();

    @POST("get_latest_merchant_product_categories")
    Call<ArrayList<ProductCategory>> getLatestMerchantProductCategories(@Body RequestBody requestBody);

    @POST("get_latest_merchant_products")
    Call<ArrayList<Product>> getLatestMerchantProducts(@Body RequestBody requestBody);

    @POST("get_latest_merchant_customers")
    Call<ArrayList<Customer>> getLatestMerchantCustomers(@Body RequestBody requestBody);

    @POST("get_latest_transactions")
    Call<ArrayList<Transaction>> getLatestTransactions(@Body RequestBody requestBody);

    @POST("transactions/record_sales/{customer_id}")
    Call<Transaction> recordSales(@Body RequestBody requestBody, @Path("customer_id") String customer_id);

    @POST("get_merchant_loyalty_programs")
    Call<ArrayList<LoyaltyProgram>> getMerchantLoyaltyPrograms(@Body RequestBody requestBody);

    @GET("get_merchant_sms_balance")
    Call<ResponseBody> getSmsBalance();

    @POST("get_pricing_plan_data")
    Call<ResponseBody> getPricingPlanPrice(@Body RequestBody requestBody);

    @POST("subscriptions/pay_with_mobile_money")
    Call<ResponseBody> paySubscriptionWithMobileMoney(@Body RequestBody requestBody);

    @POST("products/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setProductDeleteFlagToTrue(@Path("id") String id);

    @POST("merchant_product_categories/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setMerchantProductCategoryDeleteFlagToTrue(@Path("id") String id);

    @POST("merchant_loyalty_programs/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setMerchantLoyaltyProgramDeleteFlagToTrue(@Path("id") String id);

    @POST("merchant_loyalty_programs")
    Call<LoyaltyProgram> createMerchantLoyaltyProgram(@Body RequestBody requestBody);

    @PUT("merchant_loyalty_programs/{id}")
    Call<LoyaltyProgram> updateMerchantLoyaltyProgram(@Path("id") String id, @Body RequestBody requestBody);

    @POST("add_user_direct")
    Call<Customer> addUserDirect(@Body RequestBody requestBody);

    @POST("add_product_category")
    Call<ProductCategory> addProductCategory(@Body RequestBody requestBody);

    @PATCH("products/{id}")
    Call<Product> updateProduct(@Body RequestBody requestBody, @Path("id") String id);

    @DELETE("birthday_offers/{id}")
    Call<ResponseBody> deleteBirthdayOffer(@Path("id") String id);

    @PATCH("birthday_offers/{id}")
    Call<BirthdayOffer> updateBirthdayOffer(@Path("id") String id, @Body RequestBody requestBody);

    @POST("birthday_offers")
    Call<BirthdayOffer> createBirthdayOffer(@Body RequestBody requestBody);

    @POST("birthday_offer_preset_sms")
    Call<BirthdayOfferPresetSms> createBirthdayOfferPresetSMS(@Body RequestBody requestBody);

    @PATCH("birthday_offer_preset_sms/{id}")
    Call<BirthdayOfferPresetSms> updateBirthdayOfferPresetSMS(@Path("id") String id, @Body RequestBody requestBody);

    @POST("short_message_service_campaigns")
    Call<ResponseBody> sendSmsBlast(@Body RequestBody requestBody);

    @POST("short_message_services")
    Call<ResponseBody> sendSms(@Body RequestBody requestBody);

    @POST("transactions/redeem_reward/{redemption_code}/{customer_id}/{loyalty_program_id}")
    Call<Transaction> redeemReward(
            @Path("redemption_code") String redemption_code,
            @Path("customer_id") int customer_id,
            @Path("loyalty_program_id") int loyalty_program_id);

    @POST("customers/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setCustomerDeleteFlagToTrue(@Path("id") String id);

    @POST("customers/update_customer/{id}")
    Call<Customer> updateCustomer(@Path("id") int id, @Body RequestBody requestBody);
}
