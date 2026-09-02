package com.wind.client.rest;

import com.wind.common.WindConstants;
import com.wind.mask.masker.json.JsonStringMasker;
import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 统一打印请求日志。显式开启正文日志时，需要使用 {@link org.springframework.http.client.BufferingClientHttpRequestFactory}
 * 包装 {@link org.springframework.http.client.ClientHttpRequestFactory}，保证响应体可以多次读取。
 *
 * @author wuxp
 * @date 2026-01-22 14:15
 */
@Slf4j
public class ClientHttpRequestLoggingInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 在当前请求上下文中显式开启请求体和响应体日志的变量名
     */
    public static final String ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME = "api.request.log.print.enable";

    /**
     * 支持按 UTF-8 文本记录正文的媒体类型
     */
    private static final List<MediaType> LOGGABLE_BODY_MEDIA_TYPES = List.of(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_XML,
            MediaType.TEXT_HTML,
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_XML,
            MediaType.APPLICATION_ATOM_XML
    );

    /**
     * 请求体和响应体日志允许记录的最大字节数
     */
    private static final int MAX_BODY_LENGTH = 1024 * 2;

    /**
     * JSON 和表单正文中的敏感字段名称
     */
    private static final List<String> SENSITIVE_BODY_FIELD_NAMES = List.of(
            "authorization", "authentication", "auth", "cookie", "token",
            "accessToken", "access_token", "refreshToken", "refresh_token", "idToken", "id_token",
            "secret", "clientSecret", "client_secret", "credential", "credentials",
            "apiKey", "api_key", "api-key", "accessKey", "access_key", "access-key",
            "accessId", "access_id", "access-id", "username", "password", "passwd", "pwd",
            "signature", "sign", "nonce"
    );

    private static final List<String> SENSITIVE_BODY_JSON_PATHS = SENSITIVE_BODY_FIELD_NAMES.stream()
            .map(name -> "$..['" + name + "']")
            .toList();

    private static final JsonStringMasker BODY_MASKER = new JsonStringMasker();

    /**
     * 敏感请求头和响应头的脱敏占位内容
     */
    private static final String MASK_TEXT = "[****]";

    /**
     * 用于识别敏感请求头和响应头的名称片段
     */
    private static final List<String> SENSITIVE_HEADER_PARTS = List.of(
            "authorization", "authentication", "auth", "cookie", "token", "secret", "credential", "api-key", "apikey", "access-key", "accesskey",
            "access-id", "accessid", "username", "password", "signature", "sign", "nonce"
    );

    private final String env;

    public ClientHttpRequestLoggingInterceptor(String env) {
        this.env = env;
    }

    public ClientHttpRequestLoggingInterceptor() {
        this(WindConstants.UNKNOWN);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        return WindTracer.TRACER.call(() -> {
            StopWatch watch = new StopWatch();
            watch.start();
            ClientHttpResponse response = execution.execute(request, body);
            watch.stop();
            log(request, body, response, watch.getTotalTimeMillis());
            return response;
        });
    }

    private void log(HttpRequest request, byte[] body, ClientHttpResponse response, long elapsed) throws IOException {
        boolean enablePrintBody = Boolean.TRUE.equals(WindTracer.TRACER.getContextVariable(ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME));
        boolean is2xxSuccessful = response.getStatusCode().is2xxSuccessful();
        boolean printBody = enablePrintBody || !is2xxSuccessful;
        String requestBody = printBody ? readBody(request.getHeaders().getContentType(), body) : WindConstants.DASHED;
        String responseBody = printBody ? readResponseBody(response) : WindConstants.DASHED;
        String logFormat = """
                URI: {}
                Method: {}
                RequestHeaders: {}
                RequestBody: {}
                ResponseStatus: {}
                ResponseBody: {}
                ResponseHeaders: {}
                Elapsed: {}ms
                """;
        Object[] arguments = {
                request.getURI().getRawPath(), request.getMethod(), redactHeaders(request.getHeaders()), requestBody,
                response.getStatusCode(), responseBody, redactHeaders(response.getHeaders()), elapsed
        };
        if (enablePrintBody) {
            // 强制打印
            log.info(logFormat, arguments);
        } else {
            if (!Objects.equals(env, WindConstants.PROD)) {
                log.info(logFormat, arguments);
            } else if (response.getStatusCode().is2xxSuccessful()) {
                log.debug(logFormat, arguments);
            } else {
                log.error(logFormat, arguments);
            }
        }
    }


    private static String readResponseBody(ClientHttpResponse response) throws IOException {
        MediaType contentType = response.getHeaders().getContentType();
        if (shouldSkipBodyLog(contentType)) {
            return WindConstants.DASHED;
        }
        return readBody(contentType, response.getBody().readNBytes(MAX_BODY_LENGTH + 1));
    }

    private static String readBody(MediaType contentType, byte[] body) {
        if (shouldSkipBodyLog(contentType)) {
            return WindConstants.DASHED;
        }
        if (body.length > MAX_BODY_LENGTH) {
            return MASK_TEXT;
        }
        String content = new String(body, StandardCharsets.UTF_8);
        if (isJsonContentType(contentType)) {
            return maskJsonBody(content);
        }
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
            return maskFormBody(content);
        }
        return MASK_TEXT;
    }

    private static String maskJsonBody(String content) {
        try {
            return BODY_MASKER.mask(content, SENSITIVE_BODY_JSON_PATHS);
        } catch (RuntimeException ignored) {
            return MASK_TEXT;
        }
    }

    private static String maskFormBody(String content) {
        try {
            MultiValueMap<String, String> parameters = UriComponentsBuilder.newInstance().query(content).build().getQueryParams();
            StringJoiner result = new StringJoiner("&");
            parameters.forEach((name, values) -> values.forEach(value -> result.add(
                    name + "=" + (isSensitiveBodyFieldName(UriUtils.decode(name, StandardCharsets.UTF_8)) ?
                            MASK_TEXT : Objects.toString(value, WindConstants.EMPTY))
            )));
            return result.toString();
        } catch (RuntimeException ignored) {
            return MASK_TEXT;
        }
    }

    private static boolean shouldSkipBodyLog(MediaType contentType) {
        return contentType == null || (!isJsonContentType(contentType) &&
                LOGGABLE_BODY_MEDIA_TYPES.stream().noneMatch(mediaType -> mediaType.isCompatibleWith(contentType)));
    }

    private static boolean isJsonContentType(MediaType contentType) {
        return MediaType.APPLICATION_JSON.isCompatibleWith(contentType) ||
                (Objects.equals(MediaType.APPLICATION_JSON.getType(), contentType.getType()) && contentType.getSubtype().endsWith("+json"));
    }

    private static boolean isSensitiveBodyFieldName(String name) {
        return SENSITIVE_BODY_FIELD_NAMES.stream().anyMatch(fieldName -> fieldName.equalsIgnoreCase(name));
    }

    private static HttpHeaders redactHeaders(HttpHeaders headers) {
        HttpHeaders result = new HttpHeaders();
        headers.forEach((name, values) -> result.put(name, isSensitiveHeader(name) ? List.of(MASK_TEXT) : List.copyOf(values)));
        return result;
    }

    private static boolean isSensitiveHeader(String headerName) {
        String name = headerName.toLowerCase(Locale.ROOT);
        return SENSITIVE_HEADER_PARTS.stream().anyMatch(name::contains);
    }
}
