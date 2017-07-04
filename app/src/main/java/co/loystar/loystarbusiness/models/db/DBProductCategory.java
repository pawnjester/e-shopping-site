package co.loystar.loystarbusiness.models.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.greenrobot.greendao.annotation.*;


/**
 * Entity mapped to table "DBPRODUCT_CATEGORY".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "created_at",
        "updated_at",
        "merchant_id",
        "deleted",
        "name"
})
@Entity
public class DBProductCategory {

    @Id
    private Long id;
    private java.util.Date created_at;
    private java.util.Date updated_at;
    private Long merchant_id;
    private Boolean deleted;
    private String name;

    @Generated(hash = 1359958420)
    public DBProductCategory() {
    }

    public DBProductCategory(Long id) {
        this.id = id;
    }

    @Generated(hash = 1491969491)
    public DBProductCategory(Long id, java.util.Date created_at, java.util.Date updated_at, Long merchant_id, Boolean deleted, String name) {
        this.id = id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.merchant_id = merchant_id;
        this.deleted = deleted;
        this.name = name;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
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

    @JsonProperty("merchant_id")
    public Long getMerchant_id() {
        return merchant_id;
    }

    @JsonProperty("merchant_id")
    public void setMerchant_id(Long merchant_id) {
        this.merchant_id = merchant_id;
    }

    @JsonProperty("deleted")
    public Boolean getDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

}
