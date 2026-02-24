package com.wind.web.util;

import com.wind.common.exception.AssertUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从上下文中获取 http servlet request
 *
 * @author wuxp
 * @date 2023-09-28 07:32
 * @see org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter;
 **/
public final class HttpServletRequestUtils {

    private static final String NOT_CURRENTLY_IN_WEB_SERVLET_CONTEXT = "not currently in web servlet context";

    private HttpServletRequestUtils() {
        throw new AssertionError();
    }

    @NonNull
    public static HttpServletRequest requireContextRequest() {
        HttpServletRequest result = getContextRequestOfNullable();
        AssertUtils.notNull(result, NOT_CURRENTLY_IN_WEB_SERVLET_CONTEXT);
        return result;
    }

    @Nullable
    public static HttpServletRequest getContextRequestOfNullable() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    public static <T> T requireRequestAttribute(@NonNull String name) {
        return requireRequestAttribute(name, requireContextRequest());
    }

    public static <T> T requireRequestAttribute(@NonNull String name, @NonNull HttpServletRequest request) {
        T result = getRequestAttribute(name, request);
        AssertUtils.notNull(result, () -> String.format("attribute = %s must not null", name));
        return result;
    }

    @Nullable
    public static String getHeader(@NonNull String headerName) {
        return getHeader(headerName, requireContextRequest());
    }

    public static String getHeader(@NonNull String headerName, String defaultValue) {
        return getHeader(headerName, defaultValue, requireContextRequest());
    }

    public static String getHeader(@NonNull String headerName, String defaultValue, @NonNull HttpServletRequest request) {
        String result = getHeader(headerName, request);
        return result == null ? defaultValue : result;
    }

    /**
     * 获取请求头
     *
     * @param headerName 请求头名称
     * @return 请求头值
     */
    @Nullable
    public static String getHeader(@NonNull String headerName, @NonNull HttpServletRequest request) {
        String val = request.getHeader(headerName);
        if (val == null) {
            return null;
        }
        return val.replace("\"", "").trim();
    }

    @Nullable
    public static <T> T getRequestAttribute(@NonNull String name) {
        return getRequestAttribute(name, requireContextRequest());
    }

    @Nullable
    public static <T> T getRequestAttribute(@NonNull String name, T defaultValue) {
        return getRequestAttribute(name, defaultValue, requireContextRequest());
    }

    @Nullable
    public static <T> T getRequestAttribute(@NonNull String name, @Nullable T defaultValue, @NonNull HttpServletRequest request) {
        Object result = getRequestAttribute(name, request);
        return result == null ? defaultValue : (T) result;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getRequestAttribute(String name, HttpServletRequest request) {
        return (T) request.getAttribute(name);
    }

    @NonNull
    public static HttpServletResponse requireContextResponse() {
        HttpServletResponse result = getContextResponseOfNullable();
        AssertUtils.notNull(result, NOT_CURRENTLY_IN_WEB_SERVLET_CONTEXT);
        return result;
    }

    @Nullable
    public static HttpServletResponse getContextResponseOfNullable() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getResponse();
    }
}
