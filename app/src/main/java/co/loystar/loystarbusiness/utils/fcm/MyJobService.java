package co.loystar.loystarbusiness.utils.fcm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;

import co.loystar.loystarbusiness.activities.MerchantBackOfficeActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiUtils;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.OrderItem;
import co.loystar.loystarbusiness.models.databinders.SalesOrder;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.OrderItemEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesOrderEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.NotificationUtils;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;
import okhttp3.MediaType;
import okhttp3.ResponseBody;

/**
 * Created by ordgen on 12/18/17.
 */

public class MyJobService extends JobService {
    private static final String TAG = MyJobService.class.getSimpleName();

    @Override
    public boolean onStartJob(JobParameters job) {
        Bundle extras = job.getExtras();
        if (extras != null) {
            try {
                JSONObject notificationObject = new JSONObject(extras.getString("notification", ""));
                ReactiveEntityStore<Persistable> mDataStore = DatabaseManager.getDataStore(getApplicationContext());
                SessionManager sessionManager  = new SessionManager(getApplicationContext());

                ResponseBody valueToConvert  = ResponseBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    extras.getString("payload", "")
                );
                ObjectMapper objectMapper = ApiUtils.getObjectMapper(false);
                JavaType javaType = objectMapper.getTypeFactory().constructType(SalesOrder.class);
                ObjectReader reader = objectMapper.readerFor(javaType);
                SalesOrder salesOrder = reader.readValue(valueToConvert.charStream());

                MerchantEntity merchantEntity = mDataStore.findByKey(MerchantEntity.class, sessionManager.getMerchantId()).blockingGet();
                CustomerEntity customerEntity = mDataStore.select(CustomerEntity.class)
                    .where(CustomerEntity.USER_ID.eq(salesOrder.getUser_id()))
                    .get()
                    .firstOrNull();
                if (customerEntity != null && merchantEntity != null) {
                    SalesOrderEntity salesOrderEntity = new SalesOrderEntity();
                    salesOrderEntity.setMerchant(merchantEntity);
                    salesOrderEntity.setId(salesOrder.getId());
                    salesOrderEntity.setStatus(salesOrder.getStatus());
                    salesOrderEntity.setUpdateRequired(false);
                    salesOrderEntity.setTotal(salesOrder.getTotal());
                    salesOrderEntity.setCreatedAt(new Timestamp(salesOrder.getCreated_at().getMillis()));
                    salesOrderEntity.setUpdatedAt(new Timestamp(salesOrder.getUpdated_at().getMillis()));
                    salesOrderEntity.setCustomer(customerEntity);

                    mDataStore.upsert(salesOrderEntity).subscribe(orderEntity -> {
                        for (OrderItem orderItem: salesOrder.getOrder_items()) {
                            OrderItemEntity orderItemEntity = new OrderItemEntity();
                            orderItemEntity.setCreatedAt(new Timestamp(orderItem.getCreated_at().getMillis()));
                            orderItemEntity.setUpdatedAt(new Timestamp(orderItem.getUpdated_at().getMillis()));
                            orderItemEntity.setId(orderItem.getId());
                            orderItemEntity.setQuantity(orderItem.getQuantity());
                            orderItemEntity.setUnitPrice(orderItem.getUnit_price());
                            orderItemEntity.setTotalPrice(orderItem.getTotal_price());
                            orderItemEntity.setSalesOrder(orderEntity);
                            ProductEntity productEntity = mDataStore.findByKey(ProductEntity.class, orderItem.getProduct_id()).blockingGet();
                            if (productEntity != null) {
                                orderItemEntity.setProduct(productEntity);
                                mDataStore.insert(orderItemEntity).subscribe(/*no-op*/);
                            }
                        }
                    });
                }

                String title = notificationObject.getString("title");
                String message = notificationObject.getString("message");
                String imageUrl = notificationObject.getString("image");
                String timestamp = notificationObject.getString("timestamp");

                if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
                    // app is in foreground, broadcast the push message
                    Intent pushNotification = new Intent(Constants.PUSH_NOTIFICATION);
                    pushNotification.putExtra(Constants.NOTIFICATION_MESSAGE, message);
                    pushNotification.putExtra(Constants.NOTIFICATION_TYPE, Constants.ORDER_RECEIVED_NOTIFICATION);
                    pushNotification.putExtra(Constants.NOTIFICATION_ORDER_ID, salesOrder.getId());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

                    Intent resultIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                    resultIntent.putExtra(Constants.NOTIFICATION_MESSAGE, message);
                    resultIntent.putExtra(Constants.NOTIFICATION_TYPE, Constants.ORDER_RECEIVED_NOTIFICATION);
                    resultIntent.putExtra(Constants.NOTIFICATION_ORDER_ID, salesOrder.getId());
                    showNotificationMessage(getApplicationContext(), title, message, timestamp, resultIntent);
                } else {
                    // app is in background, show the notification in notification tray
                    Intent resultIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                    resultIntent.putExtra(Constants.NOTIFICATION_MESSAGE, message);
                    resultIntent.putExtra(Constants.NOTIFICATION_TYPE, Constants.ORDER_RECEIVED_NOTIFICATION);
                    resultIntent.putExtra(Constants.NOTIFICATION_ORDER_ID, salesOrder.getId());

                    // check for image attachment
                    if (TextUtils.isEmpty(imageUrl)) {
                        showNotificationMessage(getApplicationContext(), title, message, timestamp, resultIntent);
                    } else {
                        // image is present, show notification with image
                        showNotificationMessageWithBigImage(getApplicationContext(), title, message, timestamp, resultIntent, imageUrl);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    /**
     * Showing notification with text only
     */
    private void showNotificationMessage(Context context, String title, String message, String timeStamp, Intent intent) {
        NotificationUtils notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent);
    }

    /**
     * Showing notification with text and image
     */
    private void showNotificationMessageWithBigImage(Context context, String title, String message, String timeStamp, Intent intent, String imageUrl) {
        NotificationUtils notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent, imageUrl);
    }
}
