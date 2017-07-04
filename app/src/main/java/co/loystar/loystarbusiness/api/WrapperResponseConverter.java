package co.loystar.loystarbusiness.api;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

/**
 * Created by laudbruce-tagoe on 4/9/17.
 */

public class WrapperResponseConverter implements retrofit2.Converter<ResponseBody, Object> {

    private final ObjectReader reader;
    private final ObjectMapper mapper;
    private static final String TAG = WrapperResponseConverter.class.getSimpleName();

    public WrapperResponseConverter(ObjectReader reader, ObjectMapper mapper)  {
        this.reader = reader;
        this.mapper = mapper;
    }

    @Override
    public Object convert(ResponseBody value) throws IOException {
        JsonNode responseNode = mapper.readTree(value.charStream());
        ResponseBody originalValue  = ResponseBody.create(MediaType.parse("application/json; charset=utf-8"), responseNode.toString());
        try {
            if (responseNode.fields().hasNext()) {
                String wrapperName = responseNode.fields().next().getKey();
                if (!responseNode.get(wrapperName).isValueNode()) {
                    JsonNode unwrappedNode = responseNode.get(wrapperName);
                    Log.e("REQ", "CODE:unwrappedNode " + unwrappedNode.toString());
                    ResponseBody unwrappedValue  = ResponseBody.create(MediaType.parse("application/json; charset=utf-8"), unwrappedNode.toString());
                    return reader.readValue(unwrappedValue.charStream());
                }
                else {
                    return reader.readValue(originalValue.charStream());
                }
            } else {
                return reader.readValue(originalValue.charStream());
            }

        } finally {
            value.close();
        }
    }
}
