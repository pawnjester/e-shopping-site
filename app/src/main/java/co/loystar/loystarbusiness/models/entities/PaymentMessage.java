package co.loystar.loystarbusiness.models.entities;

import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;

import io.requery.Entity;
import io.requery.Key;
import io.requery.Persistable;

@Entity
public interface PaymentMessage extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    String getMessage();

    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();
}
