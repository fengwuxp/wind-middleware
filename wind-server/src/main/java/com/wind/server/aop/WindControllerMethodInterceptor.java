package com.wind.server.aop;

import com.wind.common.WindConstants;
import com.wind.common.WindHttpConstants;
import com.wind.context.injection.MethodParameterInjector;
import com.wind.common.exception.WindIdempotentException;
import com.wind.middleware.idempotent.WindIdempotentExecuteUtils;
import com.wind.script.auditlog.ScriptAuditLogRecorder;
import com.wind.web.util.HttpServletRequestUtils;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IDEMPOTENT_EXECUTE_PREFIX;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IDEMPOTENT_HEADER_NAME;

/**
 * 控制器方法拦截处理支持
 * 1. 参数注入
 * 2. http 请求幂等处理 {@link WindHttpConstants#HTTP_REQUEST_IDEMPOTENT_HEADER_NAME}
 * 3. 响应结果记录
 *
 * @author wuxp
 * @date 2023-10-25 10:00
 **/
@Slf4j
public record WindControllerMethodInterceptor(ScriptAuditLogRecorder auditLogRecorder, MethodParameterInjector methodParameterInjector) implements MethodInterceptor {

    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        // 参数注入
        methodParameterInjector.inject(invocation.getMethod(), invocation.getArguments());
        if (log.isDebugEnabled()) {
            log.debug("请求方法：{}，参数: {}", getRequestMethodDesc(invocation.getMethod()), invocation.getArguments());
        }
        try {
            String idempotentKey = HttpServletRequestUtils.getHeader(HTTP_REQUEST_IDEMPOTENT_HEADER_NAME);
            if (StringUtils.hasText(idempotentKey)) {
                // 如果幂等 key 存在，则执行幂等处理
                return WindIdempotentExecuteUtils.executeWithThrows(HTTP_REQUEST_IDEMPOTENT_EXECUTE_PREFIX + idempotentKey, () -> invokeControllerMethod(invocation));
            } else {
                return invokeControllerMethod(invocation);
            }
        } catch (Throwable throwable) {
            Throwable th = throwable;
            if (th instanceof WindIdempotentException) {
                // 如果是幂等执行异常，则获取真正的异常
                th = th.getCause();
            }
            log.error("请求方法 = {} 异常, 参数 = {}, message = {}", invocation.getMethod(), invocation.getArguments(), th.getMessage(), th);
            HttpServletRequestUtils.requireContextRequest().setAttribute(WindHttpConstants.getRequestExceptionLogOutputMarkerAttributeName(th), true);
            recordOperationLog(invocation, null, th);
            throw th;
        }
    }

    private Object invokeControllerMethod(MethodInvocation invocation) throws Throwable {
        Object result = invocation.proceed();
        if (log.isDebugEnabled()) {
            log.debug("请求方法 = {}, 响应 = {}", getRequestMethodDesc(invocation.getMethod()), result);
        }
        recordOperationLog(invocation, result, null);
        return result;
    }

    private String getRequestMethodDesc(Method method) {
        return String.format("%s%s%s", method.getDeclaringClass().getName(), WindConstants.SHARP, method.getName());
    }

    private void recordOperationLog(MethodInvocation invocation, Object result, Throwable throwable) {
        if (auditLogRecorder == null) {
            return;
        }
        auditLogRecorder.recordLog(invocation.getArguments(), result, invocation.getMethod(), throwable);
    }
}
