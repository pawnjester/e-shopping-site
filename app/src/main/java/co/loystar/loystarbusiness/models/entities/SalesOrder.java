package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;
import java.util.List;

import io.requery.CascadeAction;
import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.Persistable;

/**
 * Created by ordgen on 12/27/17.
 */

@Entity
public interface SalesOrder extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    String getStatus();
    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();

    boolean isUpdateRequired();

    double getTotal();

    @Bindable
    @ManyToOne
    MerchantEntity getMerchant();

    @Bindable
    @ManyToOne
    CustomerEntity getCustomer();

    @OneToMany(mappedBy = "salesOrder", cascade = {CascadeAction.SAVE})
    List<OrderItemEntity> getOrderItems();
}
