package co.loystar.loystarbusiness.events;

import android.os.Bundle;

/**
 * Created by laudbruce-tagoe on 4/28/17.
 */

public class RecordDirectSalesActivityFragmentEvent extends BaseFragmentOnFinishEvent{
    public static class OnFinish extends BaseFragmentOnFinishEvent.OnFinish{
        public OnFinish(Bundle extras) {
            super(extras);
        }
    }
}
