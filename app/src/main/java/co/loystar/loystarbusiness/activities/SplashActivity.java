package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = LoystarApplication.getInstance().getSessionManager();
        if (sessionManager.isLoggedIn()) {
            Thread timer = new Thread(){
                public void run() {
                    try
                    {
                        sleep(200);
                    }catch(InterruptedException e) { e.printStackTrace(); }
                    finally
                    {
                        Intent intent = new Intent(SplashActivity.this, MerchantBackOffice.class);
                        startActivity(intent);
                        finish();
                    }
                }
            };
            timer.start();
        }
        else {
            Intent intent = new Intent(SplashActivity.this, AppIntro.class);
            startActivity(intent);
            finish();
        }
    }
}
