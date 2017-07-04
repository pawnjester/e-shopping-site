package co.loystar.loystarbusiness.events;

/**
 * Created by laudbruce-tagoe on 4/15/17.
 */

public class LoyaltyProgramsAdapterItemClickEvent extends BaseAdapterItemClickEvent {

    public static class OnItemClicked extends OnClicked {
        public OnItemClicked(int adapterPosition) {
            super(adapterPosition);
        }
    }
}
