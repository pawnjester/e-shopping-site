package co.loystar.loystarbusiness.utils;

import android.content.Context;

import java.util.TimeZone;

import co.loystar.loystarbusiness.BuildConfig;

/**
 * Created by ordgen on 7/4/17.
 */

public class SettingsUtils {
    static TimeZone getDisplayTimeZone(Context context) {
        return TimeZone.getTimeZone(BuildConfig.DEFAULT_TIMEZONE);
    }
}
