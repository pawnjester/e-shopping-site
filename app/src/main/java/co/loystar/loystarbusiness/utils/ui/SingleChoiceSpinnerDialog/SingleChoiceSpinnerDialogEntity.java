package co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by laudbruce-tagoe on 3/23/17.
 */

public class SingleChoiceSpinnerDialogEntity implements Parcelable {

    private String text;
    private Long Id;

    public SingleChoiceSpinnerDialogEntity(
            String text,
            Long Id) {
        this.text = text;
        this.Id = Id;
    }

    public String getText() {
        return this.text;
    }

    public Long getId() {
        return this.Id;
    }


    private SingleChoiceSpinnerDialogEntity(Parcel in) {
        this.text = in.readString();
        this.Id = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.getId());
        dest.writeString(this.getText());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SingleChoiceSpinnerDialogEntity> CREATOR = new Creator<SingleChoiceSpinnerDialogEntity>() {
        @Override
        public SingleChoiceSpinnerDialogEntity createFromParcel(Parcel in) {
            return new SingleChoiceSpinnerDialogEntity(in);
        }

        @Override
        public SingleChoiceSpinnerDialogEntity[] newArray(int size) {
            return new SingleChoiceSpinnerDialogEntity[size];
        }
    };
}
