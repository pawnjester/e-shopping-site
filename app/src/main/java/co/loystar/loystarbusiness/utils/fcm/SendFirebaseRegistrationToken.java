package co.loystar.loystarbusiness.utils.fcm;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.utils.Constants;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 12/19/17.
 */

public class SendFirebaseRegistrationToken {
    private Context mContext;
    private String mToken;

    public SendFirebaseRegistrationToken(Context context) {
        mContext = context;
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), 0);
        mToken = sharedPreferences.getString(Constants.FIREBASE_REGISTRATION_TOKEN, "");
    }

    /**
     * Persist token to third-party servers.
     *
     */
    public void sendRegistrationToServer() {
        if (!TextUtils.isEmpty(mToken)) {
            ApiClient apiClient = new ApiClient(mContext);
            JSONObject jsonObjectData = new JSONObject();
            try {
                jsonObjectData.put("token", mToken);
                JSONObject requestData = new JSONObject();
                requestData.put("data", jsonObjectData);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                apiClient.getLoystarApi(false).setFirebaseRegistrationToken(requestBody).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.code() == 401) {
                            SessionManager sessionManager = new SessionManager(mContext);
                            AccountManager accountManager = AccountManager.get(mContext);
                            accountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, sessionManager.getAccessToken());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
