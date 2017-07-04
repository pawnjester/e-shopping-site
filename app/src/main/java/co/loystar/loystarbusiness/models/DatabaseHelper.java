package co.loystar.loystarbusiness.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import org.joda.time.DateTime;
import org.joda.time.Days;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import co.loystar.loystarbusiness.models.db.DBBirthdayOffer;
import co.loystar.loystarbusiness.models.db.DBBirthdayOfferDao;
import co.loystar.loystarbusiness.models.db.DBBirthdayOfferPresetSMS;
import co.loystar.loystarbusiness.models.db.DBBirthdayOfferPresetSMSDao;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBCustomerDao;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.models.db.DBMerchantDao;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgramDao;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.models.db.DBProductCategory;
import co.loystar.loystarbusiness.models.db.DBProductCategoryDao;
import co.loystar.loystarbusiness.models.db.DBProductDao;
import co.loystar.loystarbusiness.models.db.DBSubscription;
import co.loystar.loystarbusiness.models.db.DBSubscriptionDao;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.models.db.DBTransactionDao;
import co.loystar.loystarbusiness.models.db.DaoMaster;
import co.loystar.loystarbusiness.models.db.DaoSession;
import co.loystar.loystarbusiness.models.db.LoystarOpenHelper;
import co.loystar.loystarbusiness.utils.GraphCoordinates;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import org.greenrobot.greendao.async.AsyncOperation;
import org.greenrobot.greendao.async.AsyncOperationListener;
import org.greenrobot.greendao.async.AsyncSession;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

/**
 * Created by laudbruce-tagoe on 2/24/17.
 */

public class DatabaseHelper  extends LoystarOpenHelper implements AsyncOperationListener {
    private SQLiteDatabase database;
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private AsyncSession asyncSession;
    private Context mContext;
    private static final String TAG = DatabaseHelper.class.getCanonicalName();

    public DatabaseHelper(Context context, String name) {
        super(context, name);
        mContext = context.getApplicationContext();
    }

    private void openWritableDb() throws SQLiteException {
        database = this.getWritableDatabase();
        daoMaster = new DaoMaster(database);
        daoSession = LoystarApplication.getInstance().getDaoSession();
        asyncSession = daoSession.startAsyncSession();
        asyncSession.setListener(this);
    }

    private void openReadableDb() throws SQLiteException {
        database = this.getReadableDatabase();
        daoMaster = new DaoMaster(database);
        daoSession = daoMaster.newSession();
        asyncSession = daoSession.startAsyncSession();
        asyncSession.setListener(this);
    }

    /***************************************************** MERCHANTS START ***************************************************************************************/

