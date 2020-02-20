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
public interface Product extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();
    double getPrice();
    String getName();
    String getPicture();
    String getDescription();
    boolean isDeleted();
    String getUnit();
    String getQuantity();
    boolean isTracked();

    @Bindable
    @ManyToOne
    Merchant getOwner();

    @Bindable
    @ManyToOne
    ProductCategoryEntity getCategory();

    @OneToMany(mappedBy = "product", cascade = {CascadeAction.SAVE})
    List<OrderItemEntity> getOrderItems();

    @Bindable
    @ManyToOne
    LoyaltyProgramEntity getLoyaltyProgram();

    @OneToMany(mappedBy = "product", cascade = {CascadeAction.SAVE})
    List<ItemsItemEntity>  getItems();
}
