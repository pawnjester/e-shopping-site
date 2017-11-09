package co.loystar.loystarbusiness.models.entities;

import android.os.Parcelable;

import java.util.Date;

import io.requery.Entity;
import io.requery.Key;
import io.requery.OneToOne;
import io.requery.Persistable;

/**
 * Created by ordgen on 11/9/17.
 */

@Entity
public interface BirthdayOffer extends Parcelable, Persistable {
    @Key
    int getId();

    String getOfferDescription();
    Date getCreatedAt();
    Date getUpdatedAt();

    @OneToOne(mappedBy = "birthdayOffer")
    Merchant getMerchant();
}
