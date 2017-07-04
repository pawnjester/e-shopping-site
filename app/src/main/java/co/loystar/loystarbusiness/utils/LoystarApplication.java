package co.loystar.loystarbusiness.utils;

import android.app.Application;

/**
 * Created by ordgen on 7/3/17.
 */

public class LoystarApplication extends Application {
    private static LoystarApplication singleton;

    public static LoystarApplication getInstance() {
        return singleton;
    }
}
