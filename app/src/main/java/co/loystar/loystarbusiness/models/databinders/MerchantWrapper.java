package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by ordgen on 11/21/17.
 */
public class MerchantWrapper {
    @JsonProperty("data")
    private Merchant merchant;

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }
}