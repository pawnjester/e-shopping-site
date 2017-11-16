package co.loystar.loystarbusiness.models.entities;

import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;
import java.util.List;

import io.requery.CascadeAction;
import io.requery.Column;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Index;
import io.requery.Key;
import io.requery.OneToMany;
import io.requery.OneToOne;
import io.requery.Persistable;

/**
 * Created by ordgen on 11/1/17.
 */

@Entity
public interface Merchant extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    @Index(value = "email_index")
    String getEmail();

    @Index("name_index")
    String getFirstName();

    String getLastName();

    @Index("contact_number_index")
    @Column(unique = true)
    String getContactNumber();
    void setContactNumber(String contactNumber);

    String getSubscriptionPlan();
    String getBusinessType();
    String getBusinessName();
    String getCurrency();
    Timestamp getSubscriptionExpiresOn();
    boolean isPosTurnedOn();
    boolean isUpdateRequired();

    @ForeignKey
    @OneToOne
    SubscriptionEntity getSubscription();

    @ForeignKey
    @OneToOne
    BirthdayOfferEntity getBirthdayOffer();

    @ForeignKey
    @OneToOne
    BirthdayOfferPresetSmsEntity getBirthdayOfferPresetSms();

    @OneToMany(mappedBy = "owner", cascade = {CascadeAction.SAVE})
    List<CustomerEntity> getCustomers();

    @OneToMany(mappedBy = "owner", cascade = {CascadeAction.SAVE})
    List<ProductEntity> getProducts();

    @OneToMany(mappedBy = "owner", cascade = {CascadeAction.SAVE})
    List<ProductCategoryEntity> getProductCategories();

    @OneToMany(mappedBy = "merchant", cascade = {CascadeAction.SAVE})
    List<SalesTransactionEntity> getSalesTransactions();

    @OneToMany(mappedBy = "owner", cascade = {CascadeAction.SAVE})
    List<LoyaltyProgramEntity> getLoyaltyPrograms();
}
