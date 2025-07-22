package com.wind.client.retrofit.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * @author wuxp
 * @date 2024-02-27 14:43
 **/
@AllArgsConstructor
@Slf4j
public class JacksonResponseCallAdapterFactory extends CallAdapter.Factory {

    private final ObjectMapper objectMapper;

    private final Class<?> responseType;

    @SuppressWarnings("rawtypes")
    private final Function responseExtractor;

    public JacksonResponseCallAdapterFactory(ObjectMapper objectMapper) {
        this(objectMapper, null, o -> o);
    }

    @Override
    public CallAdapter<?, ?> get(@NotNull Type returnType, @NotNull Annotation @NotNull [] annotations, @NotNull Retrofit retrofit) {
        return new RespCallAdapter<>(returnType);
    }

    class RespCallAdapter<R> implements CallAdapter<R, Object> {

        private final Type returnType;

        public RespCallAdapter(Type responseType) {
            this.returnType = responseType;
        }

        @Override
        @NotNull
        public Type responseType() {
            return returnType;
        }

        @Override
        @NotNull
        public Object adapt(Call<R> call) {
            try {
                Response<R> response = call.execute();
                return response.isSuccessful() ? response.body() : parseErrorResp(response.errorBody());
            } catch (IOException exception) {
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "convert resp body error, case by: " + exception.getMessage(), exception);
            }
        }

        @SuppressWarnings("unchecked")
        private Object parseErrorResp(ResponseBody errorBody) throws IOException {
            JavaType javaType = objectMapper.getTypeFactory().constructType(responseType);
            ObjectReader reader = objectMapper.readerFor(javaType);
            try (errorBody) {
                Object resp = reader.readValue(errorBody.charStream());
                return responseExtractor.apply(resp);
            }
        }
    }

}
