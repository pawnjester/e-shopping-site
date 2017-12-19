package co.loystar.loystarbusiness.utils.fcm;

import android.accounts.AccountManager;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

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
 * Created by ordgen on 12/18/17.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = MyFirebaseInstanceIDService.class.getSimpleName();

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        ApiClient apiClient = new ApiClient(getApplicationContext());
        JSONObject jsonObjectData = new JSONObject();
        try {
            jsonObjectData.put("token", token);
            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObjectData);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
            apiClient.getLoystarApi(false).setFirebaseRegistrationToken(requestBody).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        SharedPreferences pref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), 0);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString(Constants.FIREBASE_REGISTRATION_TOKEN, token);
                        editor.apply();
                    } else if (response.code() == 401) {
                        SessionManager sessionManager = new SessionManager(getApplicationContext());
                        AccountManager accountManager = AccountManager.get(getApplicationContext());
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
