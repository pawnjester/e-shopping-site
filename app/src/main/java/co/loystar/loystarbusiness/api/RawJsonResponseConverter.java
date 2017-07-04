package co.loystar.loystarbusiness.api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import co.loystar.loystarbusiness.api.pojos.RawJsonResponse;
import okhttp3.ResponseBody;

/**
 * Created by laudbruce-tagoe on 4/11/17.
 */

class RawJsonResponseConverter implements retrofit2.Converter<ResponseBody, Object> {

    private final ObjectMapper mapper;

    RawJsonResponseConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object convert(ResponseBody value) {
        RawJsonResponse result = new RawJsonResponse();
        try {

            JsonNode rootNode = mapper.readTree(value.byteStream());
            result.setResponse(new JSONObject(rootNode.toString()));
            return result;
        } catch (IOException | JSONException e) {
            //Crashlytics.logException(e);
            e.printStackTrace();
        }

        return result;
    }
}
