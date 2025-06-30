package com.wind.context.injection;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.util.WindReflectUtils;
import com.wind.context.variable.annotations.ContextVariable;
import com.wind.script.spring.SpringExpressionEvaluator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 基于{@link ContextVariable}注解标记的方法参数注入者
 *
 * @author wuxp
 * @date 2023-10-25 07:18
 **/
@AllArgsConstructor
public class ContextAnnotationMethodParameterInjector implements MethodParameterInjector {

    private static final SimpleParameterInjectionDescriptor[] EMPTY = new SimpleParameterInjectionDescriptor[0];

    /**
     * 方法参数注入原信息
     *
     * @key method
     * @value 注入字段的元信息
     */
    private final Map<Method, AbstractInjectionDescriptor[]> descriptors = new ConcurrentReferenceHashMap<>(1024);

    private final ContextVariableProvider contextVariableProvider;

    private final Predicate<Class<?>> injectMatcher;

    public ContextAnnotationMethodParameterInjector(ContextVariableProvider contextVariableProvider, Set<String> injectObjectBasePackages) {
        this.contextVariableProvider = contextVariableProvider;
        this.injectMatcher = aClass -> {
            String name = aClass.getName();
            return injectObjectBasePackages.stream().anyMatch(name::startsWith);
        };
    }

    /**
     * 注入方法参数中被注解标记的的字段或参数
     *
     * @param method    方法
     * @param arguments 方法参数
     */
    @Override
    public void inject(Method method, Object[] arguments) {
        AbstractInjectionDescriptor[] injectionDescriptors = descriptors.computeIfAbsent(method, this::parseParameterInjectionDescriptors);
        if (ObjectUtils.isEmpty(injectionDescriptors)) {
            return;
        }
        Map<String, Object> variables = contextVariableProvider.get();
        for (AbstractInjectionDescriptor descriptor : injectionDescriptors) {
            arguments[descriptor.getParameterIndex()] = descriptor.getArg(arguments[descriptor.getParameterIndex()], variables);
        }
    }

