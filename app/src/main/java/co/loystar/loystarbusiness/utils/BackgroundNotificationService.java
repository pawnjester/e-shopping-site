package co.loystar.loystarbusiness.utils;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.InvoicePayActivity;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.databinders.DownloadInvoice;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class BackgroundNotificationService extends IntentService {


    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private ApiClient mApiClient;
    private int invoiceId;
    private String invoiceNumber;
    private File invoiceFile;
    private boolean isPaid;

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        invoiceId = intent.getIntExtra(Constants.INVOICE_ID, 0);
        invoiceNumber = intent.getStringExtra(Constants.INVOICE_NUMBER);
        isPaid = intent.getBooleanExtra(Constants.RECEIPT_PAID, false);
        return super.onStartCommand(intent, flags, startId);
    }

    public BackgroundNotificationService() {
        super("Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        mApiClient = new ApiClient(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new
                    NotificationChannel("id",
                    "an", NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription("no sound");
            notificationChannel.setSound(null, null);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationBuilder = new NotificationCompat.Builder(this, "id")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(invoiceNumber)
                .setContentText("Downloading Invoice")
                .setDefaults(0)
                .setAutoCancel(true);
        notificationManager.notify(0, notificationBuilder.build());
        if (isPaid) {
            downloadReceipt(invoiceId);
        } else {
            downloadPdf(invoiceId);
        }
    }

    private void updateNotification(int currentProgress) {
        notificationBuilder.setProgress(100, currentProgress, false);
        notificationBuilder.setContentText("Downloaded: " + currentProgress + "%");
        notificationManager.notify(0, notificationBuilder.build());
    }

    private void sendProgressUpdate(boolean downloadComplete) {
        Intent intent = new Intent(InvoicePayActivity.PROGRESS_UPDATE);
        intent.putExtra("downloadComplete", downloadComplete);
        LocalBroadcastManager.getInstance(BackgroundNotificationService.this).sendBroadcast(intent);
    }

    private void onDownloadComplete(boolean downloadComplete) {
        sendProgressUpdate(downloadComplete);
        notificationManager.cancel(0);

        PackageManager packageManager = getApplicationContext().getPackageManager();
        Uri uriForFile = FileProvider.getUriForFile(getApplicationContext(),
                getApplicationContext().getPackageName()
                        + ".co.loystar.loystarbusiness.provider", invoiceFile);
        final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(".pdf");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uriForFile, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        TaskStackBuilder stackBuilder = TaskStackBuilder
                .create(BackgroundNotificationService.this);
        stackBuilder.addParentStack(InvoicePayActivity.class);
        stackBuilder.addNextIntent(intent);
        final PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);;
        notificationBuilder.addAction(
                R.drawable.smooch_btn_action, "Open", resultPendingIntent);

        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        notificationBuilder.setProgress(0, 0, false);
        if (isPaid) {
            notificationBuilder.setContentText("Receipt Download Complete");
        } else {
            notificationBuilder.setContentText("Invoice Download Complete");

        }
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        if (list.size() > 0) {
            notificationBuilder.setContentIntent(resultPendingIntent);
        }
        notificationManager.notify(0, notificationBuilder.build());


    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        notificationManager.cancel(0);
    }

    private void downloadPdf(int invoiceId) {
        mApiClient.getLoystarApi(false)
                .getInvoiceDownloadLink(invoiceId)
                .flatMap((Function<DownloadInvoice,
                        ObservableSource<Response<ResponseBody>>>) downloadInvoice ->
                        mApiClient.getLoystarApi(false)
                                .downloadInvoice(BuildConfig.FLAVOR.equals("production")
                                        ? downloadInvoice.getMessage().substring(22)
                                        : downloadInvoice.getMessage().substring(33)))
                .doOnNext(this::saveToDiskRx)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doOnError(error -> Toast.makeText(getApplicationContext(),
                        "Invoice could not be downloaded", Toast.LENGTH_SHORT).show())
                .subscribe(response -> {
                    if (response.code() == 404) {
                        Toast.makeText(getApplicationContext(),
                                "Invoice could not be downloaded", Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void downloadReceipt(int invoiceId) {
        mApiClient.getLoystarApi(false)
                .getReceiptDownloadLink(invoiceId)
                .flatMap((Function<DownloadInvoice,
                        ObservableSource<Response<ResponseBody>>>) downloadInvoice ->
                        mApiClient.getLoystarApi(false)
                                .downloadInvoice(BuildConfig.FLAVOR.equals("production")
                                        ? downloadInvoice.getMessage().substring(22)
                                        : downloadInvoice.getMessage().substring(33)))
                .doOnNext(this::saveToDiskRx)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doOnError(error -> Toast.makeText(getApplicationContext(),
                        "Receipt could not be downloaded", Toast.LENGTH_SHORT).show())
                .subscribe(response -> {
                    if (response.code() == 404) {
                        Toast.makeText(getApplicationContext(),
                                "Receipt could not be downloaded", Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private boolean saveToDiskRx(final Response<ResponseBody> response) {
        try {
            String header = response.headers().get("Content-Disposition");
            String filename = header.replace("attachment; filename=", "");
            String strippedFile = filename.substring(1, filename.length() -1);
            File sdCard = Environment.getExternalStorageDirectory();
            File filePath = new File(
                    sdCard.getAbsolutePath() + File.separator + "Loystar");
            if (!filePath.exists() || !filePath.isDirectory()) {
                filePath.mkdirs();
            }

            invoiceFile = new File(filePath, strippedFile);
            InputStream inputStream = null;
            OutputStream outputStream = null;
            boolean downloadComplete = false;

            try {
                byte[] fileReader = new byte[4096];
                int count;

                long fileSize = response.body().contentLength();
                long total = 0;

                inputStream = new BufferedInputStream(response.body().byteStream(), 1024 * 8);
                outputStream = new FileOutputStream(invoiceFile);

                while ((count = inputStream.read(fileReader)) != -1) {
                    total += count;
                    int progress = (int) ((double) (total * 100) / (double) fileSize);
                    updateNotification(progress);
                    outputStream.write(fileReader, 0, count);
                    downloadComplete = true;
                }
                onDownloadComplete(downloadComplete);
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                return true;
            } catch (IOException e) {
                onDownloadFailure();
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (Exception e) {
            Log.e(">>>", e +"");
            onDownloadFailure();
            return false;
        }
    }

    void onDownloadFailure() {
        sendProgressUpdate(false);
        notificationManager.cancel(0);
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.setContentText("Invoice Download Could not be completed");
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        notificationManager.notify(0, notificationBuilder.build());
    }
}
