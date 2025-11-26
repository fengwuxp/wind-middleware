package com.wind.server.web.restful;

import com.wind.common.annotations.I18n;
import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.common.query.supports.Pagination;
import com.wind.common.util.WindReflectUtils;
import com.wind.script.spring.SpringExpressionEvaluator;
import com.wind.server.web.supports.ApiResp;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * 响应之前尝试处理结果对象中被 {@link I18n}注解标记的字段
 * <p>
 *
 * @author wuxp
 * @date 2024-07-11 15:43
 **/
public abstract class AbstractI18nResponseBodyAdvice implements ResponseBodyAdvice<@NonNull Object> {

    private static final Field[] EMPTY = new Field[0];

    private final Locale defaultLocal;

    protected AbstractI18nResponseBodyAdvice(Locale defaultLocal) {
        this.defaultLocal = defaultLocal;
    }

    protected AbstractI18nResponseBodyAdvice() {
        this(Locale.CHINA);
    }

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class converterType) {
        if (Objects.equals(defaultLocal.getLanguage(), SpringI18nMessageUtils.requireLocale().getLanguage())) {
            return false;
        }
        return Objects.requireNonNull(returnType.getMethod()).getReturnType().isAssignableFrom(ApiResp.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType, @NonNull Class selectedConverterType,
                                  @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (body instanceof ApiResp<?> resp) {
            handleReturnValueI18n(resp.getData());
        }
        return body;
    }

    @NonNull
    protected abstract String getI18nMessage(String key);

    private void handleReturnValueI18n(Object result) {
        if (result instanceof Pagination) {
            // 分页对象
            ((Pagination<?>) result).getRecords().forEach(this::fillI18nMessages);
        } else if (result instanceof Collection) {
            // 集合对象
            ((Collection<?>) result).forEach(this::fillI18nMessages);
        } else {
            fillI18nMessages(result);
        }
    }

    private void fillI18nMessages(Object val) {
        if (val == null) {
            return;
        }
        Field[] fields = this.parseI18nFields(val.getClass());
        for (Field field : fields) {
            if (field.getType() == String.class) {
                fillI18nMessage(val, field);
            } else {
                // 字段为复杂对象
                handleReturnValueI18n(WindReflectUtils.getFieldValue(field, val));
            }
        }
    }

    private void fillI18nMessage(Object val, Field field) {
        I18n annotation = field.getAnnotation(I18n.class);
        String name = annotation.name();
        String messageKey;
        if (StringUtils.hasText(name)) {
            // 通过 spring expression 解析
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable(I18n.OBJECT_VARIABLE_NAME, val);
            messageKey = SpringExpressionEvaluator.TEMPLATE.eval(name, context);
        } else {
            messageKey = WindReflectUtils.getFieldValue(field, val);
        }
        String i18nMessage = getI18nMessage(messageKey);
        if (StringUtils.hasText(i18nMessage)) {
            WindReflectUtils.setFieldValue(field, val, i18nMessage);
        }
    }

    private Field[] parseI18nFields(Class<?> clazz) {
        if ((clazz.isAnnotationPresent(I18n.class) || clazz.getSuperclass().isAnnotationPresent(I18n.class))) {
            return WindReflectUtils.findFields(clazz, I18n.class);
        }
        return EMPTY;
    }

}
