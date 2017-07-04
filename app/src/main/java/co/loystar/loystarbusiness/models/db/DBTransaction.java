package co.loystar.loystarbusiness.models.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.greenrobot.greendao.annotation.*;


/**
 * Entity mapped to table "DBTRANSACTION".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "user_id",
        "product_id",
        "merchant_loyalty_program_id",
        "auth_token",
        "amount",
        "points",
        "stamps",
        "synced",
        "created_at",
        "updated_at",
        "program_type",
        "notes",
        "local_db_created_at",
        "deleted",
        "merchant_id",
        "send_sms"
})
@Entity
public class DBTransaction {

    @Id(autoincrement = true)
    private Long id;
    private Long user_id;
    private Long product_id;
    private Long merchant_loyalty_program_id;
    private String auth_token;
    private Integer amount;
    private Integer points;
    private Integer stamps;
    private Boolean synced;
    private java.util.Date created_at;
    private java.util.Date updated_at;
    private String program_type;
    private String notes;
    private java.util.Date local_db_created_at;
    private Integer deleted;
    private Long merchant_id;
    private Boolean send_sms;


    @Generated(hash = 378552605)
    public DBTransaction() {
    }

    public DBTransaction(Long id) {
        this.id = id;
    }

    @Generated(hash = 2389290)
    public DBTransaction(Long id, Long user_id, Long product_id, Long merchant_loyalty_program_id, String auth_token, Integer amount, Integer points, Integer stamps, Boolean synced, java.util.Date created_at, java.util.Date updated_at, String program_type, String notes, java.util.Date local_db_created_at, Integer deleted, Long merchant_id, Boolean send_sms) {
        this.id = id;
        this.user_id = user_id;
        this.product_id = product_id;
        this.merchant_loyalty_program_id = merchant_loyalty_program_id;
        this.auth_token = auth_token;
        this.amount = amount;
        this.points = points;
        this.stamps = stamps;
        this.synced = synced;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.program_type = program_type;
        this.notes = notes;
        this.local_db_created_at = local_db_created_at;
        this.deleted = deleted;
        this.merchant_id = merchant_id;
        this.send_sms = send_sms;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("user_id")
    public Long getUser_id() {
        return user_id;
    }

    @JsonProperty("user_id")
    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    @JsonProperty("product_id")
    public Long getProduct_id() {
        return product_id;
    }

    @JsonProperty("product_id")
    public void setProduct_id(Long product_id) {
        this.product_id = product_id;
    }

    @JsonProperty("merchant_loyalty_program_id")
    public Long getMerchant_loyalty_program_id() {
        return merchant_loyalty_program_id;
    }

    @JsonProperty("merchant_loyalty_program_id")
    public void setMerchant_loyalty_program_id(Long merchant_loyalty_program_id) {
        this.merchant_loyalty_program_id = merchant_loyalty_program_id;
    }

    @JsonProperty("auth_token")
    public String getAuth_token() {
        return auth_token;
    }

    @JsonProperty("auth_token")
    public void setAuth_token(String auth_token) {
        this.auth_token = auth_token;
    }

    @JsonProperty("amount")
    public Integer getAmount() {
        return amount;
    }

    @JsonProperty("amount")
    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    @JsonProperty("points")
    public Integer getPoints() {
        return points;
    }

    @JsonProperty("points")
    public void setPoints(Integer points) {
        this.points = points;
    }

    @JsonProperty("stamps")
    public Integer getStamps() {
        return stamps;
    }

    @JsonProperty("stamps")
    public void setStamps(Integer stamps) {
        this.stamps = stamps;
    }

    @JsonProperty("synced")
    public Boolean getSynced() {
        return synced;
    }

    @JsonProperty("synced")
    public void setSynced(Boolean synced) {
        this.synced = synced;
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

    @JsonProperty("notes")
    public String getNotes() {
        return notes;
    }

    @JsonProperty("notes")
    public void setNotes(String notes) {
        this.notes = notes;
    }

    @JsonProperty("local_db_created_at")
    public java.util.Date getLocal_db_created_at() {
        return local_db_created_at;
    }

    @JsonProperty("local_db_created_at")
    public void setLocal_db_created_at(java.util.Date local_db_created_at) {
        this.local_db_created_at = local_db_created_at;
    }

    @JsonProperty("deleted")
    public Integer getDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @JsonProperty("merchant_id")
    public Long getMerchant_id() {
        return merchant_id;
    }

    @JsonProperty("merchant_id")
    public void setMerchant_id(Long merchant_id) {
        this.merchant_id = merchant_id;
    }

    @JsonProperty("send_sms")
    public void setSend_sms(Boolean send_sms) {
        this.send_sms = send_sms;
    }

    @JsonProperty("send_sms")
    public Boolean getSend_sms() {
        return send_sms;
    }

}
