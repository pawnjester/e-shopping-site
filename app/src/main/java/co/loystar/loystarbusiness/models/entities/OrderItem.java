package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;

import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Persistable;

/**
 * Created by ordgen on 12/27/17.
 */

@Entity
public interface OrderItem extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    double getUnitPrice();
    int getQuantity();
    double getTotalPrice();

    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();

    @Bindable
    @ManyToOne
    ProductEntity getProduct();

    @Bindable
    @ManyToOne
    SalesOrderEntity getSalesOrder();
}
