package co.loystar.loystarbusiness.events;

import android.os.Bundle;

/**
 * Created by laudbruce-tagoe on 4/28/17.
 */

public class BaseFragmentOnFinishEvent {
    public static class OnFinish {
        private Bundle extras;

        public OnFinish(Bundle extras) {
            this.extras = extras;
        }

        public Bundle getExtras() {
            return extras;
        }
    }
}
