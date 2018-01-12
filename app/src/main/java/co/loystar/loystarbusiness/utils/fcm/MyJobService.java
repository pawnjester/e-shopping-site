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
import java.util.ArrayList;

import co.loystar.loystarbusiness.activities.MerchantBackOfficeActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiUtils;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Customer;
import co.loystar.loystarbusiness.models.databinders.OrderItem;
import co.loystar.loystarbusiness.models.databinders.SalesOrder;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.OrderItemEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.models.entities.SalesOrderEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.Foreground;
import co.loystar.loystarbusiness.utils.NotificationUtils;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import timber.log.Timber;

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
                MerchantEntity merchantEntity = mDataStore.findByKey(MerchantEntity.class, sessionManager.getMerchantId()).blockingGet();

                if (merchantEntity != null) {
                    ResponseBody customerObjectToConvert  = ResponseBody.create(
                        MediaType.parse("application/json; charset=utf-8"),
                        extras.getString("customer", "")
                    );

                    ObjectMapper objectMapper = ApiUtils.getObjectMapper(false);
                    JavaType customerType = objectMapper.getTypeFactory().constructType(Customer.class);
                    ObjectReader customerReader = objectMapper.readerFor(customerType);
                    Customer customer = customerReader.readValue(customerObjectToConvert.charStream());
                    CustomerEntity existingCustomer = mDataStore.findByKey(CustomerEntity.class, customer.getId()).blockingGet();

                    ResponseBody orderObjectToConvert  = ResponseBody.create(
                        MediaType.parse("application/json; charset=utf-8"),
                        extras.getString("order", "")
                    );

                    JavaType orderType = objectMapper.getTypeFactory().constructType(SalesOrder.class);
                    ObjectReader reader = objectMapper.readerFor(orderType);
                    SalesOrder salesOrder = reader.readValue(orderObjectToConvert.charStream());

                    if (existingCustomer == null) {
                        CustomerEntity newCustomerEntity = new CustomerEntity();
                        newCustomerEntity.setId(customer.getId());
                        if (customer.getEmail() != null && !customer.getEmail().contains("yopmail.com")) {
                            newCustomerEntity.setEmail(customer.getEmail());
                        }
                        newCustomerEntity.setFirstName(customer.getFirst_name());
                        newCustomerEntity.setDeleted(false);
                        newCustomerEntity.setLastName(customer.getLast_name());
                        newCustomerEntity.setSex(customer.getSex());
                        newCustomerEntity.setDateOfBirth(customer.getDate_of_birth());
                        newCustomerEntity.setPhoneNumber(customer.getPhone_number());
                        newCustomerEntity.setUserId(customer.getUser_id());
                        newCustomerEntity.setCreatedAt(new Timestamp(customer.getCreated_at().getMillis()));
                        newCustomerEntity.setUpdatedAt(new Timestamp(customer.getUpdated_at().getMillis()));
                        newCustomerEntity.setOwner(merchantEntity);

                        mDataStore.upsert(newCustomerEntity).subscribe(customerEntity -> {
                            insertNewSalesOrder(salesOrder, customerEntity, mDataStore, merchantEntity, notificationObject);
                        });
                    } else {
                        insertNewSalesOrder(salesOrder, existingCustomer, mDataStore, merchantEntity, notificationObject);
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

    private void insertNewSalesOrder(
        SalesOrder salesOrder,
        CustomerEntity customerEntity,
        ReactiveEntityStore<Persistable> mDataStore,
        MerchantEntity merchantEntity,
        JSONObject notificationObject) {

        SalesOrderEntity salesOrderEntity = new SalesOrderEntity();
        salesOrderEntity.setMerchant(merchantEntity);
        salesOrderEntity.setId(salesOrder.getId());
        salesOrderEntity.setStatus(salesOrder.getStatus());
        salesOrderEntity.setUpdateRequired(false);
        salesOrderEntity.setTotal(salesOrder.getTotal());
        salesOrderEntity.setCreatedAt(new Timestamp(salesOrder.getCreated_at().getMillis()));
        salesOrderEntity.setUpdatedAt(new Timestamp(salesOrder.getUpdated_at().getMillis()));
        salesOrderEntity.setCustomer(customerEntity);

        ArrayList<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (OrderItem orderItem: salesOrder.getOrder_items()) {
            OrderItemEntity orderItemEntity = new OrderItemEntity();
            orderItemEntity.setCreatedAt(new Timestamp(orderItem.getCreated_at().getMillis()));
            orderItemEntity.setUpdatedAt(new Timestamp(orderItem.getUpdated_at().getMillis()));
            orderItemEntity.setId(orderItem.getId());
            orderItemEntity.setQuantity(orderItem.getQuantity());
            orderItemEntity.setUnitPrice(orderItem.getUnit_price());
            orderItemEntity.setTotalPrice(orderItem.getTotal_price());
            ProductEntity productEntity = mDataStore.findByKey(ProductEntity.class, orderItem.getProduct_id()).blockingGet();
            if (productEntity != null) {
                orderItemEntity.setProduct(productEntity);
                orderItemEntities.add(orderItemEntity);
            }
        }

        if (!orderItemEntities.isEmpty()) {
            mDataStore.upsert(salesOrderEntity).subscribe(orderEntity -> {
                for (OrderItemEntity orderItemEntity: orderItemEntities) {
                    orderItemEntity.setSalesOrder(orderEntity);
                    mDataStore.upsert(orderItemEntity).subscribe(/*no-op*/);
                }
                showNotification(notificationObject, salesOrder.getId());
            });
        }
    }

    private void showNotification(JSONObject notificationObject, int salesOrderId) {
        try {
            String title = notificationObject.getString("title");
            String message = notificationObject.getString("message");
            String imageUrl = notificationObject.getString("image");
            String timestamp = notificationObject.getString("timestamp");

            if (Foreground.get().isForeground()) {
                // app is in foreground, broadcast the push message
                Intent pushNotification = new Intent(Constants.PUSH_NOTIFICATION);
                pushNotification.putExtra(Constants.NOTIFICATION_MESSAGE, message);
                pushNotification.putExtra(Constants.NOTIFICATION_TYPE, Constants.ORDER_RECEIVED_NOTIFICATION);
                pushNotification.putExtra(Constants.NOTIFICATION_ORDER_ID, salesOrderId);
                LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

                Intent resultIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                resultIntent.putExtra(Constants.NOTIFICATION_MESSAGE, message);
                resultIntent.putExtra(Constants.NOTIFICATION_TYPE, Constants.ORDER_RECEIVED_NOTIFICATION);
                resultIntent.putExtra(Constants.NOTIFICATION_ORDER_ID, salesOrderId);
                // check for image attachment
                if (TextUtils.isEmpty(imageUrl)) {
                    showNotificationMessage(getApplicationContext(), title, message, timestamp, resultIntent);
                } else {
                    // image is present, show notification with image
                    showNotificationMessageWithBigImage(getApplicationContext(), title, message, timestamp, resultIntent, imageUrl);
                }
            } else if (Foreground.get().isBackground()){
                // app is in background, show the notification in notification tray
                Intent resultIntent = new Intent(getApplicationContext(), MerchantBackOfficeActivity.class);
                resultIntent.putExtra(Constants.NOTIFICATION_MESSAGE, message);
                resultIntent.putExtra(Constants.NOTIFICATION_TYPE, Constants.ORDER_RECEIVED_NOTIFICATION);
                resultIntent.putExtra(Constants.NOTIFICATION_ORDER_ID, salesOrderId);

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
        }
    }
}
