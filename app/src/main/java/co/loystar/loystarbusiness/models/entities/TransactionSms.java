package co.loystar.loystarbusiness.models.entities;

import android.databinding.Observable;
import android.os.Parcelable;

import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.Persistable;

/**
 * Created by ordgen on 12/27/17.
 */

@Entity
public interface TransactionSms extends Observable, Parcelable, Persistable {
    @Key
    @Generated
    int getId();

    int getCustomerId();
    int getMerchantId();
    int getLoyaltyProgramId();
}
