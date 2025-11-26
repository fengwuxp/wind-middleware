package com.wind.web.exception;

import com.wind.common.WindHttpConstants;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.ExceptionLogLevel;
import com.wind.web.util.HttpServletRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ClassUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * 全局 error 日志输出决策者
 *
 * @author wuxp
 * @date 2024-12-23 09:59
 **/
@Slf4j
public final class GlobalExceptionLogDecisionMaker {

    private static final AtomicReference<Predicate<Throwable>> SHOULD_ERROR_LOG =
            // spring security authentication 相关异常忽略打印错误日志
            new AtomicReference<>(throwable -> !isSpringSecurityAuthenticationException(throwable));

    /**
     * spring security 认证异常
     */
    @Nullable
    private static final Class<?> SECURITY_AUTHENTICATION_EXCEPTION = loadClass("org.springframework.security.core.AuthenticationException");

    /**
     * spring security 访问异常
     */
    @Nullable
    private static final Class<?> SECURITY_ACCESS_DENIED_EXCEPTION = loadClass("org.springframework.security.access.AccessDeniedException");


    private GlobalExceptionLogDecisionMaker() {
        throw new AssertionError();
    }

    /**
     * 该异常是否需要输出错误日志
     *
     * @param throwable 异常
     * @return if true 需要打印 error 日志
     */
    public static boolean requiresPrintErrorLog(Throwable throwable) {
        if (isNonePrintErrorLog(throwable)) {
            if (throwable instanceof BaseException baseException) {
                return Objects.equals(baseException.getLogLevel(), ExceptionLogLevel.ERROR);
            }
            return SHOULD_ERROR_LOG.get().test(throwable);
        }
        return false;
    }

    /**
     * 是否未输出过错误日志
     *
     * @param throwable 异常
     * @return if true 该改异常在请求上下文中没有输出过错误日志
     */
    private static boolean isNonePrintErrorLog(Throwable throwable) {
        return HttpServletRequestUtils.getRequestAttribute(WindHttpConstants.getRequestExceptionLogOutputMarkerAttributeName(throwable)) == null;
    }

    public static void configure(Predicate<Throwable> shouldErrorLog) {
        SHOULD_ERROR_LOG.set(shouldErrorLog);
    }

    public static boolean isSpringSecurityAuthenticationException(Throwable throwable) {
        if (throwable == null || SECURITY_AUTHENTICATION_EXCEPTION == null) {
            return false;
        }
        return SECURITY_AUTHENTICATION_EXCEPTION.isInstance(throwable);
    }

    public static boolean isSpringSecurityAccessDeniedException(Throwable throwable) {
        if (throwable == null || SECURITY_ACCESS_DENIED_EXCEPTION == null) {
            return false;
        }
        return SECURITY_ACCESS_DENIED_EXCEPTION.isInstance(throwable);
    }

    public static boolean isSpringSecurityException(Throwable throwable) {
        return isSpringSecurityAuthenticationException(throwable) || isSpringSecurityAccessDeniedException(throwable);
    }

    @Nullable
    private static Class<?> loadClass(String className) {
        try {
            return ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());
        } catch (IllegalArgumentException exception) {
            log.error("load class error, message = {}", className, exception);
            return null;
        }
    }
}
