package co.loystar.loystarbusiness.api;

import android.content.Context;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.utils.SessionManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by laudbruce-tagoe on 4/8/17.
 */

public class ApiClient {
    private static final String BASE_URL = BuildConfig.HOST;
    private static final String URL_PREFIX = BuildConfig.URL_PREFIX;
    private LoystarApi mLoystarApi;
    private Retrofit retrofit;
    private SessionManager sessionManager;

    public ApiClient(Context context) {
        sessionManager = new SessionManager(context);
    }

    private Retrofit getRetrofit() {
        if (retrofit == null) {

            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();

                        Request.Builder requestBuilder = original.newBuilder()
                                .header("ACCESS-TOKEN", sessionManager.getAccessToken())
                                .header("CLIENT", sessionManager.getClientKey())
                                .header("UID", sessionManager.getMerchantEmail());

                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    }
                })
                .build();

            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL + URL_PREFIX)
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(JsonUtils.objectMapper))
                .build();
        }
        return retrofit;
    }

    public LoystarApi getLoystarApi() {
        if (mLoystarApi == null) {
            mLoystarApi = getRetrofit().create(LoystarApi.class);
        }
        return mLoystarApi;
    }
}
