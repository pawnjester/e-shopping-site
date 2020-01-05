package co.loystar.loystarbusiness.utils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

/**
 * Created by ordgen on 11/15/17.
 */

public class DownloadCustomerList extends AsyncTask<String, Integer, Boolean> {
    private static final String TAG = DownloadCustomerList.class.getSimpleName();
    private ProgressDialog dialog;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;
    private WeakReference<AppCompatActivity> appReference;

    public DownloadCustomerList(AppCompatActivity context) {
        dialog = new ProgressDialog(context);
        mDatabaseManager = DatabaseManager.getInstance(context);
        mSessionManager = new SessionManager(context);
        appReference = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
        this.dialog.setMessage("Exporting customer list...");
        this.dialog.show();

    }

    @Override
    protected Boolean doInBackground(String... strings) {
        String fileName = "MyLoystarCustomerList.xls";
        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + File.separator + "Loystar");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        //file path
        File file = new File(directory, fileName);

        WorkbookSettings wbSettings = new WorkbookSettings();
        wbSettings.setLocale(new Locale("en", "EN"));
        WritableWorkbook workbook;
        try {
            workbook = Workbook.createWorkbook(file, wbSettings);
            WritableSheet sheet = workbook.createSheet("MyLoystarCustomerList", 0);
            try {
                sheet.addCell(new Label(0, 0, "First Name")); //column and row
                sheet.addCell(new Label(1, 0, "Last Name"));
                sheet.addCell(new Label(2, 0, "Phone Number"));
                sheet.addCell(new Label(3, 0, "Email"));
                sheet.addCell(new Label(4, 0, "Gender"));
                sheet.addCell(new Label(5, 0, "Total Points"));
                sheet.addCell(new Label(6, 0, "Total Stamps"));
                sheet.addCell(new Label(7, 0, "Date of Birth"));

                int index = 1;

                List<CustomerEntity> customerEntities = mDatabaseManager.getMerchantCustomers(mSessionManager.getMerchantId());
                for (CustomerEntity customer: customerEntities) {
                    Date dt = customer.getDateOfBirth();
                    String date = "";
                    if (dt != null) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(dt);
                        date = TextUtilsHelper.getFormattedDateString(calendar);
                    }

                    int customer_stamps = mDatabaseManager.getTotalCustomerStamps(mSessionManager.getMerchantId(), customer.getId());
                    int customer_points = mDatabaseManager.getTotalCustomerPoints(mSessionManager.getMerchantId(), customer.getId());

                    sheet.addCell(new Label(0, index, customer.getFirstName()));
                    sheet.addCell(new Label(1, index, customer.getLastName()));
                    sheet.addCell(new Label(2, index, customer.getPhoneNumber()));
                    sheet.addCell(new Label(3, index, customer.getEmail()));
                    sheet.addCell(new Label(4, index, customer.getSex()));
                    sheet.addCell(new Label(5, index, String.valueOf(customer_points)));
                    sheet.addCell(new Label(6, index, String.valueOf(customer_stamps)));
                    sheet.addCell(new Label(7, index, date));

                    index += 1;
                }

            }  catch (RowsExceededException e) {
                e.printStackTrace();
                return false;
            }catch (WriteException e) {
                e.printStackTrace();
                return false;
            }
            workbook.write();
            try {
                workbook.close();
            } catch (WriteException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);

        if (this.dialog.isShowing()){
            this.dialog.dismiss();
        }
        if (success){
            String fileName = "MyLoystarCustomerList.xls";
            File sdCard = Environment.getExternalStorageDirectory();
            File filePath = new File(sdCard.getAbsolutePath() + File.separator + "Loystar");
            final File file = new File(filePath, fileName);
            Log.e("QQQQ", file + "");
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(".XLS");

            final AppCompatActivity mContext = appReference.get();
            new AlertDialog.Builder(mContext)
                    .setTitle("Export successful!")
                    .setMessage("Click the button below to open your file or open 'MyLoystarCustomerList.xls' later from inside the Loystar folder on your phone. Thanks.")
                    .setPositiveButton(mContext.getString(R.string.open_file), (dialog, which) -> {
                        dialog.dismiss();
                        PackageManager packageManager = mContext.getPackageManager();
                        Uri uriForFile = FileProvider.getUriForFile(
                                mContext,
                                mContext.getApplicationContext().getPackageName() + ".co.loystar.loystarbusiness.provider",
                                file);
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(uriForFile, mime);
                        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                        if (list.size() > 0) {
                            mContext.startActivity(intent);
                        } else {
                            new AlertDialog.Builder(mContext)
                                    .setTitle("Sorry! No Excel reader found.")
                                    .setMessage("We couldn't open the file because you don't have an Excel reader installed.")
                                    .setPositiveButton("Download Excel Reader", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Intent.ACTION_VIEW)
                                                    .setData(Uri.parse("https://play.google.com/store/apps/details?id=cn.wps.moffice_eng&hl=en"));
                                            mContext.startActivity(intent);
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    })
                    .show();
        }
        else {
            Toast.makeText(appReference.get(), appReference.get().getString(R.string.error_export_failed), Toast.LENGTH_LONG).show();
        }
    }
}
