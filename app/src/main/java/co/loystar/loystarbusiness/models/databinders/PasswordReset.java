package co.loystar.loystarbusiness.models.databinders;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by ordgen on 11/23/17.
 */

public class PasswordReset {
    @JsonProperty("success")
    private Boolean success;
    @JsonProperty("message")
    private String message;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
