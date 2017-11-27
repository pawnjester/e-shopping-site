package co.loystar.loystarbusiness.models.pojos;

/**
 * Created by ordgen on 11/27/17.
 */

public class StampItem {
    private boolean isStamped;

    public StampItem(boolean isStamped) {
        this.isStamped = isStamped;
    }

    public boolean isStamped() {
        return isStamped;
    }

    public void setStamped(boolean stamped) {
        isStamped = stamped;
    }
}
