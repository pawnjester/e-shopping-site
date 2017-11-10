package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by ordgen on 11/9/17.
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
public class Customer {
    private int id;
    private int user_id;
    private String first_name;
    private String last_name;
    private String phone_number;
    private String email;
    private Date date_of_birth;
    private Boolean deleted;
    private DateTime created_at;
    private DateTime updated_at;
    private String sex;
    private int merchant_id;

    public Customer() {};

    public Customer(
            int id,
            int merchant_id,
            int user_id,
            String first_name,
            String last_name,
            String phone_number,
            String email,
            String sex,
            boolean deleted,
            DateTime created_at,
            DateTime updated_at,
            Date date_of_birth
    ) {
        this.id = id;
        this.merchant_id = merchant_id;
        this.user_id = user_id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.phone_number = phone_number;
        this.email = email;
        this.sex = sex;
        this.deleted = deleted;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.date_of_birth = date_of_birth;
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

    @JsonProperty("date_of_birth")
    public Date getDate_of_birth() {
        return date_of_birth;
    }

    @JsonProperty("date_of_birth")
    public void setDate_of_birth(Date date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    @JsonProperty("user_id")
    public int getUser_id() {
        return user_id;
    }

    @JsonProperty("user_id")
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
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

    @JsonProperty("sex")
    public String getSex() {
        return sex;
    }

    @JsonProperty("sex")
    public void setSex(String sex) {
        this.sex = sex;
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
