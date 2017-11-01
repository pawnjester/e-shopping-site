package co.loystar.loystarbusiness.auth.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by ordgen on 11/1/17.
 */

public class AuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private LoystarAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new LoystarAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
