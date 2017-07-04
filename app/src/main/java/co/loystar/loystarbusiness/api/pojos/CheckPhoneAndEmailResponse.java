package co.loystar.loystarbusiness.api.pojos;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by laudbruce-tagoe on 4/11/17.
 */


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "phone_available",
        "email_available"
})
public class CheckPhoneAndEmailResponse {

    @JsonProperty("phone_available")
    private Boolean phoneAvailable;
    @JsonProperty("email_available")
    private Boolean emailAvailable;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("phone_available")
    public Boolean getPhoneAvailable() {
        return phoneAvailable;
    }

    @JsonProperty("phone_available")
    public void setPhoneAvailable(Boolean phoneAvailable) {
        this.phoneAvailable = phoneAvailable;
    }

    @JsonProperty("email_available")
    public Boolean getEmailAvailable() {
        return emailAvailable;
    }

    @JsonProperty("email_available")
    public void setEmailAvailable(Boolean emailAvailable) {
        this.emailAvailable = emailAvailable;
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
