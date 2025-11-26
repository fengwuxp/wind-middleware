package com.wind.client.retrofit;

import com.wind.api.core.signature.ApiSecretAccount;
import com.wind.api.core.signature.ApiSignatureRequest;
import com.wind.api.core.signature.SignatureConstants;
import com.wind.api.core.signature.SignatureHttpHeaderNames;
import com.wind.common.exception.AssertUtils;
import com.wind.sequence.SequenceGenerator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * 接口请求签名加签请求拦截器
 * 参见：https://www.yuque.com/suiyuerufeng-akjad/wind/zl1ygpq3pitl00qp
 *
 * @author wuxp
 * @date 2024-02-26 18:59
 **/
@Slf4j
public class OkHttpApiSignatureRequestInterceptor implements Interceptor {

    private final Function<Request, ApiSecretAccount> accountProvider;

    private final SignatureHttpHeaderNames headerNames;

    public OkHttpApiSignatureRequestInterceptor(Function<Request, ApiSecretAccount> accountProvider) {
        this(accountProvider, null);
    }

    public OkHttpApiSignatureRequestInterceptor(Function<Request, ApiSecretAccount> accountProvider, String headerPrefix) {
        AssertUtils.notNull(accountProvider, "argument accountProvider must not null");
        this.accountProvider = accountProvider;
        this.headerNames = new SignatureHttpHeaderNames(headerPrefix);
    }

    @NonNull
    @Override
    public Response intercept(Interceptor.@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        ApiSecretAccount account = accountProvider.apply(request);
        AssertUtils.notNull(account, "ApiSecretAccount must not null");
        ApiSignatureRequest.ApiSignatureRequestBuilder builder = ApiSignatureRequest.builder();
        builder.method(request.method())
                .requestPath(request.url().encodedPath())
                .nonce(SequenceGenerator.randomAlphanumeric(32))
                .timestamp(String.valueOf(System.currentTimeMillis()))
                .queryString(getQueryString(request.url()));
        RequestBody requestBody = request.body();
        if (signRequireRequestBody(requestBody)) {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            builder.requestBody(new String(buffer.readByteArray(), StandardCharsets.UTF_8));
        }
        ApiSignatureRequest signatureRequest = builder.build();
        Request.Builder requestBuilder = request.newBuilder();
        requestBuilder.addHeader(headerNames.accessId(), account.getAccessId());
        if (StringUtils.hasText(account.getSecretKeyVersion())) {
            requestBuilder.addHeader(headerNames.secretVersion(), account.getSecretKeyVersion());
        }
        requestBuilder.addHeader(headerNames.timestamp(), signatureRequest.timestamp());
        requestBuilder.addHeader(headerNames.nonce(), signatureRequest.nonce());
        String sign = account.getSigner().sign(signatureRequest, account.getSecretKey());
        requestBuilder.addHeader(headerNames.sign(), sign);
        log.debug("api sign object = {} , sign = {}", request, sign);
        Response result = chain.proceed(requestBuilder.build());
        if (result.code() >= 400 && result.code() < 500) {
            log.debug("response sign Debug-Sign-Content = {}", result.headers().get(SignatureConstants.DEBUG_SIGN_CONTENT_HEADER_NAME));
            log.debug("response sign Debug-Sign-Query = {}", result.headers().get(SignatureConstants.DEBUG_SIGN_QUERY_HEADER_NAME));
        }
        return result;
    }

    private String getQueryString(HttpUrl url) {
        String queryString = url.url().getQuery();
        return StringUtils.hasText(queryString) ? queryString : null;
    }

    private boolean signRequireRequestBody(RequestBody requestBody) {
        if (requestBody == null || requestBody.contentType() == null) {
            // 当 reqeustBody content 为空时，content-type 为空，不参与签名
            return false;
        }
        MediaType mediaType = requestBody.contentType();
        String[] parts = mediaType.toString().split(";");
        return ApiSignatureRequest.signRequireRequestBody(parts[0]);
    }
}
