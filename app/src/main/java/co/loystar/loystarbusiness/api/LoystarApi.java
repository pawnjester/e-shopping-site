package co.loystar.loystarbusiness.api;

import java.util.ArrayList;

import co.loystar.loystarbusiness.api.pojos.CheckPhoneAndEmailResponse;
import co.loystar.loystarbusiness.api.pojos.ConfirmMMPaymentResponse;
import co.loystar.loystarbusiness.api.pojos.GetPricingPlanDataResponse;
import co.loystar.loystarbusiness.api.pojos.MerchantSignInSuccessResponse;
import co.loystar.loystarbusiness.api.pojos.MerchantSmsBalanceResponse;
import co.loystar.loystarbusiness.api.pojos.MerchantUpdateSuccessResponse;
import co.loystar.loystarbusiness.api.pojos.PaySubscriptionWithMobileMoneyResponse;
import co.loystar.loystarbusiness.api.pojos.SendPasswordResetEmailResponse;
import co.loystar.loystarbusiness.models.db.DBBirthdayOffer;
import co.loystar.loystarbusiness.models.db.DBBirthdayOfferPresetSMS;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.models.db.DBProductCategory;
import co.loystar.loystarbusiness.models.db.DBSubscription;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by laudbruce-tagoe on 4/8/17.
 */

public interface LoystarApi {
    @FormUrlEncoded
    @POST("auth")
    Call<MerchantSignInSuccessResponse> signUpMerchant(@Field("first_name") String firstName,
                                    @Field("email") String email,
                                    @Field("business_name") String businessName,
                                    @Field("contact_number") String contactNumber,
                                    @Field("business_type") String businessType,
                                    @Field("currency") String currency,
                                    @Field("password") String password);

    @FormUrlEncoded
    @PUT("auth")
    Call<MerchantUpdateSuccessResponse> updateMerchant(@Field("first_name") String firstName,
                                                       @Field("last_name") String lastName,
                                                       @Field("email") String email,
                                                       @Field("business_name") String businessName,
                                                       @Field("contact_number") String contactNumber,
                                                       @Field("business_type") String businessType,
                                                       @Field("currency") String currency,
                                                       @Field("turn_on_point_of_sale") Boolean turnOnPointOfSale);

    @GET("auth/validate_token")
    Call<MerchantUpdateSuccessResponse> validateMerchantToken();

    @FormUrlEncoded
    @POST("auth/sign_in")
    Call<MerchantSignInSuccessResponse> signInMerchant(
            @Field("email") String email,
            @Field("password") String password);

    @GET("sign_in_digits_user")
    Call<DBMerchant> signInWithDigits(@Header("X-Auth-Service-Provider") String provider, @Header("X-Verify-Credentials-Authorization") String credentials);

    @POST("check_phone_and_email")
    Call<CheckPhoneAndEmailResponse> checkPhoneAndEmailAvailability(@Body RequestBody requestBody);

    @POST("get_latest_merchant_product_categories")
    Call<ArrayList<DBProductCategory>> getLatestMerchantProductCategories(@Body RequestBody requestBody);

    @POST("get_latest_merchant_products")
    Call<ArrayList<DBProduct>> getLatestMerchantProducts(@Body RequestBody requestBody);

    @POST("get_latest_merchant_customers")
    Call<ArrayList<DBCustomer>> getLatestMerchantCustomers(@Body RequestBody requestBody);

    @POST("get_latest_transactions")
    Call<ArrayList<DBTransaction>> getLatestTransactions(@Body RequestBody requestBody);

    @GET("get_merchant_current_subscription")
    Call<DBSubscription> getMerchantSubscription();

    @POST("transactions/record_sales/{customer_id}")
    Call<DBTransaction> recordSales(@Body RequestBody requestBody, @Path("customer_id") String customer_id);

    @POST("get_merchant_loyalty_programs")
    Call<ArrayList<DBMerchantLoyaltyProgram>> getMerchantLoyaltyPrograms(@Body RequestBody requestBody);

