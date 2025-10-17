package com.wind.common;

/**
 * http 相关常量
 *
 * @author wuxp
 * @date 2023-10-18 22:08
 **/
public final class WindHttpConstants {

    private WindHttpConstants() {
        throw new AssertionError();
    }

    /**
     * http request 来源 ip
     */
    public static final String HTTP_REQUEST_IP_ATTRIBUTE_NAME = "requestSourceIp";

    /**
     * http request 来源 host
     */
    public static final String HTTP_REQUEST_HOST_ATTRIBUTE_NAME = "requestSourceHost";

    /**
     * api 请求账号
     */
    public static final String API_SECRET_ACCOUNT_ATTRIBUTE_NAME = "Wind-Attribute-Api-Secret-Account";

    /**
     * http request Host header name
     */
    public static final String HTTP_HOST_HEADER_NAME = "Host";

    /**
     * http request User-Agent header name
     */
    public static final String HTTP_USER_AGENT_HEADER_NAME = "User-Agent";

    /**
     * http request Device-Id  header name
     */
    public static final String HTTP_REQUEST_CLIENT_ID_HEADER_NAME = "Wind-Device-Id";

    /**
     * http request Idempotent header name
     */
    public static final String HTTP_REQUEST_IDEMPOTENT_HEADER_NAME = "Wind-Idempotent-Key";

    public static final String HTTP_REQUEST_IDEMPOTENT_RESULT_ATTRIBUTE_NAME = "http-request-idempotent-result";

    /**
     * http request Idempotent execute prefix, 业务侧可以根据该前缀进行额外的处理
     */
    public static final String HTTP_REQUEST_IDEMPOTENT_EXECUTE_PREFIX = "Wind-Http@";

    /**
     * 匹配所有路径的 Ant pattern
     */
    public static final String ALL_PATH_ANT_PATTERN = "/**";

    /**
     * 请求异常日志输出标记
     */
    public static String getRequestExceptionLogOutputMarkerAttributeName(Throwable throwable) {
        return String.format("%s-Log-Output-Marker", throwable.getClass().getName());
    }
}
