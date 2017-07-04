package co.loystar.loystarbusiness.models.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.greenrobot.greendao.annotation.*;

/**
 * Entity mapped to table "DBBIRTHDAY_OFFER".
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "merchant_id",
        "created_at",
        "updated_at",
        "local_db_created_at",
        "local_db_updated_at",
        "offer_description",
        "synced",
        "deleted"
})
@Entity
public class DBBirthdayOffer {

    @Id
    private Long id;
    private Long merchant_id;
    private java.util.Date created_at;
    private java.util.Date updated_at;
    private java.util.Date local_db_created_at;
    private java.util.Date local_db_updated_at;
    private String offer_description;
    private Integer synced;
    private Boolean deleted;

    @Generated(hash = 1732336723)
    public DBBirthdayOffer() {
    }

    public DBBirthdayOffer(Long id) {
        this.id = id;
    }

    @Generated(hash = 1888459879)
    public DBBirthdayOffer(Long id, Long merchant_id, java.util.Date created_at, java.util.Date updated_at, java.util.Date local_db_created_at, java.util.Date local_db_updated_at, String offer_description, Integer synced, Boolean deleted) {
        this.id = id;
        this.merchant_id = merchant_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.local_db_created_at = local_db_created_at;
        this.local_db_updated_at = local_db_updated_at;
        this.offer_description = offer_description;
        this.synced = synced;
        this.deleted = deleted;
    }


    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("merchant_id")
    public Long getMerchant_id() {
        return merchant_id;
    }

    @JsonProperty("merchant_id")
    public void setMerchant_id(Long merchant_id) {
        this.merchant_id = merchant_id;
    }

    @JsonProperty("created_at")
    public java.util.Date getCreated_at() {
        return created_at;
    }

    @JsonProperty("created_at")
    public void setCreated_at(java.util.Date created_at) {
        this.created_at = created_at;
    }

    @JsonProperty("updated_at")
    public java.util.Date getUpdated_at() {
        return updated_at;
    }

    @JsonProperty("updated_at")
    public void setUpdated_at(java.util.Date updated_at) {
        this.updated_at = updated_at;
    }

    @JsonProperty("local_db_created_at")
    public java.util.Date getLocal_db_created_at() {
        return local_db_created_at;
    }

    @JsonProperty("local_db_created_at")
    public void setLocal_db_created_at(java.util.Date local_db_created_at) {
        this.local_db_created_at = local_db_created_at;
    }

    @JsonProperty("local_db_updated_at")
    public java.util.Date getLocal_db_updated_at() {
        return local_db_updated_at;
    }

    @JsonProperty("local_db_updated_at")
    public void setLocal_db_updated_at(java.util.Date local_db_updated_at) {
        this.local_db_updated_at = local_db_updated_at;
    }

    @JsonProperty("offer_description")
    public String getOffer_description() {
        return offer_description;
    }

    @JsonProperty("offer_description")
    public void setOffer_description(String offer_description) {
        this.offer_description = offer_description;
    }

    @JsonProperty("synced")
    public Integer getSynced() {
        return synced;
    }

    @JsonProperty("synced")
    public void setSynced(Integer synced) {
        this.synced = synced;
    }

    @JsonProperty("deleted")
    public Boolean getDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

}
