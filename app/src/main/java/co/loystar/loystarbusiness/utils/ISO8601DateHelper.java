package co.loystar.loystarbusiness.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by laudbruce-tagoe on 4/22/16.
 */
public class ISO8601DateHelper {
    public static Calendar getCalendarFromISO(String datestring) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.UK);
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK);
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.UK);
        try {
            Date date = dateFormat1.parse(datestring);
            calendar.setTime(date);

        } catch (ParseException e) {
            Log.e("Parse", "ERROR 1: " + e.getMessage());
            try {
                Date date = dateFormat2.parse(datestring);
                calendar.setTime(date);
            } catch (ParseException e1) {
                Log.e("Parse", "ERROR 2: " + e1.getMessage());
            }
            e.printStackTrace();
        }

        return calendar;
    }
}
