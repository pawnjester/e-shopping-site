package co.loystar.loystarbusiness.events;

import android.os.Bundle;

/**
 * Created by laudbruce-tagoe on 4/29/17.
 */


public class AddPointsOnFinishEvent extends BaseFragmentOnFinishEvent {
    public static class OnFinish extends BaseFragmentOnFinishEvent.OnFinish {

        public OnFinish(Bundle extras) {
            super(extras);
        }
    }
}
