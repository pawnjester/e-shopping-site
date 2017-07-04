package co.loystar.loystarbusiness.models.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.greenrobot.greendao.annotation.*;

/**
 * Entity mapped to table "DBBIRTHDAY_OFFER_PRESET_SMS".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "created_at",
    "updated_at",
    "local_db_created_at",
    "local_db_updated_at",
    "offer_description",
    "preset_sms_text",
    "synced",
    "deleted",
    "merchant_id",
})
@Entity
public class DBBirthdayOfferPresetSMS {

    @Id
    private Long id;
    private java.util.Date created_at;
    private java.util.Date updated_at;
    private java.util.Date local_db_created_at;
    private java.util.Date local_db_updated_at;
    private String preset_sms_text;
    private Integer synced;
    private Long merchant_id;
    private Boolean deleted;

    @Generated(hash = 1415325166)
    public DBBirthdayOfferPresetSMS() {
    }

    public DBBirthdayOfferPresetSMS(Long id) {
        this.id = id;
    }

    @Generated(hash = 459334155)
    public DBBirthdayOfferPresetSMS(Long id, java.util.Date created_at, java.util.Date updated_at, java.util.Date local_db_created_at, java.util.Date local_db_updated_at, String preset_sms_text, Integer synced, Long merchant_id, Boolean deleted) {
        this.id = id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.local_db_created_at = local_db_created_at;
        this.local_db_updated_at = local_db_updated_at;
        this.preset_sms_text = preset_sms_text;
        this.synced = synced;
        this.merchant_id = merchant_id;
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

    @JsonProperty("preset_sms_text")
    public String getPreset_sms_text() {
        return preset_sms_text;
    }

    @JsonProperty("preset_sms_text")
    public void setPreset_sms_text(String preset_sms_text) {
        this.preset_sms_text = preset_sms_text;
    }

    @JsonProperty("synced")
    public Integer getSynced() {
        return synced;
    }

    @JsonProperty("synced")
    public void setSynced(Integer synced) {
        this.synced = synced;
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

}
