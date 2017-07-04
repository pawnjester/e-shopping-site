package co.loystar.loystarbusiness.utils;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.List;

import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBCustomerDao;
import co.loystar.loystarbusiness.models.db.DaoSession;

import org.greenrobot.greendao.DaoLog;
import org.greenrobot.greendao.query.QueryBuilder;

/* Copy this code snippet into your AndroidManifest.xml inside the
<application> element:

    <provider
            android:name="co.loystar.loystarbusiness.utils.CustomerContentProvider"
            android:authorities="co.loystar.loystarbusiness.models.db.provider"/>
    */

public class CustomerContentProvider extends ContentProvider {

    public static final String AUTHORITY = "co.loystar.loystarbusiness.models.db.provider";
    public static final String BASE_PATH = "";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/" + BASE_PATH;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/" + BASE_PATH;

    private static final String TABLENAME = DBCustomerDao.TABLENAME;
    private static final String FIRST_NAME = DBCustomerDao.Properties.First_name.columnName;
    private static final String PHONE_NUMBER = DBCustomerDao.Properties.Phone_number.columnName;
    private static final String PK = DBCustomerDao.Properties.Id.columnName;

    private static final int DBUSER_DIR = 0;
    private static final int DBUSER_ID = 1;
    private static final int SEARCH_SUGGEST = 0;

    private static final UriMatcher sURIMatcher;

    private static final String[] COLUMNS = {
            "_id",  // must include this column
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
    };

    /**
     * This must be set from outside, it's recommended to do this inside your Application object.
     * Subject to change (static isn't nice).
     */
    public static DaoSession daoSession;

    static {
        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(AUTHORITY,
                SearchManager.SUGGEST_URI_PATH_QUERY,
                SEARCH_SUGGEST);
        sURIMatcher.addURI(AUTHORITY,
                SearchManager.SUGGEST_URI_PATH_QUERY +
                        "/*",
                SEARCH_SUGGEST);
    }

    @Override
    public boolean onCreate() {
         /*if(daoSession == null) {
            throw new IllegalStateException("DaoSession must be set before content provider is created");
         }*/
        DaoLog.d("Content Provider started: " + CONTENT_URI);
        return true;
    }

    protected SQLiteDatabase getDatabase() {
        if(daoSession == null) {
            throw new IllegalStateException("DaoSession must be set during content provider is active");
        }
        return LoystarApplication.getInstance().getDatabase();
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        long id = 0;
        String path = "";
        switch (uriType) {
            case DBUSER_DIR:
                id = getDatabase().insert(TABLENAME, null, values);
                path = BASE_PATH + "/" + id;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return Uri.parse(path);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = getDatabase();
        int rowsDeleted = 0;
        String id;
        switch (uriType) {
            case DBUSER_DIR:
                rowsDeleted = db.delete(TABLENAME, selection, selectionArgs);
                break;
            case DBUSER_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(TABLENAME, PK + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(TABLENAME, PK + "=" + id + " and "
                            + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = getDatabase();
        int rowsUpdated = 0;
        String id;
        switch (uriType) {
            case DBUSER_DIR:
                rowsUpdated = db.update(TABLENAME, values, selection, selectionArgs);
                break;
            case DBUSER_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(TABLENAME, values, PK + "=" + id, null);
                } else {
                    rowsUpdated = db.update(TABLENAME, values, PK + "=" + id
                            + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case DBUSER_DIR:
                queryBuilder.setTables(TABLENAME);
                break;
            case DBUSER_ID:
                queryBuilder.setTables(TABLENAME);
                queryBuilder.appendWhere(PK + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SessionManager sessionManager = new SessionManager(getContext());
        DBCustomerDao customerDao = daoSession.getDBCustomerDao();
        String searchText = selectionArgs[0];
        String query = searchText.substring(0, 1).equals("0") ? searchText.substring(1) : searchText;
        String searchQuery = "%" + query.toLowerCase() + "%";
        QueryBuilder<DBCustomer> qBuilder = customerDao.queryBuilder();

        if (TextUtilsHelper.isInteger(query)) {
            qBuilder.where(DBCustomerDao.Properties.Phone_number.like(searchQuery))
                    .where(DBCustomerDao.Properties.Merchant_id.eq(sessionManager.getMerchantId()));
        }
        else {
            qBuilder.where(DBCustomerDao.Properties.First_name.like(searchQuery))
                    .where(DBCustomerDao.Properties.Merchant_id.eq(sessionManager.getMerchantId()));
        }

        List<DBCustomer> dbCustomerList = qBuilder.list();

        MatrixCursor cursor = new MatrixCursor(COLUMNS, dbCustomerList.size());
        MatrixCursor.RowBuilder builder;
        for (DBCustomer customer: dbCustomerList) {
            String fullName = customer.getFirst_name() + " " + customer.getLast_name();
            builder = cursor.newRow();
            builder.add(customer.getId());
            builder.add(fullName);
            builder.add(customer.getPhone_number());
            builder.add(customer.getId());

        }

        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public final String getType(@NonNull Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case DBUSER_DIR:
                return CONTENT_TYPE;
            case DBUSER_ID:
                return CONTENT_ITEM_TYPE;
            default :
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}