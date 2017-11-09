package co.loystar.loystarbusiness.auth.api;

import co.loystar.loystarbusiness.models.databinders.BirthdayOffer;
import co.loystar.loystarbusiness.models.databinders.BirthdayOfferPresetSms;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.databinders.Subscription;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

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
}
