package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by ordgen on 12/27/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesOrder {
    @JsonProperty("id")
    private int id;

    @JsonProperty("subtotal")
    private double subtotal;

    @JsonProperty("tax")
    private double tax;

    @JsonProperty("total")
    private double total;

    @JsonProperty("merchant_id")
    private int merchant_id;

    @JsonProperty("user_id")
    private int user_id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private DateTime created_at;

    @JsonProperty("updated_at")
    private DateTime updated_at;

    @JsonProperty("order_items")
    private List<OrderItem> order_items;

   public SalesOrder() {}

   public SalesOrder(
       int id,
       double subtotal,
       double tax,
       double total,
       int merchant_id,
       int user_id,
       String status,
       DateTime created_at,
       DateTime updated_at,
       List<OrderItem> order_items
   ) {
       this.id = id;
       this.subtotal = subtotal;
       this.tax = tax;
       this.total = total;
       this.merchant_id = merchant_id;
       this.user_id = user_id;
       this.status = status;
       this.created_at = created_at;
       this.updated_at = updated_at;
       this.order_items = order_items;
   }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getMerchant_id() {
        return merchant_id;
    }

    public void setMerchant_id(int merchant_id) {
        this.merchant_id = merchant_id;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<OrderItem> getOrder_items() {
        return order_items;
    }

    public void setOrder_items(List<OrderItem> order_items) {
        this.order_items = order_items;
    }

    public DateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(DateTime created_at) {
        this.created_at = created_at;
    }

    public DateTime getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(DateTime updated_at) {
        this.updated_at = updated_at;
    }
}
