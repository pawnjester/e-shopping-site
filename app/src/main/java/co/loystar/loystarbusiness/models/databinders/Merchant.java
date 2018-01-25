package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.joda.time.DateTime;

/**
 * Created by ordgen on 11/1/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "email",
        "first_name",
        "last_name",
        "address_line1",
        "address_line2",
        "contact_number",
        "business_name",
        "business_type",
        "currency",
        "subscription_expires_on",
        "subscription_plan",
        "turn_on_point_of_sale",
})
public class Merchant {
    @JsonProperty("id")
    private int id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("first_name")
    private String first_name;

    @JsonProperty("last_name")
    private String last_name;

    @JsonProperty("address_line1")
    private String address_line1;

    @JsonProperty("address_line2")
    private String address_line2;

    @JsonProperty("contact_number")
    private String contact_number;

    @JsonProperty("business_name")
    private String business_name;

    @JsonProperty("business_type")
    private String business_type;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("subscription_expires_on")
    private DateTime subscription_expires_on;

    @JsonProperty("subscription_plan")
    private String subscription_plan;

    @JsonProperty("turn_on_point_of_sale")
    private Boolean turn_on_point_of_sale;

    @JsonProperty("enable_bluetooth_printing")
    private Boolean enable_bluetooth_printing;

    @JsonProperty("sync_frequency")
    private Integer sync_frequency;

    public Merchant() {}

    public Merchant(
            int id,
            String email,
            String first_name,
            String last_name,
            String address_line1,
            String address_line2,
            String contact_number,
            String business_name,
            String business_type,
            String currency,
            DateTime subscription_expires_on,
            String subscription_plan,
            Boolean turn_on_point_of_sale,
            Boolean enable_bluetooth_printing,
            Integer sync_frequency
    ) {
        this.id = id;
        this.email = email;
        this.first_name = first_name;
        this.last_name = last_name;
        this.address_line1 = address_line1;
        this.address_line2 = address_line2;
        this.contact_number = contact_number;
        this.business_name = business_name;
        this.business_type = business_type;
        this.currency = currency;
        this.subscription_expires_on = subscription_expires_on;
        this.subscription_plan = subscription_plan;
        this.turn_on_point_of_sale = turn_on_point_of_sale;
        this.enable_bluetooth_printing = enable_bluetooth_printing;
        this.sync_frequency = sync_frequency;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getAddress_line1() {
        return address_line1;
    }

    public Boolean isTurn_on_point_of_sale() {
        return turn_on_point_of_sale;
    }

    public void setTurn_on_point_of_sale(Boolean turn_on_point_of_sale) {
        this.turn_on_point_of_sale = turn_on_point_of_sale;
    }

    public Boolean getEnable_bluetooth_printing() {
        return enable_bluetooth_printing;
    }

    public void setAddress_line1(String address_line1) {
        this.address_line1 = address_line1;
    }

    public String getAddress_line2() {
        return address_line2;
    }

    public void setAddress_line2(String address_line2) {
        this.address_line2 = address_line2;
    }

    public String getContact_number() {
        return contact_number;
    }

    public void setContact_number(String contact_number) {
        this.contact_number = contact_number;
    }

    public String getBusiness_name() {
        return business_name;
    }

    public void setBusiness_name(String business_name) {
        this.business_name = business_name;
    }

    public String getBusiness_type() {
        return business_type;
    }

    public void setBusiness_type(String business_type) {
        this.business_type = business_type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public DateTime getSubscription_expires_on() {
        return subscription_expires_on;
    }

    public void setSubscription_expires_on(DateTime subscription_expires_on) {
        this.subscription_expires_on = subscription_expires_on;
    }

    public void setEnable_bluetooth_printing(Boolean enable_bluetooth_printing) {
        this.enable_bluetooth_printing = enable_bluetooth_printing;
    }

    public Integer getSync_frequency() {
        return sync_frequency;
    }

    public void setSync_frequency(Integer sync_frequency) {
        this.sync_frequency = sync_frequency;
    }

    public Boolean getTurn_on_point_of_sale() {
        return turn_on_point_of_sale;
    }

    public String getSubscription_plan() {
        return subscription_plan;
    }

    public void setSubscription_plan(String subscription_plan) {
        this.subscription_plan = subscription_plan;
    }
}
