package co.loystar.loystarbusiness.utils.fcm;

import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

/**
 * Created by ordgen on 12/18/17.
 */

public class MyJobService extends JobService {
    private static final String TAG = MyJobService.class.getSimpleName();

    @Override
    public boolean onStartJob(JobParameters job) {
        Log.d(TAG, "Performing long running task in scheduled job");
        // TODO(developer): add long running task here.
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}
