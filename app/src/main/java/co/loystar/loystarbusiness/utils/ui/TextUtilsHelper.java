package co.loystar.loystarbusiness.utils.ui;

import android.text.Html;
import android.text.Spanned;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ordgen on 11/1/17.
 */

public class TextUtilsHelper {
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    public static String getFormattedDateString(Calendar calendar) {
        String m = (calendar.get(Calendar.MONTH) + 1) < 10 ? ("0" + (calendar.get(Calendar.MONTH) + 1)) : String.valueOf(calendar.get(Calendar.MONTH) + 1);
        String d = calendar.get(Calendar.DATE) < 10 ? ("0" + calendar.get(Calendar.DATE)) : String.valueOf(calendar.get(Calendar.DATE));
        return  calendar.get(Calendar.YEAR) + "-" + m + "-" + d;
    }

    public static boolean isValidEmailAddress(String emailAddress) {
        String emailRegEx;
        Pattern pattern;
        // Regex for a valid email address
        emailRegEx = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,4}$";
        // Compare the regex with the email address
        pattern = Pattern.compile(emailRegEx);
        Matcher matcher = pattern.matcher(emailAddress);
        return matcher.find();
    }
}
