package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.joda.time.DateTime;

/**
 * Created by ordgen on 11/10/17.
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
})
public class LoyaltyProgram {
    private int id;
    private int threshold;
    private String reward;
    private String name;
    private DateTime created_at;
    private DateTime updated_at;
    private String program_type;
    private int merchant_id;
    private boolean deleted;

    public LoyaltyProgram() {}

    public LoyaltyProgram(
            int id,
            int merchant_id,
            DateTime created_at,
            DateTime updated_at,
            boolean deleted,
            String name,
            int threshold,
            String reward,
            String program_type
    ) {
        this.id  = id;
        this.merchant_id = merchant_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.deleted = deleted;
        this.name = name;
        this.threshold = threshold;
        this.program_type = program_type;
        this.reward = reward;
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

    @JsonProperty("program_type")
    public String getProgram_type() {
        return program_type;
    }

    @JsonProperty("program_type")
    public void setProgram_type(String program_type) {
        this.program_type = program_type;
    }

    @JsonProperty("threshold")
    public int getThreshold() {
        return threshold;
    }

    @JsonProperty("threshold")
    public void setThreshold(int threshold) {
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

    @JsonProperty("deleted")
    public boolean isDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(boolean deleted) {
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
