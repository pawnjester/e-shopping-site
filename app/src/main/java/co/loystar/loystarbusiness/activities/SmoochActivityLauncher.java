package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.smooch.ui.ConversationActivity;

/**
 * Created by ordgen on 1/25/18.
 */

public class SmoochActivityLauncher extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Completable.complete()
            .delay(0, TimeUnit.SECONDS)
            .doOnComplete(() -> {
                ConversationActivity.show(SmoochActivityLauncher.this);
                finish();
            })
            .subscribe();
    }
}
