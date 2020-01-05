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

@Entity
public  interface Invoice extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();

    String getStatus();
    String getPaidAmount();
    String getPaymentMethod();
    String getDueDate();
    String getPaymentMessage();
    String getSubTotal();
    String getNumber();
    boolean isSynced();
    String getAmount();

    @Bindable
    @ManyToOne
    Merchant getOwner();

    @Bindable
    @ManyToOne
    CustomerEntity getCustomer();


    @Bindable
    @OneToMany(mappedBy = "invoice", cascade = {CascadeAction.SAVE})
    List<ItemsItemEntity> getItems();

    @Bindable
    @OneToMany(mappedBy = "invoice", cascade = {CascadeAction.SAVE})
    List<InvoiceTransactionEntity> getTransactions();

    @OneToMany(mappedBy = "invoice", cascade = {CascadeAction.SAVE})
    List<InvoiceHistoryEntity> getHistory();

}