package com.wind.server.aop;

import com.wind.common.WindConstants;
import com.wind.common.WindHttpConstants;
import com.wind.context.injection.MethodParameterInjector;
import com.wind.script.auditlog.ScriptAuditLogRecorder;
import com.wind.web.util.HttpServletRequestUtils;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IDEMPOTENT_RESULT_ATTRIBUTE_NAME;

/**
 * 控制器方法拦截处理支持
 * 1. 参数注入
 * 2. 响应结果记录
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
            log.debug("请求方法= {}, 参数 = {}", getRequestMethodDesc(invocation.getMethod()), invocation.getArguments());
        }
        try {
            Object result = invocation.proceed();
            if (log.isDebugEnabled()) {
                log.debug("请求方法 = {}, 响应 = {}", getRequestMethodDesc(invocation.getMethod()), result);
            }
            recordOperationLog(invocation, result, null);
            // 设置幂等结果
            HttpServletRequestUtils.requireContextRequest().setAttribute(HTTP_REQUEST_IDEMPOTENT_RESULT_ATTRIBUTE_NAME, result);
            return result;
        } catch (Throwable throwable) {
            log.error("请求方法 = {} 异常, 参数 = {}, message = {}", invocation.getMethod(), invocation.getArguments(), throwable.getMessage(), throwable);
            HttpServletRequestUtils.requireContextRequest().setAttribute(WindHttpConstants.getRequestExceptionLogOutputMarkerAttributeName(throwable), true);
            recordOperationLog(invocation, null, throwable);
            throw throwable;
        }
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
