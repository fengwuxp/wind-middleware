package com.wind.client.rest;

import com.wind.api.core.signature.ApiSecretAccount;
import com.wind.api.core.signature.ApiSignatureRequest;
import com.wind.api.core.signature.SignatureHttpHeaderNames;
import com.wind.common.exception.AssertUtils;
import com.wind.sequence.SequenceGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * 接口请求签名加签请求拦截器，对 application/json 和 application/x-www-form-urlencoded 请求进行签名加签
 * 详情参见：https://www.yuque.com/suiyuerufeng-akjad/wind/zl1ygpq3pitl00qp
 *
 * @author wuxp
 * @date 2024-02-21 15:45
 **/
@Slf4j
public class ApiSignatureRequestInterceptor implements ClientHttpRequestInterceptor {

    private final Function<HttpRequest, ApiSecretAccount> accountProvider;

    private final SignatureHttpHeaderNames headerNames;

    public ApiSignatureRequestInterceptor(Function<HttpRequest, ApiSecretAccount> accountProvider) {
        this(accountProvider, null);
    }

    public ApiSignatureRequestInterceptor(Function<HttpRequest, ApiSecretAccount> accountProvider, String headerPrefix) {
        AssertUtils.notNull(accountProvider, "argument accountProvider must not null");
        this.accountProvider = accountProvider;
        this.headerNames = new SignatureHttpHeaderNames(headerPrefix);
    }

    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request, byte @NonNull [] body, @NonNull ClientHttpRequestExecution execution) throws IOException {
        ApiSecretAccount account = accountProvider.apply(request);
        AssertUtils.notNull(account, "ApiSecretAccount must not null");
        ApiSignatureRequest.ApiSignatureRequestBuilder builder = ApiSignatureRequest.builder();
        builder.method(request.getMethod().name())
                .requestPath(request.getURI().getPath())
                .nonce(SequenceGenerator.randomAlphanumeric(32))
                .timestamp(String.valueOf(System.currentTimeMillis()))
                .queryString(request.getURI().getQuery());
        if (signRequireRequestBody(request.getHeaders().getContentType())) {
            builder.requestBody(new String(body, StandardCharsets.UTF_8));
        }
        ApiSignatureRequest signatureRequest = builder.build();
        request.getHeaders().add(headerNames.accessId(), account.getAccessId());
        if (StringUtils.hasText(account.getSecretKeyVersion())) {
            request.getHeaders().add(headerNames.secretVersion(), account.getSecretKeyVersion());
        }
        request.getHeaders().add(headerNames.timestamp(), signatureRequest.timestamp());
        request.getHeaders().add(headerNames.nonce(), signatureRequest.nonce());
        String sign = account.getSigner().sign(signatureRequest, account.getSecretKey());
        request.getHeaders().add(headerNames.sign(), sign);
        if (log.isDebugEnabled()) {
            log.debug("api sign request = {} , sign = {}", request, sign);
        }
        return execution.execute(request, body);
    }

    private static boolean signRequireRequestBody(@Nullable MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        return ApiSignatureRequest.signRequireRequestBody(contentType.toString());
    }
}
