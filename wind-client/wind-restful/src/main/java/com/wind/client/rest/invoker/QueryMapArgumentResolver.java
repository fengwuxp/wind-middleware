package com.wind.client.rest.invoker;

import com.wind.client.rest.annotation.SpringQueryMap;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查询参数解析器
 *
 * @author wuxp
 * @date 2026-02-26 13:43
 * @see SpringQueryMap
 **/
public class QueryMapArgumentResolver implements HttpServiceArgumentResolver {

    private final Map<Class<?>, PropertyDescriptor[]> propertyDescriptorCache = new ConcurrentHashMap<>();

    @Override
    public boolean resolve(Object argument, @NonNull MethodParameter parameter, HttpRequestValues.@NonNull Builder requestValues) {
        if (argument == null) {
            // ignore null
            return true;
        }
        if (!parameter.hasParameterAnnotation(SpringQueryMap.class)) {
            return false;
        }

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        try {
            PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(argument.getClass());
            for (PropertyDescriptor pd : propertyDescriptors) {
                if ("class".equals(pd.getName()) || isTransient(pd, argument.getClass())) {
                    // skip transient properties
                    continue;
                }
                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    Object value = readMethod.invoke(argument);
                    if (value != null) {
                        // For simplicity, convert to string using toString()
                        // Could be extended to handle collections, etc.
                        queryParams.add(pd.getName(), value.toString());
                    }
                }
            }
        } catch (Exception e) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "Failed to extract query parameters from " + argument.getClass(), e);
        }

        // Add each parameter to the request
        queryParams.forEach((name, values) -> {
            for (String value : values) {
                requestValues.addRequestParameter(name, value);
            }
        });
        return true;
    }

    private PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
        return propertyDescriptorCache.computeIfAbsent(clazz, c -> {
            try {
                return Introspector.getBeanInfo(c).getPropertyDescriptors();
            } catch (IntrospectionException e) {
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "Failed to get property descriptors for: " + c, e);
            }
        });
    }

    private boolean isTransient(PropertyDescriptor pd, Class<?> beanClass) {
        // Check if the getter method is annotated with @Transient (any annotation named "Transient")
        Method readMethod = pd.getReadMethod();
        if (readMethod != null) {
            for (Annotation ann : readMethod.getAnnotations()) {
                if (Objects.equals(ann.annotationType(), Transient.class)) {
                    return true;
                }
            }
        }

        // Check if the corresponding field is transient
        String fieldName = pd.getName();
        try {
            Field field = beanClass.getDeclaredField(fieldName);
            if (Modifier.isTransient(field.getModifiers())) {
                return true;
            }
        } catch (NoSuchFieldException e) {
            // Field might be in superclass, ignore this simple check
            // Could be enhanced to search superclasses
        }
        return false;
    }
}