    public Long insertMerchant(DBMerchant merchant) {
        Long merchantId = null;
        try {
            if (merchant != null) {
                openWritableDb();
                DBMerchantDao merchantDao = daoSession.getDBMerchantDao();
                merchantId = merchantDao.insert(merchant);
                Log.d(TAG, "Inserted merchant with email: " + merchant.getEmail());
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return merchantId;
    }

    public Boolean updateMerchant(DBMerchant merchant) {
        try {
            if (merchant != null) {
                openWritableDb();
                DBMerchantDao merchantDao = daoSession.getDBMerchantDao();
                merchantDao.update(merchant);
                Log.d(TAG, "Updated merchant with email: " + merchant.getEmail());
                daoSession.clear();
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Updating merchant error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public synchronized DBMerchant getMerchantById(Long merchantId) {
        DBMerchant merchant = null;
        try {
            openReadableDb();
            DBMerchantDao dbMerchantDao = daoSession.getDBMerchantDao();
            merchant = dbMerchantDao.load(merchantId);
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return merchant;
    }


    /***************************************************** MERCHANTS END ***************************************************************************************/

    /************************************************************ CUSTOMERS START **************************************************************************************/

    public Long insertCustomer(DBCustomer customer) {
        Long customerId = null;
        try {
            if (customer != null) {
                openWritableDb();
                DBCustomerDao customerDao = daoSession.getDBCustomerDao();
                customerId = customerDao.insert(customer);
                Log.e(TAG, "Inserted customer: " + customerId + " into the database.");
                daoSession.clear();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return customerId;
    }

    public boolean insertOrReplaceCustomer(DBCustomer customer) {
        try {
            if (customer != null) {
                openWritableDb();
                DBCustomerDao customerDao = daoSession.getDBCustomerDao();
                customerDao.insertOrReplace(customer);
                daoSession.clear();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteCustomer(DBCustomer customer) {
        try {
            if (customer != null) {
                openWritableDb();
                DBCustomerDao customerDao = daoSession.getDBCustomerDao();
                customerDao.delete(customer);
                SessionManager sessionManager = new SessionManager(mContext);
                ArrayList<DBTransaction> transactions = getAllUserTransactions(customer.getUser_id(), sessionManager.getMerchantId());
                for (DBTransaction transaction: transactions) {
                    deleteTransaction(transaction);
                }
                daoSession.clear();
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<DBCustomer> searchCustomersByNameOrNumber(String searchText, Long merchantId) {
        ArrayList<DBCustomer> customers = new ArrayList<>();
        try {
            openReadableDb();
            DBCustomerDao customerDao = daoSession.getDBCustomerDao();
            String query = searchText.substring(0, 1).equals("0") ? searchText.substring(1) : searchText;
            String searchQuery = "%" + query.toLowerCase() + "%";
            QueryBuilder<DBCustomer> queryBuilder = customerDao.queryBuilder();

            if (TextUtilsHelper.isInteger(query)) {
                queryBuilder.where(
                        DBCustomerDao.Properties.Phone_number.like(searchQuery))
                        .where(DBCustomerDao.Properties.Merchant_id.eq(merchantId));
            }
            else {
                queryBuilder.where(
                        DBCustomerDao.Properties.First_name.like(searchQuery))
                        .where(DBCustomerDao.Properties.Merchant_id.eq(merchantId));
            }

            customers.addAll(queryBuilder.list());

            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(customers, new Comparator<DBCustomer>() {
            @Override
            public int compare(DBCustomer user1, DBCustomer user2) {
                return user1.getFirst_name().compareToIgnoreCase(user2.getFirst_name());
            }
        });

        return customers;
    }

    public DBCustomer getCustomerByUserId(Long userId) {
        DBCustomer customer = null;
        try {
            openReadableDb();
            DBCustomerDao customerDao = daoSession.getDBCustomerDao();
            QueryBuilder<DBCustomer> queryBuilder = customerDao.queryBuilder().
                    where(DBCustomerDao.Properties.User_id.eq(userId));
            if (!queryBuilder.list().isEmpty()) {
                customer = queryBuilder.list().get(0);
            }
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customer;
    }

    public ArrayList<DBCustomer> getCustomersMarkedForDeletion(Long merchantId) {
        ArrayList<DBCustomer> customers = new ArrayList<>();
        if (merchantId != null) {
            try {
                openReadableDb();
                DBCustomerDao customerDao = daoSession.getDBCustomerDao();
                QueryBuilder<DBCustomer> queryBuilder = customerDao.queryBuilder().
                        where(DBCustomerDao.Properties.Merchant_id.eq(merchantId)).
                        where(DBCustomerDao.Properties.Deleted.eq(true));
                customers.addAll(queryBuilder.list());
                daoSession.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return customers;
    }

    public DBCustomer getCustomerById(Long customerId) {
        DBCustomer customer = null;
        try {
            openReadableDb();
            DBCustomerDao customerDao = daoSession.getDBCustomerDao();
            customer = customerDao.load(customerId);
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customer;
    }

    public ArrayList<DBCustomer> listMerchantCustomers(Long merchantId) {
        ArrayList<DBCustomer> customers = new ArrayList<>();
        try {
            if (merchantId != null) {
                openReadableDb();
                DBCustomerDao customerDao = daoSession.getDBCustomerDao();
                QueryBuilder<DBCustomer> queryBuilder = customerDao.queryBuilder()
                        .where(DBCustomerDao.Properties.Merchant_id.eq(merchantId))
                        .where(DBCustomerDao.Properties.Deleted.eq(false));

                customers.addAll(queryBuilder.list());
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customers;
    }

    public DBCustomer updateCustomer(DBCustomer customer) {
        try {
            if (customer != null) {
                openReadableDb();
                DBCustomerDao customerDao = daoSession.getDBCustomerDao();
                customerDao.update(customer);
                Log.e(TAG, "updated customer: " + customer.getFirst_name() + " into the database.");
                daoSession.clear();
            }
        } catch (Exception e) {
            Log.e(TAG, "EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }
        return customer;
    }

    public DBCustomer getCustomerByPhone(String phone, Long merchantId) {
        DBCustomer customer = null;
        try {
            openReadableDb();
            DBCustomerDao customerDao = daoSession.getDBCustomerDao();
            QueryBuilder<DBCustomer> queryBuilder = customerDao.queryBuilder()
                    .where(DBCustomerDao.Properties.Phone_number.eq(phone))
                    .where(DBCustomerDao.Properties.Merchant_id.eq(merchantId));
            List<DBCustomer> list = queryBuilder.list();
            if (!list.isEmpty()) {
                customer = list.get(0);
            }
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return customer;
    }

    /**************************************************** USERS END **************************************************************************************/

    /*********************************************************************** TRANSACTIONS ***************************************************************/
    public Long insertTransaction(DBTransaction transaction) {
        Long transactionId = null;
        try {
            if (transaction != null) {
                openWritableDb();
                DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
                transactionId = transactionDao.insert(transaction);
                Log.e(TAG, "Inserted transaction: " + transaction.getId() + " into the DB.");
                daoSession.clear();
            }
        } catch (Exception e) {
            Log.e("REQ", "Exception: " + e.getMessage());
            e.printStackTrace();
            //Crashlytics.logException(e);
        }
        return transactionId;
    }

    public Long insertOrReplaceTransaction(DBTransaction transaction) {
        Long transactionId = null;
        try {
            if (transaction != null) {
                openWritableDb();
                DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
                transactionId = transactionDao.insertOrReplace(transaction);
                Log.e(TAG, "Inserted transaction: " + transaction.getId() + " into the DB.");
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Crashlytics.logException(e);
        }
        return transactionId;
    }

    public ArrayList<DBTransaction> listMerchantTransactions(Long merchantId) {
        ArrayList<DBTransaction> transactions = new ArrayList<>();
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().
                    where(DBTransactionDao.Properties.Merchant_id.eq(merchantId));
            transactions.addAll(query.list());
            daoSession.clear();
        } catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }

        return transactions;
    }

    public ArrayList<DBTransaction> getAllUserPointsTransactionsForLoyaltyProgram(Long userId, Long programId, Long merchantId) {
        ArrayList<DBTransaction> transactions = new ArrayList<>();
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().where(
                    DBTransactionDao.Properties.User_id.eq(userId)).
                    where(DBTransactionDao.Properties.Merchant_loyalty_program_id.eq(programId)).
                    where(DBTransactionDao.Properties.Merchant_id.eq(merchantId)).
                    where(DBTransactionDao.Properties.Points.isNotNull());
            transactions.addAll(query.list());
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
            //Crashlytics.logException(e);
        }

        return transactions;
    }

    public int getTotalUserPointsForProgram(Long userId, Long programId, Long merchantId) {
        ArrayList<DBTransaction> allUserTransactions = getAllUserPointsTransactionsForLoyaltyProgram(userId, programId, merchantId);
        int totalPoints = 0;

        try {
            for (DBTransaction transaction: allUserTransactions) {
                totalPoints += transaction.getPoints();
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
        }
        return totalPoints;
    }

    public ArrayList<DBTransaction> getAllUserStampsTransactionsForLoyaltyProgram(Long userId, Long programId, Long merchantId) {
        ArrayList<DBTransaction> transactions = new ArrayList<>();
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().
                    where(DBTransactionDao.Properties.User_id.eq(userId)).
                    where(DBTransactionDao.Properties.Merchant_loyalty_program_id.eq(programId)).
                    where(DBTransactionDao.Properties.Merchant_id.eq(merchantId)).
                    where(DBTransactionDao.Properties.Stamps.isNotNull());
            transactions.addAll(query.list());
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
            //Crashlytics.logException(e);
        }

        return transactions;
    }

    public int getTotalUserStampsForProgram(Long userId, Long programId, Long merchantId) {
        ArrayList<DBTransaction> allUserTransactions = getAllUserStampsTransactionsForLoyaltyProgram(userId, programId, merchantId);
        int totalStamps = 0;

        try {
            for (DBTransaction transaction: allUserTransactions) {
                totalStamps += transaction.getStamps();
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
        }
        return totalStamps;
    }

    public ArrayList<DBTransaction> getSyncedTransactions(Long merchantId) {
        ArrayList<DBTransaction> transactions = new ArrayList<>();
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().
                    where(DBTransactionDao.Properties.Created_at.isNotNull())
                    .where(DBTransactionDao.Properties.Merchant_id.eq(merchantId));
            transactions.addAll(query.list());
            daoSession.clear();
        } catch (Exception e) {
            //Crashlytics.logException(e);
        }

        return transactions;
    }


    public int getTotalUserPointsForMerchant(Long userId, Long merchantId) {
        int points = 0;
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().where(
                    DBTransactionDao.Properties.User_id.eq(userId)).
                    where(DBTransactionDao.Properties.Merchant_id.eq(merchantId));
            List<DBTransaction> list = query.list();
            for (DBTransaction transaction: list) {
                if (transaction.getPoints() != null && transaction.getPoints() != 0) {
                    points += transaction.getPoints();
                }
            }
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  points;
    }

    public int getTotalUserStampsForMerchant(Long userId, Long merchantId) {
        int stamps = 0;
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().where(
                    DBTransactionDao.Properties.User_id.eq(userId)).
                    where(DBTransactionDao.Properties.Merchant_id.eq(merchantId));
            List<DBTransaction> list = query.list();
            for (DBTransaction transaction: list) {
                if (transaction.getStamps() != null) {
                    stamps += transaction.getStamps();
                }
            }
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
            //Crashlytics.logException(e);
        }
        return  stamps;
    }

    public int getTotalAmountSpentByUserForMerchant(Long userId, Long merchantId) {
        int amount = 0;
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().where(
                    DBTransactionDao.Properties.User_id.eq(userId)).
                    where(DBTransactionDao.Properties.Merchant_id.eq(merchantId));
            List<DBTransaction> list = query.list();

            for (DBTransaction transaction: list) {
                if (transaction.getAmount() != null) {
                    amount += transaction.getAmount();
                }
            }
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  amount;
    }

    public ArrayList<DBTransaction> getAllUserTransactions(Long userId, Long merchantId) {
        ArrayList<DBTransaction> transactions = new ArrayList<>();
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().
                    where(DBTransactionDao.Properties.User_id.eq(userId)).
                    where(DBTransactionDao.Properties.Merchant_id.eq(merchantId));
            transactions.addAll(query.list());
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public ArrayList<DBTransaction> getUnsyncedTransactions(Long merchantId) {
        ArrayList<DBTransaction> transactions = new ArrayList<>();
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().
                    where(DBTransactionDao.Properties.Synced.eq(false)).
                    where(DBTransactionDao.Properties.Merchant_id.eq(merchantId));
            transactions.addAll(query.list());
            daoSession.clear();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public DBTransaction getTransactionById(Long transactionId, Long merchantId) {
        DBTransaction transaction = null;
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder()
                    .where(DBTransactionDao.Properties.Id.eq(transactionId))
                    .where(DBTransactionDao.Properties.Merchant_id.eq(merchantId));
            if (!query.list().isEmpty()) {
                transaction = query.list().get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transaction;
    }

    public DBTransaction updateTransaction(DBTransaction transaction) {
        try {
            if (transaction != null) {
                openWritableDb();
                DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
                transactionDao.update(transaction);
                Log.e(TAG, "updated transaction: " + transaction.getId() + " into the database.");
                daoSession.clear();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return transaction;
    }

    public void deleteTransaction(DBTransaction transaction) {
        try {
            if (transaction != null) {
                openWritableDb();
                DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
                transactionDao.delete(transaction);
                daoSession.clear();
            }
        }catch (Exception e) {
            Log.e("REQ", "CODE:Exception " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getTotalUserAmountSpentForProgram(Long userId, Long programId) {
        int total_amount = 0;
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().where(
                    DBTransactionDao.Properties.User_id.eq(userId))
                    .where(DBTransactionDao.Properties.Merchant_loyalty_program_id.eq(programId));
            List<DBTransaction> list = query.list();
            for (DBTransaction transaction: list) {
                if (transaction.getAmount() != null && transaction.getAmount() != 0) {
                    total_amount += transaction.getAmount();
                }
            }
            daoSession.clear();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return total_amount;
    }

    public ArrayList<GraphCoordinates> getMerchantSalesHistory(Long merchantId, String type) {
        ArrayList<GraphCoordinates> graphCoordinates = new ArrayList<>();
        HashMap<Date, Integer> dateToAmount = new HashMap<>();
        DateFormat outFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            QueryBuilder<DBTransaction> query = transactionDao.queryBuilder().where(
                    DBTransactionDao.Properties.Merchant_id.eq(merchantId))
                    .where(DBTransactionDao.Properties.Local_db_created_at.isNotNull())
                    .where(DBTransactionDao.Properties.Amount.isNotNull());

            List<DBTransaction> transactionList = query.list();
            Calendar todayCalendar = Calendar.getInstance();
            String todayDateString = TextUtilsHelper.getFormattedDateString(todayCalendar);
            Date todayDateWithoutTimeStamp = outFormatter.parse(todayDateString);

            ArrayList<Date> transactionDatesFor2day = new ArrayList<>();

            for (DBTransaction transaction: transactionList) {

                Date createdAt = transaction.getLocal_db_created_at();
                Integer amount = transaction.getAmount();

                Calendar cal = Calendar.getInstance();
                cal.setTime(createdAt);
                String formattedDate = TextUtilsHelper.getFormattedDateString(cal);
                Date createdAtWithoutTime = outFormatter.parse(formattedDate);

                if (todayDateWithoutTimeStamp.equals(createdAtWithoutTime)) {
                    transactionDatesFor2day.add(createdAtWithoutTime);
                }

                if (dateToAmount.get(createdAtWithoutTime) != null) {
                    amount += dateToAmount.get(createdAtWithoutTime);
                }
                dateToAmount.put(createdAtWithoutTime, amount);
            }

            if (!dateToAmount.isEmpty()) {
                if (type.equals("daily") && transactionDatesFor2day.isEmpty()) {
                    dateToAmount.put(todayDateWithoutTimeStamp, 0);
                }

                ArrayList<GraphCoordinates> allSalesRecords = new ArrayList<>();


                for (Map.Entry<Date, Integer> entry : dateToAmount.entrySet()) {
                    allSalesRecords.add(new GraphCoordinates(entry.getKey(), entry.getValue()));
                }

                Collections.sort(allSalesRecords, new pairObjectDateComparator());
                Collections.reverse(allSalesRecords);

                for (GraphCoordinates pairObject: allSalesRecords) {
                    Log.e("GraphCoordinates", "date: " + pairObject.getX() + " amt: " + pairObject.getY());
                }

                if (type.equals("daily") && allSalesRecords.size() > 3) {
                    for (int i=0; i < 3; i++) {
                        GraphCoordinates record = allSalesRecords.get(i);
                        graphCoordinates.add(record);
                    }
                }
                else {
                    graphCoordinates.addAll(allSalesRecords);
                }

                Collections.sort(graphCoordinates, new pairObjectDateComparator());
            }

            daoSession.clear();

        }catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }

        return graphCoordinates;
    }

    private class pairObjectDateComparator implements Comparator<GraphCoordinates> {
        @Override
        public int compare(GraphCoordinates o1, GraphCoordinates o2) {
            return o1.getX().compareTo(o2.getX());
        }
    }

    private ArrayList<Date> getSalesDates(Date startDate) {
        ArrayList<Date> salesDates = new ArrayList<>();
        salesDates.add(startDate);
        DateFormat outFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        Calendar startDayCal = Calendar.getInstance();
        try {
            Calendar todayCalendar = Calendar.getInstance();
            String todayDateString = TextUtilsHelper.getFormattedDateString(todayCalendar);
            Date todayDateWithoutTimeStamp = outFormatter.parse(todayDateString);
            startDayCal.setTime(startDate);
            String startDateString = TextUtilsHelper.getFormattedDateString(startDayCal);
            Date startDateWithoutTimeStamp = outFormatter.parse(startDateString);

            //we need to get 3 days sales
            //the first sales date is the start date
            //if startDate is same as today's date then get the last two dates before today

            if (todayDateWithoutTimeStamp.equals(startDateWithoutTimeStamp)){
                for (int i=0; i<2; i++) {
                    startDayCal.add(Calendar.DAY_OF_MONTH, -1);
                    String dayString = TextUtilsHelper.getFormattedDateString(startDayCal);
                    Date nDate = outFormatter.parse(dayString);
                    salesDates.add(nDate);
                }
            }
            else {
                //add today's date to the sales dates
                salesDates.add(todayDateWithoutTimeStamp);

                DateTime todayDaytime = new DateTime(todayCalendar.getTime().getTime());
                DateTime startDaytime = new DateTime(startDate.getTime());
                Days d = Days.daysBetween(startDaytime, todayDaytime);
                int days = d.getDays();
                //if difference between today and startDate is 1 day
                if (days == 1) {
                    //add the day before startDate to the sales dates
                    startDayCal.add(Calendar.DAY_OF_MONTH, -1);
                    String dayBeforeStartDayString = TextUtilsHelper.getFormattedDateString(startDayCal);
                    Date dayBeforeStartDayDate = outFormatter.parse(dayBeforeStartDayString);
                    salesDates.add(dayBeforeStartDayDate);
                }
                else {
                    //add the day before today to the sales dates
                    todayCalendar.add(Calendar.DAY_OF_MONTH, -1);
                    String dayBeforeTodayString = TextUtilsHelper.getFormattedDateString(todayCalendar);
                    Date dayBeforeTodayDate = outFormatter.parse(dayBeforeTodayString);
                    salesDates.add(dayBeforeTodayDate);
                }
            }


        } catch (ParseException e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
        return salesDates;
    }

    private class transactionCreatedAtTimeComparator implements Comparator<DBTransaction> {
        @Override
        public int compare(DBTransaction o1, DBTransaction o2) {
            return o1.getLocal_db_created_at().compareTo(o2.getLocal_db_created_at());
        }
    }

    public ArrayList<DBTransaction> getTransactionsForDateRange(Date from, Date to, Long merchantId) {
        ArrayList<DBTransaction> transactions = new ArrayList<>();
        try {
            openReadableDb();
            DBTransactionDao transactionDao = daoSession.getDBTransactionDao();
            Query<DBTransaction> query = transactionDao.queryBuilder()
                    .where(DBTransactionDao.Properties.Merchant_id.eq(merchantId))
                    .where(new WhereCondition.StringCondition(
                            "(SELECT CREATED_AT FROM " + DBTransactionDao.TABLENAME + " WHERE CREATED_AT " + " >= " + from.getTime() + " AND CREATED_AT " + " <= " + to.getTime() + " )"
                    )).build();
            transactions.addAll(query.list());
            daoSession.clear();
        }catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
        return transactions;
    }

    /**************************************** TRANSACTIONS END *****************************************************************/


    /************************************ MerchantLoyaltyPrograms START *********************************************************************/

    public Long insertProgram(DBMerchantLoyaltyProgram program) {
        Long programId = null;
        try {
            if (program != null) {
                openWritableDb();
                DBMerchantLoyaltyProgramDao dbMerchantLoyaltyProgramDao = daoSession.getDBMerchantLoyaltyProgramDao();
                DBMerchantLoyaltyProgram duplicateProgram = getProgramById(program.getId(), program.getMerchant_id());
                if (duplicateProgram == null) {
                    programId = dbMerchantLoyaltyProgramDao.insertOrReplace(program);
                    Log.e(TAG, "Inserted program with Id: " + programId + " into the database.");
                    daoSession.clear();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

        return programId;
    }

    public boolean insertOrReplaceMerchantLoyaltyProgram(DBMerchantLoyaltyProgram loyaltyProgram) {
        try {
            if (loyaltyProgram != null) {
                DBMerchantLoyaltyProgramDao programDao = daoSession.getDBMerchantLoyaltyProgramDao();
                programDao.insertOrReplace(loyaltyProgram);
                daoSession.clear();
                return true;
            }

        } catch (Exception e) {
            //Crashlytics.logException(e);
        }
        return false;
    }



    public DBMerchantLoyaltyProgram updateProgram(DBMerchantLoyaltyProgram program) {
        try {
            if (program != null) {
                openWritableDb();
                DBMerchantLoyaltyProgramDao dbMerchantLoyaltyProgramDao = daoSession.getDBMerchantLoyaltyProgramDao();
                dbMerchantLoyaltyProgramDao.update(program);
                Log.e(TAG, "updated program: " + program.getProgram_type() + " inside the DB.");
                daoSession.clear();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

        return program;
    }

    public boolean deleteLoyaltyProgram(DBMerchantLoyaltyProgram dbMerchantLoyaltyProgram) {
        try {
            if (dbMerchantLoyaltyProgram != null) {
                openWritableDb();
                DBMerchantLoyaltyProgramDao dbMerchantLoyaltyProgramDao = daoSession.getDBMerchantLoyaltyProgramDao();
                dbMerchantLoyaltyProgramDao.delete(dbMerchantLoyaltyProgram);
                daoSession.clear();
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<DBMerchantLoyaltyProgram> getProgramsMarkedForDeletion(Long merchantId) {
        ArrayList<DBMerchantLoyaltyProgram> programs = new ArrayList<>();
        try {
            openReadableDb();
            DBMerchantLoyaltyProgramDao programDao = daoSession.getDBMerchantLoyaltyProgramDao();
            QueryBuilder<DBMerchantLoyaltyProgram> queryBuilder = programDao.queryBuilder()
                    .where(DBMerchantLoyaltyProgramDao.Properties.Deleted.eq(true))
                    .where(DBMerchantLoyaltyProgramDao.Properties.Merchant_id.eq(merchantId));
            programs.addAll(queryBuilder.list());
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return programs;
    }

    public ArrayList<DBMerchantLoyaltyProgram> getProgramsWithServerUpdateTime(Long merchantId) {
        ArrayList<DBMerchantLoyaltyProgram> programs = new ArrayList<>();
        try {
            openReadableDb();
            DBMerchantLoyaltyProgramDao programDao = daoSession.getDBMerchantLoyaltyProgramDao();
            QueryBuilder<DBMerchantLoyaltyProgram> queryBuilder = programDao.queryBuilder()
                    .where(DBMerchantLoyaltyProgramDao.Properties.Updated_at.isNotNull())
                    .where(DBMerchantLoyaltyProgramDao.Properties.Merchant_id.eq(merchantId));
            programs.addAll(queryBuilder.list());
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return programs;
    }

    public DBMerchantLoyaltyProgram getProgramById(Long programId, Long merchantId) {
        DBMerchantLoyaltyProgram program = null;
        try {
            openReadableDb();
            DBMerchantLoyaltyProgramDao dbMerchantLoyaltyProgramDao = daoSession.getDBMerchantLoyaltyProgramDao();
            QueryBuilder<DBMerchantLoyaltyProgram> queryBuilder = dbMerchantLoyaltyProgramDao.queryBuilder()
                    .where(DBMerchantLoyaltyProgramDao.Properties.Id.eq(programId))
                    .where(DBMerchantLoyaltyProgramDao.Properties.Merchant_id.eq(merchantId));

            if (queryBuilder.list().size() > 0)
                program = queryBuilder.list().get(0);
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return program;
    }

    public ArrayList<DBMerchantLoyaltyProgram> listMerchantPrograms(Long merchantId) {
        ArrayList<DBMerchantLoyaltyProgram> programs = new ArrayList<>();
        try {
            if (merchantId != null) {
                openReadableDb();
                DBMerchantLoyaltyProgramDao programDao = daoSession.getDBMerchantLoyaltyProgramDao();
                QueryBuilder<DBMerchantLoyaltyProgram> queryBuilder = programDao.queryBuilder().
                        where(DBMerchantLoyaltyProgramDao.Properties.Merchant_id.eq(merchantId))
                        .where(DBMerchantLoyaltyProgramDao.Properties.Deleted.eq(false));
                programs.addAll(queryBuilder.list());
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return programs;
    }

    /************************************ MerchantLoyaltyPrograms END *********************************************************************/




    /******************************* BirthdayOfferPresets START ***************************************************/

    public Long insertBirthdayOfferPresetSMS(DBBirthdayOfferPresetSMS birthdayOfferPresetSMS) {
        Long birthdayOfferPresetSMSId = null;
        try {
            if (birthdayOfferPresetSMS != null) {
                openWritableDb();
                DBBirthdayOfferPresetSMSDao birthdayOfferPresetSMSDao = daoSession.getDBBirthdayOfferPresetSMSDao();
                birthdayOfferPresetSMSId = birthdayOfferPresetSMSDao.insert(birthdayOfferPresetSMS);
                Log.e(TAG, "Inserted birthdayOfferPresetSMS: " + birthdayOfferPresetSMSId + " into the database.");
                daoSession.clear();

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        return birthdayOfferPresetSMSId;
    }

    public boolean insertOrReplaceBirthdayOfferPresetSMS(DBBirthdayOfferPresetSMS birthdayOfferPresetSMS) {
        try {
            if (birthdayOfferPresetSMS != null) {
                openWritableDb();
                DBBirthdayOfferPresetSMSDao birthdayOfferPresetSMSDao = daoSession.getDBBirthdayOfferPresetSMSDao();
                birthdayOfferPresetSMSDao.insertOrReplace(birthdayOfferPresetSMS);
                daoSession.clear();
                return true;
            }

        } catch (Exception e) {
            //Crashlytics.logException(e);
        }
        return false;
    }

    public DBBirthdayOfferPresetSMS updateBirthdayOfferPresetSMS(DBBirthdayOfferPresetSMS birthdayOfferPresetSMS) {
        try {
            if (birthdayOfferPresetSMS != null) {
                openWritableDb();
                DBBirthdayOfferPresetSMSDao birthdayOfferPresetSMSDao = daoSession.getDBBirthdayOfferPresetSMSDao();
                birthdayOfferPresetSMSDao.update(birthdayOfferPresetSMS);
                Log.e(TAG, "updated birthdayOfferPresetSMS: " + birthdayOfferPresetSMS.getId() + " into the database.");
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return birthdayOfferPresetSMS;
    }

    public boolean deleteBirthdayOfferPresetSMS(DBBirthdayOfferPresetSMS birthdayOfferPresetSMS) {
        try {
            if (birthdayOfferPresetSMS != null) {
                openWritableDb();
                DBBirthdayOfferPresetSMSDao birthdayOfferPresetSMSDao = daoSession.getDBBirthdayOfferPresetSMSDao();
                birthdayOfferPresetSMSDao.delete(birthdayOfferPresetSMS);
                daoSession.clear();
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public DBBirthdayOfferPresetSMS getBirthdayPresetSMSByMerchantId(Long merchantId) {
        DBBirthdayOfferPresetSMS birthdayOfferPresetSMS = null;
        try {
            if (merchantId != null) {
                openReadableDb();
                DBBirthdayOfferPresetSMSDao birthdayOfferPresetSMSDao = daoSession.getDBBirthdayOfferPresetSMSDao();
                QueryBuilder<DBBirthdayOfferPresetSMS> queryBuilder = birthdayOfferPresetSMSDao.queryBuilder()
                        .where(DBBirthdayOfferPresetSMSDao.Properties.Merchant_id.eq(merchantId));
                List<DBBirthdayOfferPresetSMS> list = queryBuilder.list();
                if (!list.isEmpty()) {
                    birthdayOfferPresetSMS = list.get(list.size() -1);
                }
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return birthdayOfferPresetSMS;
    }

    /******************************* BirthdayOfferPresets END ***************************************************/



    /******************************* BirthdayOffers START ***************************************************/

    public Long insertBirthdayOffer(DBBirthdayOffer birthdayOffer) {
        Long birthdayOfferId = null;
        try {
            if (birthdayOffer != null) {
                openWritableDb();
                DBBirthdayOfferDao birthdayOfferDao = daoSession.getDBBirthdayOfferDao();
                birthdayOfferId = birthdayOfferDao.insert(birthdayOffer);
                Log.e(TAG, "Inserted birthdayOffer: " + birthdayOfferId + " into the database.");
                daoSession.clear();

            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

        return birthdayOfferId;
    }

    public boolean insertOrReplaceBirthdayOffer(DBBirthdayOffer birthdayOffer) {
        try {
            if (birthdayOffer != null) {
                openWritableDb();
                DBBirthdayOfferDao birthdayOfferDao = daoSession.getDBBirthdayOfferDao();
                birthdayOfferDao.insertOrReplace(birthdayOffer);
                daoSession.clear();
                return true;
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
        }
        return false;
    }

    public DBBirthdayOffer updateBirthdayOffer(DBBirthdayOffer birthdayOffer) {
        try {
            if (birthdayOffer != null) {
                openWritableDb();
                DBBirthdayOfferDao birthdayOfferDao = daoSession.getDBBirthdayOfferDao();
                birthdayOfferDao.update(birthdayOffer);
                Log.e(TAG, "updated birthdayOffer: " + birthdayOffer.getId() + " into the database.");
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return birthdayOffer;
    }

    public DBBirthdayOffer getBirthdayOfferById(Long birthdayOfferId) {
        DBBirthdayOffer birthdayOffer = null;
        try {
            openReadableDb();
            DBBirthdayOfferDao dbBirthdayOfferDao = daoSession.getDBBirthdayOfferDao();
            birthdayOffer = dbBirthdayOfferDao.load(birthdayOfferId);
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return birthdayOffer;
    }

    public DBBirthdayOffer getBirthdayOfferByMerchantId(Long merchantId) {
        DBBirthdayOffer birthdayOffer = null;
        try {
            openReadableDb();
            DBBirthdayOfferDao birthdayOfferDao = daoSession.getDBBirthdayOfferDao();
            QueryBuilder<DBBirthdayOffer> queryBuilder = birthdayOfferDao.queryBuilder().
                    where(DBBirthdayOfferDao.Properties.Merchant_id.eq(merchantId));
            List<DBBirthdayOffer> list = queryBuilder.list();
            if (!list.isEmpty()) {
                birthdayOffer = list.get(list.size() -1);
            }
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return birthdayOffer;
    }

    public boolean deleteBirthdayOffer(DBBirthdayOffer birthdayOffer) {
        try {
            if (birthdayOffer != null) {
                openWritableDb();
                DBBirthdayOfferDao birthdayOfferDao = daoSession.getDBBirthdayOfferDao();
                birthdayOfferDao.delete(birthdayOffer);
                daoSession.clear();
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /******************************* BirthdayOffers END ***************************************************/

    /******************************* ProductCategoriesActivity START ***************************************************/
    public Long insertProductCategory(DBProductCategory productCategory) {
        Long productCategoryId = null;
        try {
            if (productCategory != null) {
                openWritableDb();
                DBProductCategoryDao productCategoryDao = daoSession.getDBProductCategoryDao();
                productCategoryId = productCategoryDao.insert(productCategory);
                Log.e(TAG, "Inserted productCategory: " + productCategoryId + " into the database.");
                daoSession.clear();

            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

        return productCategoryId;
    }

    public boolean insertOrReplaceProductCategory(DBProductCategory productCategory) {
        try {
            if (productCategory != null) {
                openWritableDb();
                DBProductCategoryDao productCategoryDao = daoSession.getDBProductCategoryDao();
                productCategoryDao.insertOrReplace(productCategory);
                daoSession.clear();
                return true;
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<DBProductCategory> listMerchantProductCategories(Long merchantId) {
        ArrayList<DBProductCategory> productCategories = new ArrayList<>();
        try {
            if (merchantId != null) {
                openReadableDb();
                DBProductCategoryDao productCategoryDao = daoSession.getDBProductCategoryDao();
                QueryBuilder<DBProductCategory> queryBuilder = productCategoryDao.queryBuilder().
                        where(DBProductCategoryDao.Properties.Merchant_id.eq(merchantId))
                        .where(DBProductCategoryDao.Properties.Deleted.notEq(true));
                productCategories.addAll(queryBuilder.list());
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return productCategories;
    }

    public DBProductCategory getProductCategoryById(Long productCategoryId) {
        DBProductCategory productCategory = null;
        try {
            openReadableDb();
            DBProductCategoryDao productCategoryDao = daoSession.getDBProductCategoryDao();
            productCategory = productCategoryDao.load(productCategoryId);
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return productCategory;
    }

    public boolean deleteProductCategory(DBProductCategory category) {
        if (category != null) {
            try {
                openWritableDb();
                DBProductCategoryDao productCategoryDao = daoSession.getDBProductCategoryDao();

                productCategoryDao.delete(category);
                daoSession.clear();
                return true;
            } catch (Exception e) {
                //Crashlytics.logException(e);
            }
        }
        return false;
    }

    public DBProductCategory getProductCategoryByName(String name, Long merchantId) {
        DBProductCategory productCategory = null;
        try {
            if (name != null) {
                openReadableDb();
                DBProductCategoryDao productCategoryDao = daoSession.getDBProductCategoryDao();
                QueryBuilder<DBProductCategory> queryBuilder = productCategoryDao.queryBuilder().
                        where(DBProductCategoryDao.Properties.Merchant_id.eq(merchantId))
                        .where(DBProductCategoryDao.Properties.Name.eq(name));
                if (queryBuilder.list().size() > 0) {
                    productCategory = queryBuilder.list().get(0);
                };
                daoSession.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return productCategory;
    }

    public Boolean updateProductCategory(DBProductCategory category) {
        try {
            if (category != null) {
                openWritableDb();
                DBProductCategoryDao categoryDao = daoSession.getDBProductCategoryDao();
                categoryDao.update(category);
                Log.d(TAG, "Updated category with ID: " + category.getId());
                daoSession.clear();
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Updating category error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<DBProductCategory> searchProductCategoriesByName(String name, Long merchantId) {
        ArrayList<DBProductCategory> categories = new ArrayList<>();
        try {
            openReadableDb();
            DBProductCategoryDao categoryDao = daoSession.getDBProductCategoryDao();
            String searchQuery = "%" + name + "%";
            QueryBuilder<DBProductCategory> queryBuilder = categoryDao.queryBuilder();
            queryBuilder.where(
                    DBProductCategoryDao.Properties.Name.like(searchQuery))
                    .where(DBProductDao.Properties.Merchant_id.eq(merchantId));

            categories.addAll(queryBuilder.list());

            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return categories;
    }

    public ArrayList<DBProductCategory> getProductCategoriesMarkedForDeletion(Long merchantId) {
        ArrayList<DBProductCategory> categories = new ArrayList<>();
        if (merchantId != null) {
            try {
                openReadableDb();
                DBProductCategoryDao categoryDao = daoSession.getDBProductCategoryDao();
                QueryBuilder<DBProductCategory> queryBuilder = categoryDao.queryBuilder();
                queryBuilder.where(DBProductCategoryDao.Properties.Deleted.eq(true))
                        .where(DBProductCategoryDao.Properties.Merchant_id.eq(merchantId));
                categories.addAll(queryBuilder.list());

            } catch (Exception e) {
                //Crashlytics.logException(e);
            }
        }

        return categories;
    }

    /******************************* ProductCategoriesActivity END ***************************************************/

    /******************************* ProductsActivity START ***************************************************/
    public Long insertProduct(DBProduct product) {
        Long productId = null;
        try {
            if (product != null) {
                openWritableDb();
                DBProductDao productDao = daoSession.getDBProductDao();
                productId = productDao.insert(product);
                Log.e(TAG, "Inserted product: " + productId + " into the database.");
                daoSession.clear();

            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
        return productId;
    }

    public boolean insertOrReplaceProduct(DBProduct product) {
        try {
            if (product != null) {
                openWritableDb();
                DBProductDao productDao = daoSession.getDBProductDao();
                productDao.insertOrReplace(product);
                daoSession.clear();
                return true;
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<DBProduct> listMerchantProducts(Long merchantId) {
        ArrayList<DBProduct> products = new ArrayList<>();
        try {
            if (merchantId != null) {
                openReadableDb();
                DBProductDao productDao = daoSession.getDBProductDao();
                QueryBuilder<DBProduct> queryBuilder = productDao.queryBuilder().
                        where(DBProductDao.Properties.Merchant_id.eq(merchantId))
                        .where(DBProductDao.Properties.Deleted.notEq(true));
                products.addAll(queryBuilder.list());
                daoSession.clear();
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }

        return products;
    }

    public DBProduct getProductById(Long productId) {
        DBProduct product = null;
        try {
            openReadableDb();
            DBProductDao productDao = daoSession.getDBProductDao();
            product = productDao.load(productId);
            daoSession.clear();
        } catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
        return product;
    }

    public Boolean updateProduct(DBProduct product) {
        try {
            if (product != null) {
                openWritableDb();
                DBProductDao productDao = daoSession.getDBProductDao();
                productDao.update(product);
                daoSession.clear();
                return true;
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<DBProduct> getProductsMarkedForDeletion(Long merchantId) {
        ArrayList<DBProduct> products = new ArrayList<>();
        try {
            openReadableDb();
            DBProductDao productDao = daoSession.getDBProductDao();
            QueryBuilder<DBProduct> queryBuilder = productDao.queryBuilder()
                    .where(DBProductDao.Properties.Deleted.eq(true))
                    .where(DBProductDao.Properties.Merchant_id.eq(merchantId));
            products.addAll(queryBuilder.list());
            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }

    public ArrayList<DBProduct> getProductsByCategoryId(Long categoryId) {
        ArrayList<DBProduct> products = new ArrayList<>();
        if (categoryId != null) {
            try {
                openReadableDb();
                DBProductDao productDao = daoSession.getDBProductDao();
                QueryBuilder<DBProduct> queryBuilder = productDao.queryBuilder()
                        .where(DBProductDao.Properties.Merchant_product_category_id.eq(categoryId));
                products.addAll(queryBuilder.list());
                daoSession.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return products;
    }

    public boolean deleteProduct(DBProduct product) {
        try {
            if (product != null) {
                openWritableDb();
                DBProductDao productDao = daoSession.getDBProductDao();
                productDao.delete(product);
                daoSession.clear();
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<DBProduct> searchProductsByName(String name, Long merchantId) {
        ArrayList<DBProduct> products = new ArrayList<>();
        try {
            openReadableDb();
            DBProductDao productDao = daoSession.getDBProductDao();
            String searchQuery = "%" + name + "%";
            QueryBuilder<DBProduct> queryBuilder = productDao.queryBuilder();
            queryBuilder.where(
                    DBProductDao.Properties.Name.like(searchQuery))
                    .where(DBProductDao.Properties.Merchant_id.eq(merchantId));

            products.addAll(queryBuilder.list());

            daoSession.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }

    /******************************* ProductsActivity END ***************************************************/

    /******************************* Subscriptions START ***************************************************/

    public boolean insertOrReplaceSubscription(DBSubscription subscription) {
        try {
            if (subscription != null) {
                openWritableDb();
                DBSubscriptionDao subscriptionDao = daoSession.getDBSubscriptionDao();
                subscriptionDao.insertOrReplace(subscription);
                daoSession.clear();
                return true;
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
        }
        return false;
    }

    public DBSubscription getMerchantSubscription(Long merchantId) {
        DBSubscription subscription = null;
        try {
            if (merchantId != null) {
                openReadableDb();
                DBSubscriptionDao subscriptionDao = daoSession.getDBSubscriptionDao();
                QueryBuilder<DBSubscription> queryBuilder = subscriptionDao.queryBuilder();
                queryBuilder.where(DBSubscriptionDao.Properties.Merchant_id.eq(merchantId));
                if (!queryBuilder.list().isEmpty()) {
                    subscription = queryBuilder.list().get(0);
                }
            }
        } catch (Exception e) {
            //Crashlytics.logException(e);
        }
        return subscription;
    }


    /******************************* Subscriptions END ***************************************************/


    @Override
    public void onAsyncOperationCompleted(AsyncOperation operation) {

    }

    public void closeDB() {
        if (database != null && database.isOpen())
            database.close();
    }
}
