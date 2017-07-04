package co.loystar.loystarbusiness.events;

/**
 * Created by laudbruce-tagoe on 4/28/17.
 */

public class SelectProgramAdapterItemClickEvent extends BaseAdapterItemClickEvent{
    public static class OnItemClicked extends OnClicked {

        public OnItemClicked(int adapterPosition) {
            super(adapterPosition);
        }
    }
}
