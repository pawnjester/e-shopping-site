package co.loystar.loystarbusiness.models.entities;

import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;

import io.requery.Entity;
import io.requery.Key;
import io.requery.OneToOne;
import io.requery.Persistable;

/**
 * Created by ordgen on 11/9/17.
 */

@Entity
public interface BirthdayOffer extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    String getOfferDescription();
    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();

    @OneToOne(mappedBy = "birthdayOffer")
    Merchant getMerchant();
}
