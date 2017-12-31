package co.loystar.loystarbusiness.utils.fcm;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import co.loystar.loystarbusiness.auth.api.ApiUtils;
import co.loystar.loystarbusiness.models.databinders.SalesOrder;
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
                JSONObject jsonObj = new JSONObject(extras.getString("data", ""));
                ResponseBody valueToConvert  = ResponseBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.getJSONObject("payload").toString());
                ObjectMapper objectMapper = ApiUtils.getObjectMapper(false);
                JavaType javaType = objectMapper.getTypeFactory().constructType(SalesOrder.class);
                ObjectReader reader = objectMapper.readerFor(javaType);
                SalesOrder salesOrder = reader.readValue(valueToConvert.charStream());
                Log.e(TAG, "salesOrder: " + salesOrder.getStatus() );
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "JSONException: " + e.getMessage() );
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.getMessage() );
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}
