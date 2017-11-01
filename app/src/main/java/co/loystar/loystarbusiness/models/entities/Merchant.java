package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.util.Date;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Index;
import io.requery.Key;
import io.requery.Persistable;

/**
 * Created by ordgen on 11/1/17.
 */

@Entity
public interface Merchant extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    @Index(value = "email_index")
    String getEmail();

    @Bindable
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
    Date getCreatedAt();
    Date getUpdatedAt();
    Date getSubscriptionExpiresOn();
}
