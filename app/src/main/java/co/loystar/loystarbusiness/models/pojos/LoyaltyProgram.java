package co.loystar.loystarbusiness.models.pojos;

/**
 * Created by ordgen on 11/16/17.
 */

public class LoyaltyProgram {
    public String id;
    public String title;
    public String description;

    public LoyaltyProgram(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
