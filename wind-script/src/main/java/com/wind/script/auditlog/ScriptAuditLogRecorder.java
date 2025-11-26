package com.wind.script.auditlog;

import com.wind.common.WindConstants;
import com.wind.common.annotations.VisibleForTesting;
import com.wind.script.spring.SpringExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 通过 Spring Expression 构建审计日志
 *
 * @author wuxp
 * @date 2023-09-23 06:26
 **/
@Slf4j
public class ScriptAuditLogRecorder {

    /**
     * 日志备注属性名称
     */
    public static final String AUDIT_LOG_REMARK_ATTRIBUTE_NAME = "AUDIT_LOG_REMARK";

    /**
     * 方法参数列表变量
     */
    private static final String ARGS_VARIABLE_NAME = "args";

    /**
     * 方法执行结果响应变量（原始值）
     */
    private static final String RESULT_VARIABLE_NAME = "result";

    /**
     * 方法执行结果响应变量（解析值）
     */
    private static final String RESULT_RESOLVE_VARIABLE_NAME = "resolveResult";

    /**
     * spring 的方法参数发现者
     * 编译时需要开启保留方法参数名称
     */
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final AuditLogRecorder auditLogRecorder;

    private final Supplier<Map<String, Object>> contextVariablesSupplier;

    protected ScriptAuditLogRecorder(AuditLogRecorder auditLogRecorder) {
        this(auditLogRecorder, Collections::emptyMap);
    }

    protected ScriptAuditLogRecorder(AuditLogRecorder auditLogRecorder, Supplier<Map<String, Object>> contextVariablesSupplier) {
        this.auditLogRecorder = auditLogRecorder;
        this.contextVariablesSupplier = contextVariablesSupplier;
    }


    /**
     * 记录方法操作日志
     *
     * @param arguments         方法参数
     * @param methodReturnValue 方法返回值
     * @param method            方法对象
     * @param throwable         执行抛出的异常，没有则为空
     */
    public void recordLog(Object[] arguments, @Nullable Object methodReturnValue, Method method, @Nullable Throwable throwable) {
        AuditLogContent content = buildLogContent(arguments, methodReturnValue, method, throwable);
        if (content == null) {
            return;
        }
        auditLogRecorder.write(content, throwable);
    }

    @VisibleForTesting
    @Nullable
    AuditLogContent buildLogContent(Object[] arguments, @Nullable Object methodReturnValue, Method method, Throwable throwable) {
        AuditLog auditLog = method == null ? null : AnnotationUtils.getAnnotation(method, AuditLog.class);
        if (auditLog == null) {
            return null;
        }
        Assert.hasLength(auditLog.value(), "AuditLog#value must not empty");
        Map<String, Object> variables = buildEvaluationVariables(arguments, methodReturnValue, method.getParameters());
        EvaluationContext evaluationContext = new StandardEvaluationContext();
        variables.forEach(evaluationContext::setVariable);
        String remark = auditLog.remark();
        return AuditLogContent.builder()
                .args(arguments)
                .resultValue(methodReturnValue)
                .log(evalLog(auditLog.value(), evaluationContext, throwable))
                .remark(StringUtils.hasLength(remark) ? evalLog(remark, evaluationContext, throwable) :
                        (String) variables.get(AUDIT_LOG_REMARK_ATTRIBUTE_NAME))
                .group(auditLog.group())
                .type(auditLog.resourceType())
                .operation(auditLog.operation())
                .resourceId(evalResourceId(auditLog.resourceId(), evaluationContext))
                .contextVariables(Collections.unmodifiableMap(variables))
                .throwable(throwable)
                .build();
    }

    protected String evalLog(String expression, EvaluationContext context, Throwable throwable) {
        if (throwable == null) {
            try {
                return SpringExpressionEvaluator.TEMPLATE.eval(expression, context);
            } catch (Exception exception) {
                log.error("eval audit log error, expression = {}", expression, exception);
                return WindConstants.EMPTY;
            }
        }
        String message = throwable.getMessage();
        return StringUtils.hasLength(message) ? message : "Unknown Error";
    }

    @Nullable
    private Object evalResourceId(String expression, EvaluationContext evaluationContext) {
        if (StringUtils.hasLength(expression)) {
            try {
                return SpringExpressionEvaluator.DEFAULT.eval(expression, evaluationContext);
            } catch (Exception exception) {
                log.error("eval resource id error, expression = {}", expression, exception);
            }
        }
        return null;
    }

    /**
     * @param arguments         请求参数
     * @param methodReturnValue 方法执行结果
     * @param parameters        执行方法的参数类型列表
     * @return spring expression 执行上下文
     */
    private Map<String, Object> buildEvaluationVariables(Object[] arguments, Object methodReturnValue, Parameter[] parameters) {
        Map<String, Object> result = new HashMap<>(contextVariablesSupplier.get());
        if (ObjectUtils.isEmpty(arguments)) {
            return result;
        }
        // 填充请求参数
        int length = parameters.length;
        for (int i = 0; i < length; i++) {
            Parameter parameter = parameters[i];
            String name = getParameterName(parameter);
            result.put(name, arguments[i]);
        }
        result.put(ARGS_VARIABLE_NAME, arguments);

        // 填充方法执行结果
        if (methodReturnValue != null) {
            result.put(RESULT_VARIABLE_NAME, methodReturnValue);
            result.put(RESULT_RESOLVE_VARIABLE_NAME, resolveMethodReturnValue(methodReturnValue));
        }
        return result;
    }

    /**
     * 在控制层有统一响应对象时，可以重载改方法返回真正需要的返回值
     *
     * @param methodReturnValue 方法返回值
     * @return 解析后的方法返回值
     */
    protected Object resolveMethodReturnValue(Object methodReturnValue) {
        return methodReturnValue;
    }

    /**
     * 获取参数的真实名称
     *
     * @param parameter 方法参数对象
     * @return 参数的名称
     */
    private String getParameterName(Parameter parameter) {
        Method method = (Method) parameter.getDeclaringExecutable();
        int index = Arrays.asList(method.getParameters()).indexOf(parameter);
        try {
            return Objects.requireNonNull(PARAMETER_NAME_DISCOVERER.getParameterNames(method))[index];
        } catch (Exception e) {
            log.warn("获取方法{}的参数名称列表失败：{}", method, e.getMessage(), e);
        }
        return parameter.getName();
    }


}
