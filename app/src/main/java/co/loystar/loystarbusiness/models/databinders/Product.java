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
        "price",
        "merchant_id",
        "name",
        "picture",
        "description",
        "deleted",
        "merchant_product_category_id"
})
public class Product {
    private int id;
    private DateTime created_at;
    private DateTime updated_at;
    private double price;
    private int merchant_id;
    private String name;
    private String picture;
    private String description;
    private Boolean deleted;
    private int merchant_product_category_id;

    public Product() {}

    public Product(
            int id,
            int merchant_id,
            DateTime created_at,
            DateTime updated_at,
            boolean deleted,
            String name,
            double price,
            String picture,
            int merchant_product_category_id,
            String description
    ) {
        this.id  = id;
        this.merchant_id = merchant_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.deleted = deleted;
        this.name = name;
        this.price = price;
        this.picture = picture;
        this.description = description;
        this.merchant_product_category_id = merchant_product_category_id;
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

    @JsonProperty("price")
    public double getPrice() {
        return price;
    }

    @JsonProperty("price")
    public void setPrice(double price) {
        this.price = price;
    }

    @JsonProperty("merchant_product_category_id")
    public int getMerchant_product_category_id() {
        return merchant_product_category_id;
    }

    @JsonProperty("merchant_product_category_id")
    public void setMerchant_product_category_id(int merchant_product_category_id) {
        this.merchant_product_category_id = merchant_product_category_id;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("picture")
    public String getPicture() {
        return picture;
    }

    @JsonProperty("picture")
    public void setPicture(String picture) {
        this.picture = picture;
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
