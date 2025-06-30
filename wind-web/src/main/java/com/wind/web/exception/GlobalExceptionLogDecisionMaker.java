package com.wind.web.exception;

import com.wind.common.WindHttpConstants;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.ExceptionLogLevel;
import com.wind.web.util.HttpServletRequestUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * 全局 error 日志输出决策者
 *
 * @author wuxp
 * @date 2024-12-23 09:59
 **/
public final class GlobalExceptionLogDecisionMaker {

    private static final AtomicReference<Predicate<Throwable>> SHOULD_ERROR_LOG =
            // spring security authentication 相关异常忽略打印错误日志
            new AtomicReference<>(throwable -> !isSpringSecurityAuthenticationException(throwable));

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
        if (throwable == null) {
            return false;
        }
        Class<? extends Throwable> throwableClass = throwable.getClass();
        return throwableClass.getName().startsWith("org.springframework.security.authentication");
    }
}
