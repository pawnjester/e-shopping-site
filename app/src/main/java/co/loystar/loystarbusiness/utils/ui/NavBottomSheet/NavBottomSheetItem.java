package co.loystar.loystarbusiness.utils.ui.NavBottomSheet;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by laudbruce-tagoe on 3/12/17.
 */

class NavBottomSheetItem implements Parcelable {
    private int image;
    private String text;

    NavBottomSheetItem(int image, String text){
        this.image = image;
        this.text = text;
    }

    public int getImage(){
        return( this.image);
    }

    public String getText() {
        return text;
    }

    private NavBottomSheetItem(Parcel in) {
        this.image = in.readInt();
        this.text = in.readString();
    }

    public static final Creator<NavBottomSheetItem> CREATOR = new Creator<NavBottomSheetItem>() {
        @Override
        public NavBottomSheetItem createFromParcel(Parcel in) {
            return new NavBottomSheetItem(in);
        }

        @Override
        public NavBottomSheetItem[] newArray(int size) {
            return new NavBottomSheetItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.image);
        parcel.writeString(this.text);
    }
}
