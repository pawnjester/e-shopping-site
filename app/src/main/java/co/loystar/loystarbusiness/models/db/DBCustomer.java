package co.loystar.loystarbusiness.models.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.greenrobot.greendao.annotation.*;

/**
 * Entity mapped to table "DBCUSTOMER".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "user_id",
        "first_name",
        "last_name",
        "phone_number",
        "email",
        "date_of_birth",
        "synced",
        "deleted",
        "created_at",
        "updated_at",
        "local_db_created_at",
        "local_db_updated_at",
        "update_required",
        "token",
        "sex",
        "merchant_id",
})
@Entity
public class DBCustomer {

    @Id(autoincrement = true)
    private Long id;
    private Long user_id;
    private String first_name;
    private String last_name;
    private String phone_number;
    private String email;
    private java.util.Date date_of_birth;
    private Integer synced;
    private Boolean deleted;
    private java.util.Date created_at;
    private java.util.Date updated_at;
    private java.util.Date local_db_created_at;
    private java.util.Date local_db_updated_at;
    private Boolean update_required;
    private String token;
    private String sex;
    private Long merchant_id;

    @Generated(hash = 196190902)
    public DBCustomer() {
    }

    public DBCustomer(Long id) {
        this.id = id;
    }

    @Generated(hash = 1512337932)
    public DBCustomer(Long id, Long user_id, String first_name, String last_name, String phone_number, String email, java.util.Date date_of_birth, Integer synced, Boolean deleted, java.util.Date created_at, java.util.Date updated_at, java.util.Date local_db_created_at, java.util.Date local_db_updated_at, Boolean update_required, String token, String sex, Long merchant_id) {
        this.id = id;
        this.user_id = user_id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.phone_number = phone_number;
        this.email = email;
        this.date_of_birth = date_of_birth;
        this.synced = synced;
        this.deleted = deleted;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.local_db_created_at = local_db_created_at;
        this.local_db_updated_at = local_db_updated_at;
        this.update_required = update_required;
        this.token = token;
        this.sex = sex;
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

    @JsonProperty("user_id")
    public Long getUser_id() {
        return user_id;
    }

    @JsonProperty("user_id")
    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    @JsonProperty("first_name")
    public String getFirst_name() {
        return first_name;
    }

    @JsonProperty("first_name")
    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    @JsonProperty("last_name")
    public String getLast_name() {
        return last_name;
    }

    @JsonProperty("last_name")
    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    @JsonProperty("phone_number")
    public String getPhone_number() {
        return phone_number;
    }

    @JsonProperty("phone_number")
    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("date_of_birth")
    public java.util.Date getDate_of_birth() {
        return date_of_birth;
    }

    @JsonProperty("date_of_birth")
    public void setDate_of_birth(java.util.Date date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    @JsonProperty("synced")
    public Integer getSynced() {
        return synced;
    }

    @JsonProperty("synced")
    public void setSynced(Integer synced) {
        this.synced = synced;
    }

    @JsonProperty("deleted")
    public Boolean getDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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

    @JsonProperty("local_db_created_at")
    public java.util.Date getLocal_db_created_at() {
        return local_db_created_at;
    }

    @JsonProperty("local_db_created_at")
    public void setLocal_db_created_at(java.util.Date local_db_created_at) {
        this.local_db_created_at = local_db_created_at;
    }

    @JsonProperty("local_db_updated_at")
    public java.util.Date getLocal_db_updated_at() {
        return local_db_updated_at;
    }

    @JsonProperty("local_db_updated_at")
    public void setLocal_db_updated_at(java.util.Date local_db_updated_at) {
        this.local_db_updated_at = local_db_updated_at;
    }

    @JsonProperty("update_required")
    public Boolean getUpdate_required() {
        return update_required;
    }

    @JsonProperty("update_required")
    public void setUpdate_required(Boolean update_required) {
        this.update_required = update_required;
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("sex")
    public String getSex() {
        return sex;
    }

    @JsonProperty("sex")
    public void setSex(String sex) {
        this.sex = sex;
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
