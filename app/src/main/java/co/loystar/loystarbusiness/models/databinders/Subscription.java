package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.joda.time.DateTime;

/**
 * Created by ordgen on 11/9/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "pricing_plan_id",
        "merchant_id",
        "created_at",
        "updated_at",
        "duration",
        "expires_on",
        "plan_name"
})
public class Subscription {
    private int id;
    private int pricing_plan_id;
    private int merchant_id;
    private DateTime created_at;
    private DateTime updated_at;
    private DateTime expires_on;
    private String plan_name;
    private int duration;

    public Subscription() {}

    public Subscription(
            int id,
            int pricing_plan_id,
            int merchant_id,
            DateTime expires_on,
            DateTime created_at,
            DateTime updated_at,
            String plan_name,
            int duration
    ) {
        this.id = id;
        this.pricing_plan_id = pricing_plan_id;
        this.merchant_id = merchant_id;
        this.expires_on = expires_on;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.plan_name = plan_name;
        this.duration = duration;
    }

    @JsonProperty("id")
    public int getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(int id) {
        this.id = id;
    }

    @JsonProperty("merchant_id")
    public int getMerchant_id() {
        return merchant_id;
    }

    @JsonProperty("merchant_id")
    public void setMerchant_id(int merchant_id) {
        this.merchant_id = merchant_id;
    }

    @JsonProperty("pricing_plan_id")
    public int getPricing_plan_id() {
        return pricing_plan_id;
    }

    @JsonProperty("pricing_plan_id")
    public void setPricing_plan_id(int pricing_plan_id) {
        this.pricing_plan_id = pricing_plan_id;
    }

    @JsonProperty("duration")
    public int getDuration() {
        return duration;
    }

    @JsonProperty("duration")
    public void setDuration(int duration) {
        this.duration = duration;
    }

    @JsonProperty("expires_on")
    public DateTime getExpires_on() {
        return expires_on;
    }

    @JsonProperty("expires_on")
    public void setExpires_on(DateTime expires_on) {
        this.expires_on = expires_on;
    }

    @JsonProperty("plan_name")
    public String getPlan_name() {
        return plan_name;
    }

    @JsonProperty("plan_name")
    public void setPlan_name(String plan_name) {
        this.plan_name = plan_name;
    }

    @JsonProperty("created_at")
    public DateTime getCreated_at() {
        return created_at;
    }

    @JsonProperty("created_at")
    public void setCreated_at(DateTime created_at) {
        this.created_at = created_at;
    }

    @JsonProperty("updated_at")
    public DateTime getUpdated_at() {
        return updated_at;
    }

    @JsonProperty("updated_at")
    public void setUpdated_at(DateTime updated_at) {
        this.updated_at = updated_at;
    }
}
