package co.loystar.loystarbusiness.events;

/**
 * Created by laudbruce-tagoe on 4/26/17.
 */

public class LoyaltyProgramsOverflowItemClickEvent extends BaseAdapterItemClickEvent {
    public static class OnItemClicked extends OnClicked {
        public OnItemClicked(int adapterPosition, String action) {
            super(adapterPosition, action);
        }
    }
}
