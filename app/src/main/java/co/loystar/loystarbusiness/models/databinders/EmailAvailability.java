package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by ordgen on 11/22/17.
 */

public class EmailAvailability {
    @JsonProperty("email_available")
    private boolean emailAvailable;

    public boolean isEmailAvailable() {
        return emailAvailable;
    }

    public void setEmailAvailable(boolean emailAvailable) {
        this.emailAvailable = emailAvailable;
    }
}
