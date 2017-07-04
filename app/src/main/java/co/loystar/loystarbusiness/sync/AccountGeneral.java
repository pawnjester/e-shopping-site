package co.loystar.loystarbusiness.sync;

import java.util.Calendar;
import java.util.Date;

import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TimeUtils;

/**
 * Created by ordgen on 7/4/17.
 */

public class AccountGeneral {
    /**
     * Account type id
     */
    public static final String ACCOUNT_TYPE = "co.loystar.loystarbusiness";
    /**
     * Auth token types
     */
    public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
    public static final String AUTHORITY = "co.loystar.loystarbusiness.provider";
    public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to Loystar account";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to Loystar account";

    public static boolean merchantAccountIsActive() {
        DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
        SessionManager sessionManager = new SessionManager(LoystarApplication.getInstance());
        DBMerchant dbMerchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());
        if (dbMerchant != null) {
            Date currentDate = TimeUtils.getCurrentDateAndTime();
            Date subscriptionExpiryDate = dbMerchant.getSubscription_expires_on();
            if (subscriptionExpiryDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(subscriptionExpiryDate);
                cal.add(Calendar.DATE, 7);
                if (cal.getTime().after(currentDate)) {
                    return true;
                }
            }
        }
        return false;
    }
}
