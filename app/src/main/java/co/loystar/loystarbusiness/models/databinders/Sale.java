package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by ordgen on 2/6/18.
 */

public class Sale {
    @JsonProperty("id")
    private int id;

    @JsonProperty("total")
    private double total;

    @JsonProperty("merchant_id")
    private int merchant_id;

    @JsonProperty("user_id")
    private int user_id;

    @JsonProperty("created_at")
    private DateTime created_at;

    @JsonProperty("updated_at")
    private DateTime updated_at;

    @JsonProperty("is_paid_with_card")
    private boolean is_paid_with_card;

    @JsonProperty("is_paid_with_cash")
    private boolean is_paid_with_cash;

    @JsonProperty("is_paid_with_mobile")
    private boolean is_paid_with_mobile;

    @JsonProperty("transactions")
    private List<Transaction> transactions;

    public Sale() {}

    public Sale(
        int id,
        double total,
        int merchant_id,
        int user_id,
        DateTime created_at,
        DateTime updated_at,
        boolean is_paid_with_card,
        boolean is_paid_with_cash,
        boolean is_paid_with_mobile,
        List<Transaction> transactions
    ) {
        this.id = id;
        this.total = total;
        this.merchant_id = merchant_id;
        this.user_id = user_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.is_paid_with_card = is_paid_with_card;
        this.is_paid_with_cash = is_paid_with_cash;
        this.is_paid_with_mobile = is_paid_with_mobile;
        this.transactions = transactions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public int getMerchant_id() {
        return merchant_id;
    }

    public void setMerchant_id(int merchant_id) {
        this.merchant_id = merchant_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public boolean isPaid_with_card() {
        return is_paid_with_card;
    }

    public void setPaid_with_card(boolean paid_with_card) {
        this.is_paid_with_card = paid_with_card;
    }


    public boolean isPaid_with_cash() {
        return is_paid_with_cash;
    }

    public void setPaid_with_cash(boolean paid_with_cash) {
        this.is_paid_with_cash = paid_with_cash;
    }

    public boolean isPaid_with_mobile() {
        return is_paid_with_mobile;
    }

    public void setPaid_with_mobile(boolean paid_with_mobile) {
        this.is_paid_with_mobile = paid_with_mobile;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
