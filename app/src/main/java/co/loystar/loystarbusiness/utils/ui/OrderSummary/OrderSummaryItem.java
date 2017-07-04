package co.loystar.loystarbusiness.utils.ui.OrderSummary;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by laudbruce-tagoe on 3/25/17.
 */

public class OrderSummaryItem implements Parcelable {

    private String itemName;
    private double itemPrice;
    private int itemCount;
    private Long itemId;

    private OrderSummaryItem(Parcel in) {
        itemName = in.readString();
        itemPrice = in.readDouble();
        itemCount = in.readInt();
        itemId = in.readLong();
    }

    public OrderSummaryItem(String name, Double price, int count, Long id) {
        this.itemName = name;
        this.itemPrice = price;
        this.itemCount = count;
        this.itemId = id;
    }


    public Long getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(itemName);
        parcel.writeDouble(itemPrice);
        parcel.writeInt(itemCount);
        parcel.writeLong(itemId);
    }
}
