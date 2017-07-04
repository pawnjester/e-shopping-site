package co.loystar.loystarbusiness.utils;

import java.util.Date;

/**
 * Created by ordgen on 7/4/17.
 */

public class GraphCoordinates {
    private Date x = new Date();
    private int y = 0;

    public GraphCoordinates(Date x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Date getX() {
        return x;
    }

    public void setX(Date x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
