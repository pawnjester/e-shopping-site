package co.loystar.loystarbusiness.models.entities;

import java.sql.Timestamp;

import io.requery.Entity;
import io.requery.Key;

@Entity
public interface Staff {
    @Key
    int getId();

    String getUsername();

    String getEmail();
    String getRole();
    String getBusinessType();
    String getBusinessName();
    String getAddressLine1();
    String getAddressLine2();

    Timestamp getSubscriptionExpiresOn();

    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();
}
