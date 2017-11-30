package co.loystar.loystarbusiness.utils.EventBus;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by ordgen on 11/28/17.
 */

public class CustomerDetailFragmentEventBus {
    public static final int ACTION_START_SALE = 103;

    private static CustomerDetailFragmentEventBus mInstance;

    public static CustomerDetailFragmentEventBus getInstance() {
        if (mInstance == null) {
            mInstance = new CustomerDetailFragmentEventBus();
        }
        return mInstance;
    }

    private CustomerDetailFragmentEventBus() {}

    private PublishSubject<Integer> fragmentEventSubject = PublishSubject.create();

    public Observable<Integer> getFragmentEventObservable() {
        return fragmentEventSubject;
    }

    public void postFragmentAction(Integer actionId) {
        fragmentEventSubject.onNext(actionId);
    }
}