    private AbstractInjectionDescriptor[] parseParameterInjectionDescriptors(Method method) {
        List<AbstractInjectionDescriptor> result = new ArrayList<>(4);
        Parameter[] parameters = method.getParameters();
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Parameter parameter = parameters[i];
            if (isSimpleType(parameterType) || ClassUtils.isPrimitiveArray(parameterType)) {
                ContextVariable annotation = AnnotatedElementUtils.getMergedAnnotation(parameter, ContextVariable.class);
                if (annotation != null) {
                    result.add(new SimpleParameterInjectionDescriptor(annotation, parameter, i));
                }
            } else if (parameterType.isArray()) {
                // 数组
                ObjectArrayParameterInjectionDescriptor descriptor = new ObjectArrayParameterInjectionDescriptor(parameterType.getComponentType(),
                        parameter, i);
                if (!descriptor.getFieldAnnotations().isEmpty()) {
                    result.add(descriptor);
                }

            } else if (isCollection(parameterType)) {
                // 集合对象
                Type genericType = parameter.getParameterizedType();
                if (genericType instanceof ParameterizedType parameterizedType) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    AssertUtils.isTrue(actualTypeArguments.length >= 1, String.format("method = %s, parameter = %s actualTypeArguments size le 1",
                            method.getName(), parameter.getName()));
                    Class<?> actualTypeArgument = (Class<?>) actualTypeArguments[0];
                    ObjectCollectionParameterInjectionDescriptor descriptor = new ObjectCollectionParameterInjectionDescriptor(actualTypeArgument,
                            parameter, i);
                    if (!descriptor.getFieldAnnotations().isEmpty()) {
                        result.add(descriptor);
                    }
                } else {
                    throw new BaseException(DefaultExceptionCode.COMMON_ERROR, String.format("find method = %s, parameter = %s parameterizedType " +
                            "error", method.getName(), parameter.getName()));
                }
            } else {
                if (isAllowInject(parameterType)) {
                    ObjectParameterInjectionDescriptor descriptor = new ObjectParameterInjectionDescriptor(parameterType, parameter, i);
                    if (!descriptor.getFieldAnnotations().isEmpty()) {
                        result.add(descriptor);
                    }
                }
            }
        }

        return result.isEmpty() ? EMPTY : result.toArray(new AbstractInjectionDescriptor[0]);
    }


    private boolean isSimpleType(Class<?> clazz) {
        return ClassUtils.isPrimitiveOrWrapper(clazz) ||
                Objects.equals(String.class, clazz) ||
                clazz.isEnum();
    }

    private boolean isCollection(Class<?> clazz) {
        return ClassUtils.isAssignable(Collection.class, clazz);
    }

    private boolean isAllowInject(Class<?> clazz) {
        return injectMatcher.test(clazz);
    }

    @Getter
    @AllArgsConstructor
    abstract static class AbstractInjectionDescriptor {

        /**
         * 参数
         */
        private final Parameter parameter;

        /**
         * 在方法参数中的索引
         */
        private final int parameterIndex;

        /**
         * 参数注入
         *
         * @param value     原始参数
         * @param variables 上下文变量
         * @return 进过注入处理或计算的参数
         */
        protected abstract Object getArg(@Nullable Object value, Map<String, Object> variables);

    }

    /**
     * 简单的参数类型
     */
    private static final class SimpleParameterInjectionDescriptor extends AbstractInjectionDescriptor {

        private final ContextVariable annotation;

        public SimpleParameterInjectionDescriptor(ContextVariable annotation, Parameter parameter, int parameterIndex) {
            super(parameter, parameterIndex);
            this.annotation = annotation;
        }

        @Override
        protected Object getArg(Object value, Map<String, Object> variables) {
            Object result = evalVariable(annotation, variables);
            if (annotation.override()) {
                // 强制覆盖
                return result;
            } else {
                // 空则覆盖
                return value == null ? result : value;
            }
        }
    }

    /**
     * 对象
     */
    @Getter
    private static class ObjectParameterInjectionDescriptor extends AbstractInjectionDescriptor {

        private final Map<Field, ContextVariable> fieldAnnotations;

        public ObjectParameterInjectionDescriptor(Class<?> clazz, Parameter parameter, int parameterIndex) {
            super(parameter, parameterIndex);
            this.fieldAnnotations = this.parseFields(clazz);
        }

        @Override
        protected Object getArg(Object value, Map<String, Object> variables) {
            fieldAnnotations.forEach((field, annotation) -> injectField(field, annotation, value, variables));
            return value;
        }

        private Map<Field, ContextVariable> parseFields(Class<?> clazz) {
            Map<Field, ContextVariable> result = new HashMap<>();
            Arrays.stream(WindReflectUtils.getFields(clazz)).forEach(field -> {
                ContextVariable annotation = AnnotatedElementUtils.getMergedAnnotation(field, ContextVariable.class);
                if (annotation != null) {
                    result.put(field, annotation);
                }
            });
            return result;
        }
    }

    /**
     * 对象数组
     */
    @Getter
    private static final class ObjectArrayParameterInjectionDescriptor extends ObjectParameterInjectionDescriptor {

        public ObjectArrayParameterInjectionDescriptor(Class<?> clazz, Parameter parameter, int parameterIndex) {
            super(clazz, parameter, parameterIndex);
        }

        @Override
        protected Object getArg(Object value, Map<String, Object> variables) {
            Object[] values = (Object[]) value;
            for (Object v : values) {
                getFieldAnnotations().forEach((field, annotation) -> injectField(field, annotation, v, variables));
            }
            return values;
        }

    }


    /**
     * 集合对象
     */
    private static final class ObjectCollectionParameterInjectionDescriptor extends ObjectParameterInjectionDescriptor {

        public ObjectCollectionParameterInjectionDescriptor(Class<?> componentType, Parameter parameter, int parameterIndex) {
            super(componentType, parameter, parameterIndex);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Object getArg(Object value, Map<String, Object> variables) {
            for (Object v : (Collection<Object>) value) {
                getFieldAnnotations().forEach((field, annotation) -> injectField(field, annotation, v, variables));
            }
            return value;
        }
    }

    static Object evalVariable(ContextVariable annotation, Map<String, Object> contextVariables) {
        if (StringUtils.hasLength(annotation.name())) {
            return contextVariables.get(annotation.name());
        }
        if (StringUtils.hasLength(annotation.expression())) {
            return SpringExpressionEvaluator.DEFAULT.eval(annotation.expression(), contextVariables);
        }
        throw BaseException.common("invalid annotation tag, name and expression attribute is empty");
    }

    static void injectField(Field field, ContextVariable annotation, Object object, Map<String, Object> contextVariables) {
        try {
            Object value = evalVariable(annotation, contextVariables);
            if (annotation.override()) {
                // 强制覆盖
                field.set(object, value);
            } else {
                if (field.get(object) == null) {
                    // 空则覆盖
                    field.set(object, value);
                }
            }
        } catch (IllegalAccessException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "inject filed error", exception);
        }
    }

}
