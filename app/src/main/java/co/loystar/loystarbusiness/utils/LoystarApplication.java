package co.loystar.loystarbusiness.utils;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DaoMaster;
import co.loystar.loystarbusiness.models.db.DaoSession;
import io.smooch.core.Smooch;

/**
 * Created by ordgen on 7/3/17.
 */

public class LoystarApplication extends Application {
    public static final String PERMISSIONS_PREFERENCES = "PermissionsPref";
    public static final String DATABASE_NAME = "db_loystar_app";
    private Typeface latoFont;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private static LoystarApplication singleton;
    private DaoSession daoSession;
    private SQLiteDatabase database;
    private ApiClient mApiClient;

    public static LoystarApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        extractLato();
        setGlobalFontType();

        singleton.initializeInstance();

        /*initialize libraries*/

        /*Smooch*/
        Smooch.init(this, BuildConfig.SMOOCH_TOKEN);
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

    protected void initializeInstance() {
        databaseHelper = new DatabaseHelper(this, DATABASE_NAME);
        database = databaseHelper.getWritableDatabase();

        DaoMaster daoMaster = new DaoMaster(database);
        daoSession = daoMaster.newSession();
        sessionManager = new SessionManager(this);
        mApiClient = new ApiClient(this);
        //CustomerContentProvider.daoSession = daoSession;
    }

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ApiClient getApiClient() {
        return mApiClient;
    }
}
