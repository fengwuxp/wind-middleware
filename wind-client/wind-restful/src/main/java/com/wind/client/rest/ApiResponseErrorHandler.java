package com.wind.client.rest;

import com.wind.api.core.ApiResponse;
import com.wind.common.exception.ApiClientException;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;

/**
 * Wind Api 风格的 Http 错误处理器
 *
 * @author wuxp
 * @date 2026-03-11 10:00
 * @see ApiResponse
 **/
@AllArgsConstructor
public class ApiResponseErrorHandler implements ResponseErrorHandler {

    private final JsonMapper jsonMapper;

    @Override
    public boolean hasError(@NonNull ClientHttpResponse response) throws IOException {
        return response.getStatusCode().value() >= 400;
    }

    @Override
    public void handleError(@NonNull URI url, @NonNull HttpMethod method, ClientHttpResponse response) throws IOException {
        ApiResponse<?> apiResponse = jsonMapper.readValue(response.getBody(), ApiResponse.class);
        throw new ApiClientException(apiResponse, apiResponse.getErrorMessage());
    }
}
