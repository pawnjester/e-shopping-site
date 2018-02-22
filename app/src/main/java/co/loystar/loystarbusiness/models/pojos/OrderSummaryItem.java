package co.loystar.loystarbusiness.models.pojos;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ordgen on 2/22/18.
 */

public class OrderSummaryItem implements Parcelable {
    private String name;
    private int count;
    private double price;
    private double total;

    public OrderSummaryItem( String name, int count, double price, double total) {
        this.name = name;
        this.count = count;
        this.price = price;
        this.total= total;
    }

    protected OrderSummaryItem(Parcel in) {
        name = in.readString();
        count = in.readInt();
        price = in.readDouble();
        total = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(count);
        dest.writeDouble(price);
        dest.writeDouble(total);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<OrderSummaryItem> CREATOR = new Creator<OrderSummaryItem>() {
        @Override
        public OrderSummaryItem createFromParcel(Parcel in) {
            return new OrderSummaryItem(in);
        }

        @Override
        public OrderSummaryItem[] newArray(int size) {
            return new OrderSummaryItem[size];
        }
    };

    public double getTotal() {
        return total;
    }

    public double getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }
}
