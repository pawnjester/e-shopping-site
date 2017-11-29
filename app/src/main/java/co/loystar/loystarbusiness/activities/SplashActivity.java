package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.utils.Constants;
import io.reactivex.Completable;

/**
 * Created by ordgen on 11/1/17.
 */

public class SplashActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            Completable.complete()
                    .delay(1, TimeUnit.SECONDS)
                    .doOnComplete(() -> {
                        Intent intent = new Intent(SplashActivity.this, MerchantBackOfficeActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .subscribe();
        }
        else {
            if (getIntent().getBooleanExtra(Constants.SKIP_INTRO, false)) {
                AccountManager.get(this).addAccount(
                        AccountGeneral.ACCOUNT_TYPE,
                        AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS,
                        null,
                        null,
                        SplashActivity.this,
                        accountManagerFuture -> finish(),
                        null
                );
            } else {
                Intent intent = new Intent(SplashActivity.this, AppIntro.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
