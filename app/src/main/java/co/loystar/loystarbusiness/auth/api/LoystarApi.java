package co.loystar.loystarbusiness.auth.api;

import java.util.ArrayList;

import co.loystar.loystarbusiness.models.databinders.BirthdayOffer;
import co.loystar.loystarbusiness.models.databinders.BirthdayOfferPresetSms;
import co.loystar.loystarbusiness.models.databinders.Customer;
import co.loystar.loystarbusiness.models.databinders.EmailAvailability;
import co.loystar.loystarbusiness.models.databinders.LoyaltyProgram;
import co.loystar.loystarbusiness.models.databinders.MerchantWrapper;
import co.loystar.loystarbusiness.models.databinders.PasswordReset;
import co.loystar.loystarbusiness.models.databinders.PaySubscription;
import co.loystar.loystarbusiness.models.databinders.PhoneNumberAvailability;
import co.loystar.loystarbusiness.models.databinders.PricingPlan;
import co.loystar.loystarbusiness.models.databinders.Product;
import co.loystar.loystarbusiness.models.databinders.ProductCategory;
import co.loystar.loystarbusiness.models.databinders.SalesOrder;
import co.loystar.loystarbusiness.models.databinders.SmsBalance;
import co.loystar.loystarbusiness.models.databinders.Subscription;
import co.loystar.loystarbusiness.models.databinders.Transaction;
import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Created by ordgen on 11/1/17.
 */

public interface LoystarApi {
    @FormUrlEncoded
    @POST("auth")
    Call<MerchantWrapper> signUpMerchant(
            @Field("first_name") String firstName,
            @Field("email") String email,
            @Field("business_name") String businessName,
            @Field("contact_number") String contactNumber,
            @Field("business_type") String businessType,
            @Field("currency") String currency,
            @Field("password") String password);

    @FormUrlEncoded
    @POST("auth/sign_in")
    Observable<Response<MerchantWrapper>> signInMerchant(
            @Field("email") String email,
            @Field("password") String password);

    @FormUrlEncoded
    @PUT("auth")
    Call<MerchantWrapper> updateMerchant(@Field("first_name") String firstName,
                                                       @Field("last_name") String lastName,
                                                       @Field("email") String email,
                                                       @Field("business_name") String businessName,
                                                       @Field("contact_number") String contactNumber,
                                                       @Field("business_type") String businessType,
                                                       @Field("currency") String currency,
                                                       @Field("turn_on_point_of_sale") Boolean turnOnPointOfSale);

    @GET("get_merchant_current_subscription")
    Call<Subscription> getMerchantSubscription();

    @GET("get_merchant_birthday_preset_sms")
    Call<BirthdayOfferPresetSms> getMerchantBirthdayPresetSms();

    @GET("merchants/check_phone_availability/{contact_number}")
    Observable<Response<PhoneNumberAvailability>> checkMerchantPhoneNumberAvailability(@Path("contact_number") String contact_number);

    @GET("merchants/check_email_availability/{email}")
    Call<EmailAvailability> checkMerchantEmailAvailability(@Path("email") String email);

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
    Call<Transaction> recordSales(@Body RequestBody requestBody, @Path("customer_id") int customer_id);

    @POST("get_merchant_loyalty_programs")
    Call<ArrayList<LoyaltyProgram>> getMerchantLoyaltyPrograms(@Body RequestBody requestBody);

    @GET("get_merchant_sms_balance")
    Call<SmsBalance> getSmsBalance();

    @POST("get_pricing_plan_data")
    Call<PricingPlan> getPricingPlanPrice(@Body RequestBody requestBody);

    @POST("subscriptions/pay_with_mobile_money")
    Call<PaySubscription> paySubscriptionWithMobileMoney(@Body RequestBody requestBody);

