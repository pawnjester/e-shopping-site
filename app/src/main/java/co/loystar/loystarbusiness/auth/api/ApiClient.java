package co.loystar.loystarbusiness.auth.api;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import timber.log.Timber;

/**
 * Created by ordgen on 11/1/17.
 */

public class ApiClient {
    private static final String TAG = ApiClient.class.getSimpleName();
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
        if (retrofit == null) {

            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request originalRequest = chain.request();
                        if (mAccount == null) {
                            return chain.proceed(originalRequest);
                        }

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
                            String oldToken = mAccountManager.peekAuthToken(mAccount, AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS);
                            if (oldToken != null) {
                                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, oldToken);
                                try {
                                    String newToken = mAccountManager.blockingGetAuthToken(mAccount, AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS, true);
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
