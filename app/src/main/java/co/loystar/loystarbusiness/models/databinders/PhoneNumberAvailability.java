package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by ordgen on 11/22/17.
 */

public class PhoneNumberAvailability {
    @JsonProperty("phone_available")
    private boolean phoneAvailable;

    public boolean isPhoneAvailable() {
        return phoneAvailable;
    }

    public void setPhoneAvailable(boolean phoneAvailable) {
        this.phoneAvailable = phoneAvailable;
    }
}
