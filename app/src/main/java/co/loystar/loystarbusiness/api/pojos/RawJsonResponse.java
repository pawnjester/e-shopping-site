package co.loystar.loystarbusiness.api.pojos;

import org.json.JSONObject;

/**
 * Created by laudbruce-tagoe on 4/11/17.
 */

public class RawJsonResponse {
    private JSONObject response;

    public JSONObject getResponse() {
        return response;
    }

    public void setResponse(JSONObject response) {
        this.response = response;
    }
}
