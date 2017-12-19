package co.loystar.loystarbusiness.models;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.sql.SQLException;
import java.util.List;

import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import io.requery.sql.ResultSetIterator;

/**
 * Created by ordgen on 12/19/17.
 */

public class CustomerContentProvider extends ContentProvider {
    private static final String[] COLUMNS = {
        "_id",  // must include this column
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
    };

    private Context mContext;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(
        @NonNull Uri uri,
        @Nullable String[] projection,
        @Nullable String selection,
        @Nullable String[] selectionArgs,
        @Nullable String sortOrder
    ) {
        ReactiveEntityStore<Persistable> mDataStore = DatabaseManager.getDataStore(mContext);
        SessionManager sessionManager = new SessionManager(mContext);
        MerchantEntity merchantEntity = mDataStore.select(MerchantEntity.class)
            .where(MerchantEntity.ID.eq(sessionManager.getMerchantId()))
            .get()
            .firstOrNull();

        if (selectionArgs != null) {
            String searchText = selectionArgs[0];
            String query = searchText.substring(0, 1).equals("0") ? searchText.substring(1) : searchText;
            String searchQuery = "%" + query.toLowerCase() + "%";

            List<CustomerEntity> entityList;

            if (TextUtilsHelper.isInteger(searchText)) {
                Selection<ReactiveResult<CustomerEntity>> phoneSelection = mDataStore.select(CustomerEntity.class);
                phoneSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                phoneSelection.where(CustomerEntity.DELETED.notEqual(true));
                phoneSelection.where(CustomerEntity.PHONE_NUMBER.like(searchQuery));
                entityList = phoneSelection.get().toList();

            } else {
                Selection<ReactiveResult<CustomerEntity>> nameSelection = mDataStore.select(CustomerEntity.class);
                nameSelection.where(CustomerEntity.OWNER.eq(merchantEntity));
                nameSelection.where(CustomerEntity.DELETED.notEqual(true));
                nameSelection.where(CustomerEntity.FIRST_NAME.like(searchQuery));
                entityList = nameSelection.get().toList();
            }

            MatrixCursor cursor = new MatrixCursor(COLUMNS, entityList.size());
            MatrixCursor.RowBuilder builder;

            for (CustomerEntity customerEntity: entityList) {
                String lastName;
                if (customerEntity.getLastName() == null) {
                    lastName = "";
                } else {
                    lastName = customerEntity.getLastName();
                }
                String fullName = customerEntity.getFirstName() + " " + lastName;

                builder = cursor.newRow();
                builder.add(customerEntity.getId()); // compulsory _id column
                builder.add(fullName);
                builder.add(customerEntity.getPhoneNumber());
                builder.add(customerEntity.getId()); // used to retrieve LastPathSegment from uri
            }

            return cursor;
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
