package co.loystar.loystarbusiness.api.pojos;

/**
 * Created by laudbruce-tagoe on 4/21/17.
 */

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "status",
        "subscription_plan",
        "subscription_expires_on"
})
public class ConfirmMMPaymentResponse {

    @JsonProperty("status")
    private String status;
    @JsonProperty("subscription_plan")
    private String subscriptionPlan;
    @JsonProperty("subscription_expires_on")
    private Date subscriptionExpiresOn;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("subscription_plan")
    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    @JsonProperty("subscription_plan")
    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    @JsonProperty("subscription_expires_on")
    public Date getSubscriptionExpiresOn() {
        return subscriptionExpiresOn;
    }

    @JsonProperty("subscription_expires_on")
    public void setSubscriptionExpiresOn(Date subscriptionExpiresOn) {
        this.subscriptionExpiresOn = subscriptionExpiresOn;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

