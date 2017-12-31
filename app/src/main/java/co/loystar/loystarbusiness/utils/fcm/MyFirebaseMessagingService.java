package co.loystar.loystarbusiness.utils.fcm;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.MerchantBackOfficeActivity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.NotificationUtils;

/**
 * Created by ordgen on 12/18/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();
    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.e(TAG, "Message data payload: " + remoteMessage.getData());
            try {
                JSONObject json = new JSONObject(remoteMessage.getData().toString());
                /* Check if data needs to be processed by long running job */
                if (json.has("payload")) {
                    Bundle bundle = new Bundle();
                    bundle.putString("payload", json.getJSONObject("payload").toString());
                    bundle.putString("notification", json.getJSONObject("notification").toString());
                    scheduleJob(bundle);
                } else {
                    // Handle message within 10 seconds
                    handleNow();
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
        pushNotification.putExtra("message", message);
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

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }
}
