package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

/**
 * Created by ordgen on 12/27/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItem {
    @JsonProperty("id")
    private int id;

    @JsonProperty("created_at")
    private DateTime created_at;

    @JsonProperty("updated_at")
    private DateTime updated_at;

    @JsonProperty("product_id")
    private int product_id;

    @JsonProperty("order_id")
    private int order_id;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("unit_price")
    private double unit_price;

    @JsonProperty("total_price")
    private double total_price;

    public OrderItem() {}

    public OrderItem(
        int id,
        int product_id,
        int order_id,
        int quantity,
        double unit_price,
        double total_price,
        DateTime created_at,
        DateTime updated_at
    ) {
        this.id = id;
        this.created_at = created_at;
        this.unit_price = unit_price;
        this.updated_at = updated_at;
        this.order_id  = order_id;
        this.total_price = total_price;
        this.quantity = quantity;
        this.product_id = product_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public DateTime getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(DateTime updated_at) {
        this.updated_at = updated_at;
    }

    public DateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(DateTime created_at) {
        this.created_at = created_at;
    }

    public int getProduct_id() {
        return product_id;
    }

    public void setProduct_id(int product_id) {
        this.product_id = product_id;
    }

    public double getTotal_price() {
        return total_price;
    }

    public void setTotal_price(double total_price) {
        this.total_price = total_price;
    }

    public double getUnit_price() {
        return unit_price;
    }

    public void setUnit_price(double unit_price) {
        this.unit_price = unit_price;
    }

    public int getOrder_id() {
        return order_id;
    }

    public void setOrder_id(int order_id) {
        this.order_id = order_id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

