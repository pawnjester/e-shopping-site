package co.loystar.loystarbusiness.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.lang.reflect.Field;

/**
 * Created by ordgen on 11/1/17.
 */

public class TypefaceUtil {
    private static final String TAG = TypefaceUtil.class.getSimpleName();

    public static void overrideFont(Context context) {
        try {
            final Typeface customFontTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/Lato.ttf");

            final Field defaultFontTypefaceField = Typeface.class.getDeclaredField("SERIF");
            defaultFontTypefaceField.setAccessible(true);
            defaultFontTypefaceField.set(null, customFontTypeface);
        } catch (Exception e) {
            Log.e(TAG, "Can not set custom font to: " + "SERIF");
        }
    }
}
