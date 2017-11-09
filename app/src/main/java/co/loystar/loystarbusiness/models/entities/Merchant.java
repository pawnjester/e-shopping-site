package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;

import io.requery.Column;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Index;
import io.requery.Key;
import io.requery.OneToOne;
import io.requery.Persistable;

/**
 * Created by ordgen on 11/1/17.
 */

@Entity
public interface Merchant extends Parcelable, Persistable {
    @Key
    int getId();

    @Index(value = "email_index")
    String getEmail();

    @Index("name_index")
    String getFirstName();

    String getLastName();

    @Index("phone_index")
    @Column(unique = true)
    String getContactNumber();
    void setContactNumber(String contactNumber);

    String getSubscriptionPlan();
    String getBusinessType();
    String getBusinessName();
    String getCurrency();
    Timestamp getSubscriptionExpiresOn();

    @ForeignKey
    @OneToOne
    SubscriptionEntity getSubscription();

    @ForeignKey
    @OneToOne
    BirthdayOfferEntity getBirthdayOffer();

    @ForeignKey
    @OneToOne
    BirthdayOfferPresetSmsEntity getBirthdayOfferPresetSms();
}
