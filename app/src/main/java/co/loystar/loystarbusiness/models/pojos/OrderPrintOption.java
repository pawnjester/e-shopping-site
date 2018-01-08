package co.loystar.loystarbusiness.models.pojos;

/**
 * Created by ordgen on 1/7/18.
 */

public class OrderPrintOption {
    public String id;
    public String title;

    public OrderPrintOption(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
