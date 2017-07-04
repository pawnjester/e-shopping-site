package co.loystar.loystarbusiness.events;

/**
 * Created by laudbruce-tagoe on 4/15/17.
 */

public class BaseAdapterItemClickEvent {
    static class OnClicked {
        private int adapterPosition;
        private String action;

        public OnClicked(int adapterPosition) {
            this.adapterPosition = adapterPosition;
        }

        public OnClicked(int adapterPosition, String action) {
            this.adapterPosition = adapterPosition;
            this.action = action;
        }

        public int getAdapterPosition() {
            return adapterPosition;
        }

        public String getAction() {
            return action;
        }
    }
}
