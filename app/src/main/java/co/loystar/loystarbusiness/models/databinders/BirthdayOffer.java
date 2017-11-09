package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

/**
 * Created by ordgen on 11/9/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BirthdayOffer {

    private int id;
    private int merchant_id;
    private DateTime created_at;
    private DateTime updated_at;
    private String offer_description;

    public BirthdayOffer() {}

    public BirthdayOffer(
            int id,
            int merchant_id,
            DateTime created_at,
            DateTime updated_at,
            String offer_description
    ) {
        this.id = id;
        this.merchant_id = merchant_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.offer_description = offer_description;
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
    public void setMerchant_id(int merchant_id) {
        this.merchant_id = merchant_id;
    }

    @JsonProperty("merchant_id")
    public int getMerchant_id() {
        return merchant_id;
    }

    @JsonProperty("offer_description")
    public String getOffer_description() {
        return offer_description;
    }

    @JsonProperty("offer_description")
    public void setOffer_description(String offer_description) {
        this.offer_description = offer_description;
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
