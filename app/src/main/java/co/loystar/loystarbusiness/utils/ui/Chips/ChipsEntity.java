package co.loystar.loystarbusiness.utils.ui.Chips;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by laudbruce-tagoe on 3/18/17.
 */

public class ChipsEntity implements Parcelable {
    @DrawableRes
    private Integer drawableResId;

    @Nullable
    private String description;

    @NonNull
    private String name;

    @NonNull
    private Long entityId;

    public Integer getDrawableResId() {
        return drawableResId;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public Long getEntityId() {
        return entityId;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    private ChipsEntity(Builder builder) {
        drawableResId = builder.drawableResId;
        description = builder.description;
        name = builder.name;
        entityId = builder.entityId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private int drawableResId;
        private String description;
        private String name;
        private Long entityId;

        private Builder() {
        }

        @NonNull
        public Builder drawableResId(int drawableResId) {
            this.drawableResId = drawableResId;
            return this;
        }

        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        @NonNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public Builder entityId(@Nullable Long entityId) {
            this.entityId = entityId;
            return this;
        }

        @NonNull
        public ChipsEntity build() {
            return new ChipsEntity(this);
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.drawableResId);
        dest.writeString(this.description);
        dest.writeString(this.name);
        dest.writeLong(this.entityId);
    }

    private ChipsEntity(Parcel in) {
        this.drawableResId = in.readInt();
        this.description = in.readString();
        this.name = in.readString();
        this.entityId = in.readLong();
    }

    public static final Parcelable.Creator<ChipsEntity> CREATOR = new Parcelable.Creator<ChipsEntity>() {
        @Override
        public ChipsEntity createFromParcel(Parcel source) {
            return new ChipsEntity(source);
        }

        @Override
        public ChipsEntity[] newArray(int size) {
            return new ChipsEntity[size];
        }
    };
}
