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
public interface Subscription extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    int getPricingPlanId();
    int getDuration();
    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();
    Timestamp getExpiresOn();
    String getPlanName();

    @OneToOne(mappedBy = "subscription")
    Merchant getMerchant();
}
