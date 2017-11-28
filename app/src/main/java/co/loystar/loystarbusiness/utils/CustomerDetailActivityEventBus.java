package co.loystar.loystarbusiness.utils;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by ordgen on 11/28/17.
 */

public class CustomerDetailActivityEventBus {
    public static final int ACTION_START_SALE = 103;

    private static CustomerDetailActivityEventBus mInstance;

    public static CustomerDetailActivityEventBus getInstance() {
        if (mInstance == null) {
            mInstance = new CustomerDetailActivityEventBus();
        }
        return mInstance;
    }

    private CustomerDetailActivityEventBus() {}

    private PublishSubject<Integer> fragmentEventSubject = PublishSubject.create();

    public Observable<Integer> getFragmentEventObservable() {
        return fragmentEventSubject;
    }

    public void postFragmentAction(Integer actionId) {
        fragmentEventSubject.onNext(actionId);
    }
}
