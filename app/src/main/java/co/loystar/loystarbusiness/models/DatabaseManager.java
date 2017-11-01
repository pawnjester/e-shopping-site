package co.loystar.loystarbusiness.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Models;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveSupport;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;

/**
 * Created by ordgen on 11/1/17.
 */

public class DatabaseManager implements IDatabaseManager{
    private static final int DATABASE_VERSION = 1;
    private static DatabaseManager mInstance;
    private ReactiveEntityStore<Persistable> mDataStore;
    private StdDateFormat mDateFormat;

    private DatabaseManager(@NonNull ReactiveEntityStore<Persistable> data) {
        mDataStore = data;
        mDateFormat = new StdDateFormat();
    }

    public static @NonNull ReactiveEntityStore<Persistable> getDataStore(@NonNull Context context) {
        // override onUpgrade to handle migrating to a new version
        DatabaseSource source = new DatabaseSource(context, Models.DEFAULT, DATABASE_VERSION);

        if (BuildConfig.DEBUG) {
            // use this in development mode to drop and recreate the tables on every upgrade
            source.setTableCreationMode(TableCreationMode.DROP_CREATE);
        }

        Configuration configuration = source.getConfiguration();
        return ReactiveSupport.toReactiveStore(new EntityDataStore<Persistable>(configuration));
    }

    public static @NonNull DatabaseManager getInstance(@NonNull Context context) {
        if (mInstance == null) {
            mInstance = new DatabaseManager(getDataStore(context));
        }

        return mInstance;
    }

    @Nullable
    @Override
    public MerchantEntity getMerchant(int merchantId) {
        return mDataStore.select(MerchantEntity.class)
                .where(MerchantEntity.ID.eq(merchantId))
                .get()
                .firstOrNull();
    }
}
