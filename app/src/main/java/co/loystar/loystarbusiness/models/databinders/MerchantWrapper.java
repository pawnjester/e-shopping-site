package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by ordgen on 11/21/17.
 */
public class MerchantWrapper {
    @JsonProperty("data")
    private Merchant data;

    public MerchantWrapper() {}

    public Merchant getMerchant() {
        return data;
    }

    public void setMerchant(Merchant data) {
        this.data = data;
    }
}