    @GET("get_merchant_birthday_preset_sms")
    Call<DBBirthdayOfferPresetSMS> getMerchantBirthdayPresetSms();

    @GET("get_merchant_birthday_offer")
    Call<DBBirthdayOffer> getMerchantBirthdayOffer();

    @GET("get_credentials_by_phone_and_id/{contact_number}/{merchant_id}")
    Call<ResponseBody> getCredentialsByPhoneAndId(@Path("contact_number") String contact_number,
                                                  @Path("merchant_id") String merchant_id);

    @GET("get_merchant_sms_balance")
    Call<MerchantSmsBalanceResponse> getSmsBalance();

    @GET("subscriptions/confirm_mobile_money_payment")
    Call<ConfirmMMPaymentResponse> confirmMobileMoneyPayment();

    @POST("get_pricing_plan_data")
    Call<GetPricingPlanDataResponse> getPricingPlanPrice(@Body RequestBody requestBody);

    @POST("subscriptions/pay_with_mobile_money")
    Call<PaySubscriptionWithMobileMoneyResponse> paySubscriptionWithMobileMoney(@Body RequestBody requestBody);

    @POST("products/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setProductDeleteFlagToTrue(@Path("id") String id);

    @POST("merchant_product_categories/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setMerchantProductCategoryDeleteFlagToTrue(@Path("id") String id);

    @POST("merchant_loyalty_programs/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setMerchantLoyaltyProgramDeleteFlagToTrue(@Path("id") String id);

    @POST("merchant_loyalty_programs")
    Call<DBMerchantLoyaltyProgram> createMerchantLoyaltyProgram(@Body RequestBody requestBody);

    @PUT("merchant_loyalty_programs/{id}")
    Call<DBMerchantLoyaltyProgram> updateMerchantLoyaltyProgram(@Path("id") String id, @Body RequestBody requestBody);

    @POST("add_user_direct")
    Call<DBCustomer> addUserDirect(@Body RequestBody requestBody);

    @POST("add_product_category")
    Call<DBProductCategory> addProductCategory(@Body RequestBody requestBody);

    @PATCH("products/{id}")
    Call<DBProduct> updateProduct(@Body RequestBody requestBody, @Path("id") String id);

    @DELETE("birthday_offers/{id}")
    Call<ResponseBody> deleteBirthdayOffer(@Path("id") String id);

    @PATCH("birthday_offers/{id}")
    Call<DBBirthdayOffer> updateBirthdayOffer(@Path("id") String id, @Body RequestBody requestBody);

    @POST("birthday_offers")
    Call<DBBirthdayOffer> createBirthdayOffer(@Body RequestBody requestBody);

    @POST("birthday_offer_preset_sms")
    Call<DBBirthdayOfferPresetSMS> createBirthdayOfferPresetSMS(@Body RequestBody requestBody);

    @PATCH("birthday_offer_preset_sms/{id}")
    Call<DBBirthdayOfferPresetSMS> updateBirthdayOfferPresetSMS(@Path("id") String id, @Body RequestBody requestBody);

    @POST("short_message_service_campaigns")
    Call<ResponseBody> sendSmsBlast(@Body RequestBody requestBody);

    @POST("short_message_services")
    Call<ResponseBody> sendSms(@Body RequestBody requestBody);

    @POST("auth/password")
    Call<SendPasswordResetEmailResponse> sendPasswordResetEmail(@Body RequestBody requestBody);

    @POST("transactions/redeem_reward/{redemption_code}/{customer_id}/{loyalty_program_id}")
    Call<DBTransaction> redeemReward(
            @Path("redemption_code") String redemption_code,
            @Path("customer_id") String customer_id,
            @Path("loyalty_program_id") String loyalty_program_id);

    @POST("customers/set_delete_flag_to_true/{id}")
    Call<ResponseBody> setCustomerDeleteFlagToTrue(@Path("id") String id);

    @POST("customers/update_customer/{id}")
    Call<DBCustomer> updateCustomer(@Path("id") String id, @Body RequestBody requestBody);
}
