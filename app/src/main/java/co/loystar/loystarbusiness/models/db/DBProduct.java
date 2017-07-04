package co.loystar.loystarbusiness.models.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.greenrobot.greendao.annotation.*;

/**
 * Entity mapped to table "DBPRODUCT".
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
@Entity
public class DBProduct {

    @Id
    private Long id;
    private java.util.Date created_at;
    private java.util.Date updated_at;
    private Double price;
    private Long merchant_id;
    private String name;
    private String picture;
    private String description;
    private Boolean deleted;
    private Long merchant_product_category_id;

    @Generated(hash = 811650195)
    public DBProduct() {
    }

    public DBProduct(Long id) {
        this.id = id;
    }

    @Generated(hash = 596276742)
    public DBProduct(Long id, java.util.Date created_at, java.util.Date updated_at, Double price, Long merchant_id, String name, String picture, String description, Boolean deleted, Long merchant_product_category_id) {
        this.id = id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.price = price;
        this.merchant_id = merchant_id;
        this.name = name;
        this.picture = picture;
        this.description = description;
        this.deleted = deleted;
        this.merchant_product_category_id = merchant_product_category_id;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
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

    @JsonProperty("price")
    public Double getPrice() {
        return price;
    }

    @JsonProperty("price")
    public void setPrice(Double price) {
        this.price = price;
    }

    @JsonProperty("merchant_id")
    public Long getMerchant_id() {
        return merchant_id;
    }

    @JsonProperty("merchant_id")
    public void setMerchant_id(Long merchant_id) {
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

    @JsonProperty("picture")
    public String getPicture() {
        return picture;
    }

    @JsonProperty("picture")
    public void setPicture(String picture) {
        this.picture = picture;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("deleted")
    public Boolean getDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @JsonProperty("merchant_product_category_id")
    public Long getMerchant_product_category_id() {
        return merchant_product_category_id;
    }

    @JsonProperty("merchant_product_category_id")
    public void setMerchant_product_category_id(Long merchant_product_category_id) {
        this.merchant_product_category_id = merchant_product_category_id;
    }

}
