package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;
import java.util.List;

import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.Persistable;

/**
 * Created by ordgen on 2/6/18.
 */

@Entity
public interface Sale extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    double getTotal();
    boolean isPayedWithCash();
    boolean isPayedWithCard();
    boolean isPayedWithMobile();

    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();

    @Bindable
    @ManyToOne
    MerchantEntity getMerchant();

    @Bindable
    @ManyToOne
    CustomerEntity getCustomer();

    @OneToMany(mappedBy = "sale")
    List<SalesTransactionEntity> getTransactions();
}
