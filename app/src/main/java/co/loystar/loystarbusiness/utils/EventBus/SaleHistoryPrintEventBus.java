package co.loystar.loystarbusiness.utils.EventBus;

import android.os.Bundle;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by ordgen on 2/22/18.
 */

public class SaleHistoryPrintEventBus {
    public static final int ACTION_START_PRINT = 105;

    private static SaleHistoryPrintEventBus mInstance;

    public static SaleHistoryPrintEventBus getInstance() {
        if (mInstance == null) {
            mInstance = new SaleHistoryPrintEventBus();
        }
        return mInstance;
    }

    private SaleHistoryPrintEventBus() {}

    private PublishSubject<Bundle> fragmentEventSubject = PublishSubject.create();

    public Observable<Bundle> getFragmentEventObservable() {
        return fragmentEventSubject;
    }

    public void postFragmentAction(Bundle data) {
        fragmentEventSubject.onNext(data);
    }
}
