package co.loystar.loystarbusiness.utils.ui.PosProducts;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by laudbruce-tagoe on 5/24/17.
 */

public class PosProductEntity implements Parcelable{
    private String name;
    private Long id;
    private String picture;
    private int count;
    private double price;

    PosProductEntity(Long id, String name, String picture, int count, double price) {
        this.id = id;
        this.name = name;
        this.picture = picture;
        this.count = count;
        this.price = price;
    }

    private PosProductEntity(Parcel in) {
        name = in.readString();
        id = in.readLong();
        price = in.readDouble();
        count = in.readInt();
        picture = in.readString();
    }

    public double getPrice() {
        return price;
    }

    public int getCount() {
        return count;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPicture() {
        return picture;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public static final Creator<PosProductEntity> CREATOR = new Creator<PosProductEntity>() {
        @Override
        public PosProductEntity createFromParcel(Parcel in) {
            return new PosProductEntity(in);
        }

        @Override
        public PosProductEntity[] newArray(int size) {
            return new PosProductEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(price);
        parcel.writeLong(id);
        parcel.writeString(picture);
        parcel.writeString(name);
        parcel.writeInt(count);
    }
}
