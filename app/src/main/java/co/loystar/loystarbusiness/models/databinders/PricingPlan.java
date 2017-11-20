package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ordgen on 11/20/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "price",
        "currency",
        "sms_allowed",
        "subscription_duration_list"
})
public class PricingPlan {
    @JsonProperty("price")
    private String price;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("sms_allowed")
    private String smsAllowed;
    @JsonProperty("subscription_duration_list")
    private String[] subscriptionDurationList = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("price")
    public String getPrice() {
        return price;
    }

    @JsonProperty("price")
    public void setPrice(String price) {
        this.price = price;
    }

    @JsonProperty("currency")
    public String getCurrency() {
        return currency;
    }

    @JsonProperty("currency")
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @JsonProperty("sms_allowed")
    public String getSmsAllowed() {
        return smsAllowed;
    }

    @JsonProperty("sms_allowed")
    public void setSmsAllowed(String smsAllowed) {
        this.smsAllowed = smsAllowed;
    }

    @JsonProperty("subscription_duration_list")
    public String[] getSubscriptionDurationList() {
        return subscriptionDurationList;
    }

    @JsonProperty("subscription_duration_list")
    public void setSubscriptionDurationList(String[] subscriptionDurationList) {
        this.subscriptionDurationList = subscriptionDurationList;
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
