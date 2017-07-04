package co.loystar.loystarbusiness.models.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.greenrobot.greendao.annotation.*;


/**
 * Entity mapped to table "DBMERCHANT_LOYALTY_PROGRAM".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "threshold",
    "reward",
    "name",
    "created_at",
    "updated_at",
    "program_type",
    "merchant_id",
    "deleted",
    "sms_template"
})
@Entity
public class DBMerchantLoyaltyProgram {

    @Id
    private Long id;
    private Integer threshold;
    private String reward;
    private String name;
    private java.util.Date created_at;
    private java.util.Date updated_at;
    private String program_type;
    private Long merchant_id;
    private Boolean deleted;
    private String sms_template;

    @Generated(hash = 1642308130)
    public DBMerchantLoyaltyProgram() {
    }

    public DBMerchantLoyaltyProgram(Long id) {
        this.id = id;
    }

    @Generated(hash = 56233952)
    public DBMerchantLoyaltyProgram(Long id, Integer threshold, String reward, String name, java.util.Date created_at, java.util.Date updated_at, String program_type, Long merchant_id, Boolean deleted, String sms_template) {
        this.id = id;
        this.threshold = threshold;
        this.reward = reward;
        this.name = name;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.program_type = program_type;
        this.merchant_id = merchant_id;
        this.deleted = deleted;
        this.sms_template = sms_template;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("threshold")
    public Integer getThreshold() {
        return threshold;
    }

    @JsonProperty("threshold")
    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    @JsonProperty("reward")
    public String getReward() {
        return reward;
    }

    @JsonProperty("reward")
    public void setReward(String reward) {
        this.reward = reward;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
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

    @JsonProperty("program_type")
    public String getProgram_type() {
        return program_type;
    }

    @JsonProperty("program_type")
    public void setProgram_type(String program_type) {
        this.program_type = program_type;
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

    @JsonProperty("sms_template")
    public String getSms_template() {
        return sms_template;
    }

    @JsonProperty("sms_template")
    public void setSms_template(String sms_template) {
        this.sms_template = sms_template;
    }

}
