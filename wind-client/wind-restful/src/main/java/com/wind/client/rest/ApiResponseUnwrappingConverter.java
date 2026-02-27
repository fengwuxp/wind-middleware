package com.wind.client.rest;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.wind.api.core.ApiResponse;
import com.wind.common.exception.ApiClientException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


/**
 * 将 ApiResponse<T> 解包为 T 的 HttpMessageConverter
 *
 * @author wuxp
 * @date 2026-02-26 17:22
 **/
public class ApiResponseUnwrappingConverter extends AbstractGenericHttpMessageConverter<Object> {

    private final GenericHttpMessageConverter<Object> jsonConverter;

    public ApiResponseUnwrappingConverter(ObjectMapper objectMapper) {
        super(MediaType.APPLICATION_JSON);
        // 复用 Spring 默认的 Jackson2 转换器（用于实际读）
        this.jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
    }

    @Override
    public boolean canRead(@NonNull Class<?> clazz, MediaType mediaType) {
        return canRead(clazz, null, mediaType);
    }

    @Override
    @Nullable
    protected Object readInternal(@NonNull Class<?> clazz, @NonNull HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return read(clazz, null, inputMessage);
    }

    @Override
    public boolean canRead(@NonNull Type type, Class<?> contextClass, MediaType mediaType) {
        // 只处理 JSON 类型
        if (!jsonConverter.canRead(type, contextClass, mediaType)) {
            return false;
        }
        // 如果目标类型本身就是 ApiResponse（包括其泛型形式），则不解包，让其他转换器处理 ApiResponse 本身
        return !isApiResponseType(type);
    }

    @Override
    protected void writeInternal(@NonNull Object object, @Nullable Type type, @NonNull HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public @Nullable Object read(@NonNull Type type, @Nullable Class<?> contextClass, @NonNull HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        // 1. 构造 ApiResponse 的泛型类型
        JavaType apiResponseType = TypeFactory.defaultInstance().constructParametricType(
                ApiResponse.class,
                TypeFactory.defaultInstance().constructType(type) // 原始类型作为 data 类型
        );
        // 2. 使用 jsonConverter 读取为 ApiResponse 对象
        ApiResponse<?> response = (ApiResponse<?>) jsonConverter.read(apiResponseType, contextClass, inputMessage);
        // 3. 根据业务逻辑处理
        if (response.isSuccess()) {
            return response.getData();
        } else {
            // 可以根据需要抛出特定异常，让上层统一处理
            throw new ApiClientException(response, response.getErrorMessage());
        }
    }

    @Override
    protected boolean canWrite(@Nullable MediaType mediaType) {
        // 不支持写入
        return false;
    }

    private boolean isApiResponseType(Type type) {
        if (type instanceof Class<?> clazz) {
            return ApiResponse.class.isAssignableFrom(clazz);
        }
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            return rawType instanceof Class && ApiResponse.class.isAssignableFrom((Class<?>) rawType);
        }
        return false;
    }

}