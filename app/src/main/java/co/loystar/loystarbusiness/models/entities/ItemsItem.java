package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.util.List;

import io.requery.CascadeAction;
import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.Persistable;

@Entity
public interface ItemsItem extends Observable, Parcelable, Persistable {

    @Key
    int getId();

    String getAmount();

    int getQuantity();

    @ManyToOne
    Product getProduct();

    boolean isSynced();

    @ManyToOne
    InvoiceEntity getInvoice();
}
