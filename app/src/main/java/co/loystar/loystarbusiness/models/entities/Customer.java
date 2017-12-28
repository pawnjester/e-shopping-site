package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import io.requery.CascadeAction;
import io.requery.Column;
import io.requery.Entity;
import io.requery.Index;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.Persistable;

/**
 * Created by ordgen on 11/9/17.
 */

@Entity
public interface Customer extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    String getEmail();
    String getFirstName();
    String getLastName();

    @Index("phone_number_index")
    @Column(unique = true)
    String getPhoneNumber();

    String getSex();
    Date getDateOfBirth();
    int getUserId();
    boolean isDeleted();
    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();

    @Bindable
    @ManyToOne
    Merchant getOwner();

    @OneToMany(mappedBy = "customer", cascade = {CascadeAction.SAVE})
    List<SalesTransactionEntity> getSalesTransactions();

    @OneToMany(mappedBy = "customer", cascade = {CascadeAction.SAVE})
    List<SalesOrderEntity> getSalesOrders();
}
