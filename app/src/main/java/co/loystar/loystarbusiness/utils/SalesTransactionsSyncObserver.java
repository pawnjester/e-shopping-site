package co.loystar.loystarbusiness.utils;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by ordgen on 11/23/17.
 */

public class SalesTransactionsSyncObserver {
    private PublishSubject<Boolean> subject = PublishSubject.create();

    public void setSyncFinished(Boolean syncFinished) {
        subject.onNext(syncFinished);
    }

    public Observable<Boolean> getSynced() {
        return subject;
    }
}
