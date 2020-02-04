package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.PaymentMessage;
import co.loystar.loystarbusiness.models.entities.PaymentMessageEntity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import timber.log.Timber;

public class PaymentMessageActivity extends AppCompatActivity {

    EditText message;

    Button setMessageButton;
    private View mLayout;
    private ApiClient mApiClient;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    private ReactiveEntityStore<Persistable> mDataStore;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_message);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Set Invoice Payment Message");
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setDisplayShowHomeEnabled(true);
        mDataStore = DatabaseManager.getDataStore(this);
        mLayout = findViewById(R.id.message_container);
        mApiClient = new ApiClient(this);
        message = findViewById(R.id.setPaymentMessage);
        builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.layout_loading_dialog);
        dialog = builder.create();

        setMessageButton = findViewById(R.id.setMessageButton);

        setMessageButton.setOnClickListener(view -> {
            if (isOnline(this)) {
                setPaymentMesage();
            }else {
                Snackbar.make(mLayout,
                        "Please connect to the internet", Snackbar.LENGTH_SHORT).show();
            }
        });
        if (isOnline(this)) {
            getInvoiceMessage();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }
    public boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }

    private void getInvoiceMessage() {
        mApiClient.getLoystarApi(false)
                .getPaymentMessage()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    if (response.isSuccessful()) {
                        PaymentMessage newPaymentMessage = response.body();
                        message.setText(newPaymentMessage.getMessage());
                    } else {
                        message.setText(null);
                    }
                });
    }

    private void setPaymentMesage() {
        dialog.setTitle("Updating Payment Message");
        dialog.show();
        String paymentMessage = message.getText().toString();
        if (paymentMessage.length() == 0) {
            Snackbar.make(mLayout, "Type in a payment Message", Snackbar.LENGTH_LONG).show();
        } else {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("message", paymentMessage);

                RequestBody requestBody = RequestBody
                        .create(MediaType.parse(
                                "application/json; charset=utf-8"), jsonObject.toString());
                mApiClient.getLoystarApi(false)
                        .setPaymentMessage(requestBody)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(response -> {
                            if (response.isSuccessful()) {
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                                PaymentMessage message = response.body();
                                PaymentMessageEntity paymentMessageEntity = new PaymentMessageEntity();
                                paymentMessageEntity.setId(message.getId());
                                paymentMessageEntity.setMessage(message.getMessage());
                                mDataStore.upsert(paymentMessageEntity).subscribe( result -> {
                                        Toast.makeText(this,
                                                "Updated successfully",
                                                Toast.LENGTH_LONG).show();
                                        Intent merchantIntent = new Intent(this,
                                                SettingsActivity.class);
                                        merchantIntent.addFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(merchantIntent);
                                        }

                                );
                            } else {
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                                Snackbar.make(mLayout,
                                        "Payment Message could not be saved",
                                        Snackbar.LENGTH_LONG).show();
                            }
                        });

            } catch (Exception e) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                Snackbar.make(mLayout, "Payment Message could not be saved", Snackbar.LENGTH_LONG).show();
                Timber.e(e);
            }
        }
    }
}
