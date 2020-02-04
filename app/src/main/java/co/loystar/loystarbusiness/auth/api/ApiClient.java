package co.loystar.loystarbusiness.auth.api;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
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
    private SessionManager mSessionManager;
    private AccountManager mAccountManager;
    private Account mAccount;


    public ApiClient(Context context) {
        mSessionManager = new SessionManager(context);
        mAccountManager = AccountManager.get(context);
        mAccount = AccountGeneral.getUserAccount(context, mSessionManager.getEmail());
    }

    private Retrofit getRetrofit(boolean hasRootValue) {
        Log.e("account333", mSessionManager.getEmail()
                +" " + mSessionManager.getClientKey() + " "
                + mSessionManager.getAccessToken());
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request originalRequest = chain.request();
                        if (mAccount == null) {
                            return chain.proceed(originalRequest);
                        }
                        Log.e("accountwww", mSessionManager.getEmail()
                                +" " + mSessionManager.getClientKey() + " "
                                + mSessionManager.getAccessToken());

                        Request authorisedRequest = originalRequest.newBuilder()
                            .header("ACCESS-TOKEN", mSessionManager.getAccessToken())
                            .header("CLIENT", mSessionManager.getClientKey())
                            .header("UID", mSessionManager.getEmail())
                            .build();
                        return chain.proceed(authorisedRequest);
                    })
                    .authenticator((route, response) -> {
                        if (responseCount(response) >= 3) {
                            return null;
                        }
                        if (mAccount != null) {
                            Log.e("here", "dundee");
                            String oldToken = mAccountManager.peekAuthToken(mAccount,
                                    AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS);
                            if (oldToken != null) {
                                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, oldToken);
                                try {
                                    String newToken = mAccountManager.blockingGetAuthToken(mAccount,
                                            AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS, true);
                                    if (newToken != null) {
                                        return response.request().newBuilder()
                                            .header("ACCESS-TOKEN", newToken)
                                            .header("CLIENT", mSessionManager.getClientKey())
                                            .header("UID", mSessionManager.getEmail())
                                            .build();
                                    }
                                } catch (OperationCanceledException e) {
                                    e.printStackTrace();
                                } catch (AuthenticatorException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        return null;
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

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
