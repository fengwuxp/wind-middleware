package com.wind.client.rest;


import com.wind.api.core.ApiResponse;
import com.wind.common.exception.ApiClientException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractSmartHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;


/**
 * 将 ApiResponse<T> 解包为 T 的 HttpMessageConverter
 *
 * @author wuxp
 * @date 2026-02-26 17:22
 **/
public class ApiResponseUnwrappingConverter extends AbstractSmartHttpMessageConverter<Object> {

    private final SmartHttpMessageConverter<Object> jsonConverter;

    public ApiResponseUnwrappingConverter(JsonMapper jsonMapper) {
        super(MediaType.APPLICATION_JSON);
        // 复用 Spring 默认的 Jackson 转换器（用于实际读）
        this.jsonConverter = new JacksonJsonHttpMessageConverter(jsonMapper);
    }

    @Override
    @Nullable
    protected Object readInternal(@NonNull Class<?> clazz, @NonNull HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return read(clazz, inputMessage);
    }

    @Override
    protected void writeInternal(Object object, @NonNull ResolvableType type, @NonNull HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints) throws IOException,
            HttpMessageNotWritableException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public boolean canRead(@NonNull ResolvableType type, @Nullable MediaType mediaType) {
        // 只处理 JSON 类型
        if (!jsonConverter.canRead(type, mediaType)) {
            return false;
        }
        // 如果目标类型本身就是 ApiResponse（包括其泛型形式），则不解包，让其他转换器处理 ApiResponse 本身
        return !isApiResponseType(type);
    }

    @Override
    public Object read(ResolvableType type, @NonNull HttpInputMessage inputMessage, @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotReadableException {
        // 构造 ApiResponse 的完整泛型类型：ApiResponse<targetType>
        JavaType targetJavaType = TypeFactory.createDefaultInstance().constructType(type.getType());
        JavaType responseJavaType = TypeFactory.createDefaultInstance().constructParametricType(
                ApiResponse.class,
                targetJavaType
        );

        // 使用 jsonConverter 读取 ApiResponse
        ResolvableType apiResponseResolvableType = ResolvableType.forType(responseJavaType);
        ApiResponse<?> response;
        try {
            response = (ApiResponse<?>) jsonConverter.read(apiResponseResolvableType, inputMessage, hints);
        } catch (Exception e) {
            throw new HttpMessageNotReadableException("Failed to read ApiResponse: " + e.getMessage(), e, inputMessage);
        }
        // 业务逻辑判断：如果 success 则返回 data，否则抛出异常
        if (response.isSuccess()) {
            return response.getData();
        } else {
            // 将业务错误转换为异常，可自定义异常类型
            throw new ApiClientException(response, response.getErrorMessage());
        }
    }

    @Override
    public boolean canWrite(@NonNull ResolvableType targetType, @NonNull Class<?> valueClass, @Nullable MediaType mediaType) {
        return false;
    }


    private boolean isApiResponseType(ResolvableType type) {
        Class<?> rawClass = type.getRawClass();
        return rawClass != null && ApiResponse.class.isAssignableFrom(rawClass);
    }

}