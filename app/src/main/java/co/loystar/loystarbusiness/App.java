package co.loystar.loystarbusiness;

import android.graphics.Typeface;
import android.support.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;

import co.loystar.loystarbusiness.utils.TypefaceUtil;

/**
 * Created by ordgen on 11/1/17.
 */

public class App extends MultiDexApplication{
    private static App singleton;
    private Typeface latoFont;

    public static App getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        /*StrictMode.enableDefaults();*/
        singleton = this;
        extractLato();
        setGlobalFontType();
        singleton.initializeInstance();
        FirebaseApp.initializeApp(this);
    }

    protected void initializeInstance() {
    }

    private void extractLato() {
        latoFont = Typeface.createFromAsset(getAssets(), "fonts/Lato.ttf");
    }

    public Typeface getTypeface() {
        if (latoFont == null) {
            extractLato();
        }
        return latoFont;
    }

    /**
     * Using reflection to override default typeface
     */
    private void setGlobalFontType() {
        TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Lato.ttf");
    }
}
