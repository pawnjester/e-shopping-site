package co.loystar.loystarbusiness.auth.api;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.auth.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by ordgen on 11/1/17.
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

    private Retrofit getRetrofit(boolean hasRootValue) {
        if (retrofit == null) {

            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request original = chain.request();

                        Request.Builder requestBuilder = original.newBuilder()
                                .header("ACCESS-TOKEN", sessionManager.getAccessToken())
                                .header("CLIENT", sessionManager.getClientKey())
                                .header("UID", sessionManager.getEmail());

                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL + URL_PREFIX)
                    .client(okHttpClient)
                    .addConverterFactory(JacksonConverterFactory.create(ApiUtils.getObjectMapper(hasRootValue)))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public LoystarApi getLoystarApi(boolean hasRootValue) {
        if (mLoystarApi == null) {
            mLoystarApi = getRetrofit(hasRootValue).create(LoystarApi.class);
        }
        return mLoystarApi;
    }
}
