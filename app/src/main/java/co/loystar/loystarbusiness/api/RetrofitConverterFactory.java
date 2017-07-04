package co.loystar.loystarbusiness.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

;

/**
 * Created by laudbruce-tagoe on 4/9/17.
 */

class RetrofitConverterFactory extends Converter.Factory {

    private final ObjectMapper mapper = JsonUtils.objectMapper;

    RetrofitConverterFactory() {}

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(final Type type, Annotation[] annotations, Retrofit retrofit) {


        JavaType javaType = mapper.getTypeFactory().constructType(type);
        ObjectReader reader = mapper.readerFor(javaType);

        return new WrapperResponseConverter(reader, mapper);
    }
}
