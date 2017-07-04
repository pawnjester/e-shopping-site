package co.loystar.loystarbusiness.models.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.internal.DaoConfig;

import co.loystar.loystarbusiness.utils.LoystarApplication;

/**
 * Created by laudbruce-tagoe on 3/6/16.
 */
class MigrationHelper {
    private static final String CONVERSION_CLASS_NOT_FOUND_EXCEPTION = "MIGRATION HELPER - CLASS DOESN'T MATCH WITH THE CURRENT PARAMETERS";
    @SuppressLint("StaticFieldLeak")
    private static MigrationHelper instance;
    private Context mContext = LoystarApplication.getInstance().getApplicationContext();
    private static final String TAG = MigrationHelper.class.getSimpleName();

    public static MigrationHelper getInstance() {
        if(instance == null) {
            instance = new MigrationHelper();
        }
        return instance;
    }

    @SafeVarargs
    final void migrate(int oldDbVersion, Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        if (oldDbVersion < 92) {
            DaoMaster.dropAllTables(db, true);
            DaoMaster.createAllTables(db, false);

            if (databaseExists(mContext, "db_merchant") || databaseExists(mContext, "db_user") ||
                    databaseExists(mContext, "db_merchant_loyalty_program") || databaseExists(mContext, "db_transactions") ||
                    databaseExists(mContext, "db_birthday_offer") ||
                    databaseExists(mContext, "db_birthday_offer_preset_sms") || databaseExists(mContext, "dbDeletedUser")) {

                deleteOldDBInstances(mContext);
            }
        }
        else {
            generateTempTables(db, daoClasses);
            DaoMaster.dropAllTables(db, true);
            DaoMaster.createAllTables(db, false);
            restoreData(db, daoClasses);
        }

    }

    @SafeVarargs
    private final void generateTempTables(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            DaoConfig daoConfig = new DaoConfig(db, daoClass);

            String divider = "";
            String tableName = daoConfig.tablename;

            String tempTableName = daoConfig.tablename.concat("_TEMP");
            ArrayList<String> properties = new ArrayList<>();

            StringBuilder createTableStringBuilder = new StringBuilder();

            createTableStringBuilder.append("CREATE TABLE ").append(tempTableName).append(" (");

            for (int j = 0; j < daoConfig.properties.length; j++) {
                String columnName = daoConfig.properties[j].columnName;

                if (getColumns(db, tableName).contains(columnName)) {
                    properties.add(columnName);

                    String type = null;

                    try {
                        type = getTypeByClass(daoConfig.properties[j].type);
                    } catch (Exception ignored) {
                    }

                    createTableStringBuilder.append(divider).append(columnName).append(" ").append(type);

                    if (daoConfig.properties[j].primaryKey) {
                        createTableStringBuilder.append(" PRIMARY KEY");
                    }

                    divider = ",";
                }
            }
            createTableStringBuilder.append(");");

            db.execSQL(createTableStringBuilder.toString());

            db.execSQL("INSERT INTO " + tempTableName + " (" + TextUtils.join(",", properties) + ") SELECT " + TextUtils.join(",", properties) + " FROM " + tableName + ";");
        }
    }

    @SafeVarargs
    private final void restoreData(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            DaoConfig daoConfig = new DaoConfig(db, daoClass);

            String tableName = daoConfig.tablename;
            String tempTableName = daoConfig.tablename.concat("_TEMP");

            ArrayList<String> properties = new ArrayList<>();
            for (int j = 0; j < daoConfig.properties.length; j++) {
                String columnName = daoConfig.properties[j].columnName;

                if (getColumns(db, tempTableName).contains(columnName)) {
                    properties.add(columnName);
                }
            }

            db.execSQL("INSERT INTO " + tableName + " (" + TextUtils.join(",", properties) + ") SELECT " + TextUtils.join(",", properties) + " FROM " + tempTableName + ";");
            db.execSQL("DROP TABLE " + tempTableName);
        }
    }

    private String getTypeByClass(Class<?> type) throws Exception {
        if(type.equals(String.class)) {
            return "TEXT";
        }
        if(type.equals(Long.class) || type.equals(Integer.class) || type.equals(long.class)) {
            return "INTEGER";
        }
        if(type.equals(Boolean.class)) {
            return "BOOLEAN";
        }

        throw new Exception(CONVERSION_CLASS_NOT_FOUND_EXCEPTION.concat(" - Class: ").concat(type.toString()));
    }

    private static List<String> getColumns(Database db, String tableName) {
        List<String> columns = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " limit 1", null);
            if (cursor != null) {
                columns = new ArrayList<>(Arrays.asList(cursor.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return columns;
    }

    private boolean tableExists(Database db, String tableName) {
        if (tableName == null || db == null)
        {
            return false;
        }
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?", new String[] {"table", tableName});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }

    private static boolean databaseExists(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

    private void deleteOldDBInstances(Context mContext) {

        if (databaseExists(mContext, "db_merchant")) {
            boolean res = mContext.deleteDatabase("db_merchant");
            Log.e("REQ", "DBMERCHANT DELETED: " + res);
        }

        if (databaseExists(mContext, "db_user")) {
            boolean res = mContext.deleteDatabase("db_user");
            Log.e("REQ", "DBUSER DELETED: " + res);
        }

        if (databaseExists(mContext, "db_merchant_loyalty_program")) {
            boolean res = mContext.deleteDatabase("db_merchant_loyalty_program");
            Log.e("REQ", "DBMERCHANT_LOYALTY_PROGRAM DELETED: " + res);
        }

        if (databaseExists(mContext, "db_transactions")) {
            boolean res = mContext.deleteDatabase("db_transactions");
            Log.e("REQ", "DBTRANSACTION DELETED: " + res);
        }

        if (databaseExists(mContext, "db_birthday_offer")) {
            boolean res = mContext.deleteDatabase("db_birthday_offer");
            Log.e("REQ", "DBBIRTHDAY_OFFER DELETED: " + res);
        }

        if (databaseExists(mContext, "db_birthday_offer_preset_sms")) {
            boolean res = mContext.deleteDatabase("db_birthday_offer_preset_sms");
            Log.e("REQ", "DBBIRTHDAY_OFFER_PRESET_SMS DELETED: " + res);
        }

        if (databaseExists(mContext, "dbDeletedUser")) {
            boolean res = mContext.deleteDatabase("dbDeletedUser");
            Log.e("REQ", "DBDELETED_USER DELETED: " + res);
        }
    }
}
