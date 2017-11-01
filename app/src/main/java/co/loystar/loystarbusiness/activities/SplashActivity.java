package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import co.loystar.loystarbusiness.auth.SessionManager;

/**
 * Created by ordgen on 11/1/17.
 */

public class SplashActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            Thread timer = new Thread(){
                public void run() {
                    try
                    {
                        sleep(200);
                    }catch(InterruptedException e) { e.printStackTrace(); }
                    finally
                    {
                        Intent intent = new Intent(SplashActivity.this, MerchantBackOfficeActivity.class);
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
