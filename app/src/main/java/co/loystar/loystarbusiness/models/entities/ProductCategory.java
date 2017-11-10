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
 * Created by ordgen on 11/9/17.
 */

@Entity
public interface ProductCategory extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();
    String getName();
    boolean isDeleted();

    @Bindable
    @ManyToOne
    Merchant getOwner();

    @OneToMany(mappedBy = "category", cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<ProductEntity> getProducts();
}
