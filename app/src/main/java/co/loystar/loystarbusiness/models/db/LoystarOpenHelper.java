package co.loystar.loystarbusiness.models.db;


import android.content.Context;
import android.util.Log;
import org.greenrobot.greendao.database.Database;

/**
 * Created by laudbruce-tagoe on 3/6/16.
 */


public class LoystarOpenHelper extends DaoMaster.OpenHelper{
    public LoystarOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);

        Log.e("greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion + " by migrating all tables data");

        MigrationHelper.getInstance().migrate(oldVersion, db,
                DBMerchantDao.class,
                DBCustomerDao.class,
                DBMerchantLoyaltyProgramDao.class,
                DBTransactionDao.class,
                DBBirthdayOfferDao.class,
                DBBirthdayOfferPresetSMSDao.class,
                DBProductDao.class,
                DBProductCategoryDao.class,
                DBSubscriptionDao.class
        );
    }
}
