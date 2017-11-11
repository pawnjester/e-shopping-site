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
        "created_at",
        "updated_at",
        "merchant_id",
        "deleted",
        "name"
})
public class ProductCategory {
    private int id;
    private DateTime created_at;
    private DateTime updated_at;
    private int merchant_id;
    private Boolean deleted;
    private String name;

    public ProductCategory() {}

    public ProductCategory(
            int id,
            int merchant_id,
            DateTime created_at,
            DateTime updated_at,
            boolean deleted,
            String name
    ) {
        this.id  = id;
        this.merchant_id = merchant_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.deleted = deleted;
        this.name = name;
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

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("deleted")
    public Boolean isDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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
