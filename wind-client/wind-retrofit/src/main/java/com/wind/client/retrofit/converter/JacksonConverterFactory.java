package com.wind.client.retrofit.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.wind.common.WindConstants;
import lombok.AllArgsConstructor;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author wuxp
 * @date 2024-02-27 14:28
 **/
@AllArgsConstructor
public class JacksonConverterFactory extends Converter.Factory {

    private final ObjectMapper objectMapper;

    private final Class<?> responseType;

    @SuppressWarnings("rawtypes")
    private final Function responseExtractor;

    public JacksonConverterFactory(ObjectMapper objectMapper) {
        this(objectMapper, null, o -> o);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(
            @NotNull Type type,
            @NotNull Annotation[] parameterAnnotations,
            @NotNull Annotation[] methodAnnotations,
            @NotNull Retrofit retrofit) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        ObjectWriter writer = objectMapper.writerFor(javaType);
        return new JacksonRequestBodyConverter<>(writer);
    }

    @SuppressWarnings({"rawtypes"})
    public @Nullable Converter<?, String> stringConverter(
            @NotNull Type type, Annotation @NotNull [] annotations, @NotNull Retrofit retrofit) {
        return (Converter<Object, String>) value -> {
            if (value instanceof Collection val) {
                return joinAsQueryString(val);
            }
            if (value.getClass().isArray()) {
                List args = Arrays.asList((Object[]) value);
                return joinAsQueryString(args);
            }
            return String.valueOf(value);
        };
    }

    private String joinAsQueryString(Collection<?> args) {
        return args.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(WindConstants.COMMA));
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(@NotNull Type type, Annotation @NotNull [] annotations, @NotNull Retrofit retrofit) {
        return new JacksonResponseBodyConverter<>(type);
    }

    final class JacksonResponseBodyConverter<T> implements Converter<ResponseBody, T> {

        private final Type type;

        public JacksonResponseBodyConverter(Type type) {
            this.type = type;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T convert(ResponseBody value) throws IOException {
            Object result = objectMapper.readValue(value.bytes(), resolveResponseType());
            return (T) responseExtractor.apply(result);
        }

        private JavaType resolveResponseType() {
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            if (responseType == null) {
                return javaType;
            }
            return objectMapper.getTypeFactory().constructParametricType(responseType, javaType);
        }
    }

    static final class JacksonRequestBodyConverter<T> implements Converter<T, RequestBody> {

        private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");

        private final ObjectWriter adapter;

        JacksonRequestBodyConverter(ObjectWriter adapter) {
            this.adapter = adapter;
        }

        @Override
        public RequestBody convert(@NotNull T value) throws IOException {
            byte[] bytes = adapter.writeValueAsBytes(value);
            return RequestBody.create(bytes, MEDIA_TYPE);
        }
    }

}
