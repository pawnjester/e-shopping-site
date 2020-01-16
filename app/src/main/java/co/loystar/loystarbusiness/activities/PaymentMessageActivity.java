package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import timber.log.Timber;

public class PaymentMessageActivity extends AppCompatActivity {

    EditText message;

    Button setMessageButton;
    private View mLayout;
    private ApiClient mApiClient;




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
        mLayout = findViewById(R.id.message_container);
        mApiClient = new ApiClient(this);
        message = findViewById(R.id.setPaymentMessage);

        setMessageButton = findViewById(R.id.setMessageButton);

        setMessageButton.setOnClickListener(view -> {
            setPaymentMesage();
        });
    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setPaymentMesage() {
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
                                Toast.makeText(this,
                                        "Payment message has been saved successfully",
                                        Toast.LENGTH_LONG).show();
                                Intent merchantIntent = new Intent(this,
                                        MerchantBackOfficeActivity.class);
                                merchantIntent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(merchantIntent);
                            } else {
                                Snackbar.make(mLayout,
                                        "Payment Message could not be saved",
                                        Snackbar.LENGTH_LONG).show();
                            }
                        });

            } catch (Exception e) {
                Snackbar.make(mLayout, "Payment Message could not be saved", Snackbar.LENGTH_LONG).show();
                Timber.e(e);
            }
        }
    }
}
