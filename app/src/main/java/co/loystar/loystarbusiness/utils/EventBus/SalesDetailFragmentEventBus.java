package co.loystar.loystarbusiness.utils.EventBus;

import android.os.Bundle;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by ordgen on 2/22/18.
 */

public class SalesDetailFragmentEventBus {
    public static final int ACTION_START_SALE = 104;

    private static SalesDetailFragmentEventBus mInstance;

    public static SalesDetailFragmentEventBus getInstance() {
        if (mInstance == null) {
            mInstance = new SalesDetailFragmentEventBus();
        }
        return mInstance;
    }

    private SalesDetailFragmentEventBus() {}

    private PublishSubject<Bundle> fragmentEventSubject = PublishSubject.create();

    public Observable<Bundle> getFragmentEventObservable() {
        return fragmentEventSubject;
    }

    public void postFragmentAction(Bundle data) {
        fragmentEventSubject.onNext(data);
    }
}
