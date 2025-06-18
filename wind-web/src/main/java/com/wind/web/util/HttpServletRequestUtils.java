package com.wind.web.util;

import com.wind.common.exception.AssertUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
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

    private HttpServletRequestUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static HttpServletRequest requireContextRequest() {
        HttpServletRequest result = getContextRequestOfNullable();
        AssertUtils.notNull(result, "not currently in web servlet context");
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

    public static <T> T requireRequestAttribute(@NotBlank String name) {
        return requireRequestAttribute(name, requireContextRequest());
    }


    public static <T> T requireRequestAttribute(@NotBlank String name, @NotNull HttpServletRequest request) {
        T result = getRequestAttribute(name, request);
        AssertUtils.notNull(result, () -> String.format("attribute = %s must not null", name));
        return result;
    }

    @Nullable
    public static <T> T getRequestAttribute(@NotBlank String name) {
        return getRequestAttribute(name, requireContextRequest());
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getRequestAttribute(String name, HttpServletRequest request) {
        return (T) request.getAttribute(name);
    }
}
