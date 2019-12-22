package co.loystar.loystarbusiness.models.entities;


import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;

import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Persistable;

@Entity
public interface InvoiceTransaction extends Observable, Parcelable, Persistable {

    @Key
    int getId();

    boolean isSynced();
    int getUserId();
    int getProductId();
    int getMerchantLoyaltyProgramId();
    double getAmount();
    int getStamps();
    int getPoints();
    String getProgramType();
    Timestamp getCreatedAt();

    @Bindable
    @ManyToOne
    MerchantEntity getMerchant();

    @Bindable
    @ManyToOne
    CustomerEntity getCustomer();

    @Bindable
    @ManyToOne
    InvoiceEntity getInvoice();
}
