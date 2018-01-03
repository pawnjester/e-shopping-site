package co.loystar.loystarbusiness.utils.fcm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.NotificationUtils;

/**
 * Created by ordgen on 12/18/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();
    private static final String KEY_SALES_ORDER = "sales_order";
    private static final String KEY_SYNC_REQUEST = "sync_request";
    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            try {
                JSONObject data = new JSONObject(remoteMessage.getData().toString());
                if (data.has(KEY_SALES_ORDER)) {
                    /* Sales Order needs to be processed by long running job */
                    JSONObject salesOrder = data.getJSONObject("sales_order");
                    Bundle bundle = new Bundle();
                    bundle.putString("order", salesOrder.getJSONObject("order").toString());
                    bundle.putString("notification", salesOrder.getJSONObject("notification").toString());
                    bundle.putString("customer", salesOrder.getJSONObject("customer").toString());
                    scheduleJob(bundle);
                } else if (data.getBoolean(KEY_SYNC_REQUEST)) {
                    SessionManager sessionManager = new SessionManager(getApplicationContext());
                    SyncAdapter.performSync(getApplicationContext(), sessionManager.getEmail());
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            handleNotification(remoteMessage.getNotification().getBody());
        }
    }

    private void handleNotification(String message) {
        // app is in foreground, broadcast the push message
        Intent pushNotification = new Intent(Constants.PUSH_NOTIFICATION);
        pushNotification.putExtra(Constants.NOTIFICATION_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

        // play notification sound
        NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
        notificationUtils.playNotificationSound();
    }

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    private void scheduleJob(Bundle extras) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job myJob = dispatcher.newJobBuilder()
            .setService(MyJobService.class)
            .setTag("my-job-tag")
            .setExtras(extras)
            .build();
        dispatcher.schedule(myJob);
    }
}