    @POST("products/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setProductDeleteFlagToTrue(@Path("id") int id);

    @POST("merchant_product_categories/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setMerchantProductCategoryDeleteFlagToTrue(@Path("id") int id);

    @POST("merchant_loyalty_programs/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setMerchantLoyaltyProgramDeleteFlagToTrue(@Path("id") int id);

    @POST("merchant_loyalty_programs")
    Call<LoyaltyProgram> createMerchantLoyaltyProgram(@Body RequestBody requestBody);

    @PUT("merchant_loyalty_programs/{id}")
    Call<LoyaltyProgram> updateMerchantLoyaltyProgram(@Path("id") String id, @Body RequestBody requestBody);

    @POST("add_user_direct")
    Call<Customer> addUserDirect(@Body RequestBody requestBody);

    @POST("add_product_category")
    Call<ProductCategory> addProductCategory(@Body RequestBody requestBody);

    @PATCH("products/{id}")
    Call<Product> updateProduct(@Body RequestBody requestBody, @Path("id") int id);

    @PATCH("merchant_product_categories/{id}")
    Call<ProductCategory> updateProductCategory(@Body RequestBody requestBody, @Path("id") int id);

    @DELETE("birthday_offers/{id}")
    Call<ResponseBody> deleteBirthdayOffer(@Path("id") int id);

    @PATCH("birthday_offers/{id}")
    Call<BirthdayOffer> updateBirthdayOffer(@Path("id") int id, @Body RequestBody requestBody);

    @POST("birthday_offers")
    Call<BirthdayOffer> createBirthdayOffer(@Body RequestBody requestBody);

    @POST("birthday_offer_preset_sms")
    Call<BirthdayOfferPresetSms> createBirthdayOfferPresetSMS(@Body RequestBody requestBody);

    @PATCH("birthday_offer_preset_sms/{id}")
    Call<BirthdayOfferPresetSms> updateBirthdayOfferPresetSMS(@Path("id") int id, @Body RequestBody requestBody);

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
    Call<ResponseBody> setCustomerDeleteFlagToTrue(@Path("id") int id);

    @POST("customers/update_customer/{id}")
    Call<Customer> updateCustomer(@Path("id") int id, @Body RequestBody requestBody);

    @POST("auth/password")
    Call<PasswordReset> sendPasswordResetEmail(@Body RequestBody requestBody);

    @POST("merchants/reset_password")
    Call<ResponseBody> resetMerchantPassword(@Body RequestBody requestBody);

    @Multipart
    @PATCH("products/{id}")
    Call<Product> updateProduct(
            @Path("id") int id,
            @Part("data[name]") RequestBody name,
            @Part("data[price]") RequestBody price,
            @Part("data[merchant_product_category_id]") RequestBody merchant_product_category_id,
            @Part("data[merchant_loyalty_program_id]") RequestBody merchant_loyalty_program_id,
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("add_product")
    Call<Product> addProduct(
            @Part("data[name]") RequestBody name,
            @Part("data[price]") RequestBody price,
            @Part("data[merchant_product_category_id]") RequestBody merchant_product_category_id,
            @Part("data[merchant_loyalty_program_id]") RequestBody merchant_loyalty_program_id,
            @Part MultipartBody.Part file
    );

    @POST("merchants/set_firebase_registration_token")
    Call<ResponseBody> setFirebaseRegistrationToken(@Body RequestBody requestBody);

    @GET("transactions/send_transaction_sms/{merchant_id}/{customer_id}/{loyalty_program_id}")
    Call<ResponseBody> sendTransactionSms(
        @Path("merchant_id") int merchant_id,
        @Path("customer_id") int customer_id,
        @Path("loyalty_program_id") int loyalty_program_id);

    @POST("merchants/latest_orders")
    Call<ArrayList<SalesOrder>> getMerchantOrders(@Body RequestBody requestBody);

    @PATCH("merchants/orders/{id}")
    Call<ResponseBody> updateMerchantOrder(@Path("id") int id, @Body RequestBody requestBody);
}
