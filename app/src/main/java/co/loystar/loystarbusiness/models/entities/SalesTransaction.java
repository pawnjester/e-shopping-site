package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Persistable;

/**
 * Created by ordgen on 11/10/17.
 */

@Entity
public interface SalesTransaction extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    boolean isSynced();

    int getUserId();
    int getProductId();
    int getMerchantLoyaltyProgramId();
    boolean isSendSms();
    int getAmount();

    int getPoints();
    int getStamps();
    String getProgramType();
    Timestamp getCreatedAt();

    @Bindable
    @ManyToOne
    MerchantEntity getMerchant();

    @Bindable
    @ManyToOne
    CustomerEntity getCustomer();
}
