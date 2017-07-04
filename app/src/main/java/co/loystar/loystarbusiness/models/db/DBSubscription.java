package co.loystar.loystarbusiness.models.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.greenrobot.greendao.annotation.*;


/**
 * Entity mapped to table "DBSUBSCRIPTION".
 */
@JsonPropertyOrder({
        "id",
        "plan_name",
        "expires_on",
        "created_at",
        "updated_at",
        "pricing_plan_id",
        "mm_payment_token",
        "mm_invoice_number",
        "mm_payment_duration",
        "merchant_id"
})
@Entity
public class DBSubscription {

    @Id
    private Long id;
    private String plan_name;
    private java.util.Date expires_on;
    private java.util.Date created_at;
    private java.util.Date updated_at;
    private Long pricing_plan_id;
    private String mm_payment_token;
    private String mm_invoice_number;
    private Integer mm_payment_duration;
    private Long merchant_id;

    @Generated(hash = 1574116625)
    public DBSubscription() {
    }

    public DBSubscription(Long id) {
        this.id = id;
    }

    @Generated(hash = 283675111)
    public DBSubscription(Long id, String plan_name, java.util.Date expires_on, java.util.Date created_at, java.util.Date updated_at, Long pricing_plan_id, String mm_payment_token, String mm_invoice_number, Integer mm_payment_duration, Long merchant_id) {
        this.id = id;
        this.plan_name = plan_name;
        this.expires_on = expires_on;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.pricing_plan_id = pricing_plan_id;
        this.mm_payment_token = mm_payment_token;
        this.mm_invoice_number = mm_invoice_number;
        this.mm_payment_duration = mm_payment_duration;
        this.merchant_id = merchant_id;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("plan_name")
    public String getPlan_name() {
        return plan_name;
    }

    @JsonProperty("plan_name")
    public void setPlan_name(String plan_name) {
        this.plan_name = plan_name;
    }

    @JsonProperty("expires_on")
    public java.util.Date getExpires_on() {
        return expires_on;
    }

    @JsonProperty("expires_on")
    public void setExpires_on(java.util.Date expires_on) {
        this.expires_on = expires_on;
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

    @JsonProperty("pricing_plan_id")
    public Long getPricing_plan_id() {
        return pricing_plan_id;
    }

    @JsonProperty("pricing_plan_id")
    public void setPricing_plan_id(Long pricing_plan_id) {
        this.pricing_plan_id = pricing_plan_id;
    }

    @JsonProperty("mm_payment_token")
    public String getMm_payment_token() {
        return mm_payment_token;
    }

    @JsonProperty("mm_payment_token")
    public void setMm_payment_token(String mm_payment_token) {
        this.mm_payment_token = mm_payment_token;
    }

    @JsonProperty("mm_invoice_number")
    public String getMm_invoice_number() {
        return mm_invoice_number;
    }

    @JsonProperty("mm_invoice_number")
    public void setMm_invoice_number(String mm_invoice_number) {
        this.mm_invoice_number = mm_invoice_number;
    }

    @JsonProperty("mm_payment_duration")
    public Integer getMm_payment_duration() {
        return mm_payment_duration;
    }

    @JsonProperty("mm_payment_duration")
    public void setMm_payment_duration(Integer mm_payment_duration) {
        this.mm_payment_duration = mm_payment_duration;
    }

    @JsonProperty("merchant_id")
    public Long getMerchant_id() {
        return merchant_id;
    }

    @JsonProperty("merchant_id")
    public void setMerchant_id(Long merchant_id) {
        this.merchant_id = merchant_id;
    }

}
