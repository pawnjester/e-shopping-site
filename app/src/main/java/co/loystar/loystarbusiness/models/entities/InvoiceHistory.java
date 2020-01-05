package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Persistable;

@Entity
public interface InvoiceHistory extends Observable, Parcelable, Persistable {

    @Key
    int getId();

    String getPaidAmount();

    String getPaidAt();

    @Bindable
    @ManyToOne
    InvoiceEntity getInvoice();


}
