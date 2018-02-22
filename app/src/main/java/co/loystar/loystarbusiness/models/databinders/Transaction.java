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
        "user_id",
        "product_id",
        "merchant_loyalty_program_id",
        "amount",
        "points",
        "stamps",
        "created_at",
        "updated_at",
        "program_type",
        "customer_id",
        "merchant_id"
})
public class Transaction {
    private int id;
    private int user_id;
    private int product_id;
    private int merchant_loyalty_program_id;
    private double amount;
    private int points;
    private int stamps;
    private boolean synced;
    private DateTime created_at;
    private String program_type;
    private int merchant_id;
    private int customer_id;

    public Transaction() {}

    public Transaction(
            int id,
            int merchant_id,
            int user_id,
            int product_id,
            int merchant_loyalty_program_id,
            double amount,
            int points,
            int stamps,
            DateTime created_at,
            boolean synced,
            String program_type,
            int customer_id
    ) {
        this.id  = id;
        this.merchant_id = merchant_id;
        this.created_at = created_at;
        this.user_id = user_id;
        this.product_id = product_id;
        this.amount = amount;
        this.points = points;
        this.stamps = stamps;
        this.merchant_loyalty_program_id = merchant_loyalty_program_id;
        this.synced = synced;
        this.program_type = program_type;
        this.customer_id = customer_id;
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

    @JsonProperty("user_id")
    public int getUser_id() {
        return user_id;
    }

    @JsonProperty("user_id")
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    @JsonProperty("amount")
    public double getAmount() {
        return amount;
    }

    @JsonProperty("amount")
    public void setAmount(int amount) {
        this.amount = amount;
    }

    @JsonProperty("merchant_loyalty_program_id")
    public int getMerchant_loyalty_program_id() {
        return merchant_loyalty_program_id;
    }

    @JsonProperty("merchant_loyalty_program_id")
    public void setMerchant_loyalty_program_id(int merchant_loyalty_program_id) {
        this.merchant_loyalty_program_id = merchant_loyalty_program_id;
    }

    @JsonProperty("customer_id")
    public int getCustomer_id() {
        return customer_id;
    }

    @JsonProperty("customer_id")
    public void setCustomer_id(int customer_id) {
        this.customer_id = customer_id;
    }

    @JsonProperty("points")
    public int getPoints() {
        return points;
    }

    @JsonProperty("points")
    public void setPoints(int points) {
        this.points = points;
    }

    @JsonProperty("product_id")
    public int getProduct_id() {
        return product_id;
    }

    @JsonProperty("product_id")
    public void setProduct_id(int product_id) {
        this.product_id = product_id;
    }

    @JsonProperty("stamps")
    public int getStamps() {
        return stamps;
    }

    @JsonProperty("stamps")
    public void setStamps(int stamps) {
        this.stamps = stamps;
    }

    @JsonProperty("program_type")
    public String getProgram_type() {
        return program_type;
    }

    @JsonProperty("program_type")
    public void setProgram_type(String program_type) {
        this.program_type = program_type;
    }

    @JsonProperty("synced")
    public boolean isSynced() {
        return synced;
    }

    @JsonProperty("synced")
    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    @JsonProperty("created_at")
    public DateTime getCreated_at() {
        return created_at;
    }

    @JsonProperty("created_at")
    public void setCreated_at(DateTime created_at) {
        this.created_at = created_at;
    }
}
