package co.loystar.loystarbusiness.auth.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Created by ordgen on 11/1/17.
 */

public class ApiUtils {

    public static ObjectMapper getObjectMapper(boolean hasRootValue) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.registerModule(new JodaModule());
        if (hasRootValue) {
            objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
        }
        return objectMapper;
    }
}
