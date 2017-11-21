package co.loystar.loystarbusiness.models.pojos;

/**
 * Created by ordgen on 11/20/17.
 */

public class BusinessType {
    public int id;
    public String tag;
    public String title;

    public BusinessType(int id, String tag, String title) {
        this.id = id;
        this.title = title;
        this.tag = tag;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(int id) {
        this.id = id;
    }
}
