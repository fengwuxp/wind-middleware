package com.wind.script.spring;

import com.google.common.collect.ImmutableSet;
import com.wind.common.exception.AssertUtils;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 限制 spring 调用方法的范围
 *
 * @author wuxp
 * @date 2024-09-12 11:35
 **/
public final class WindSecurityReflectiveMethodResolver extends ReflectiveMethodResolver {

    private static final Set<String> DEFAULT_CLASSNAMES = ImmutableSet.of(
            String.class.getName(),
            Boolean.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Double.class.getName(),
            Float.class.getName(),
            Enum.class.getName(),
            "java.math.BigDecimal",
            "java.util.Date",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.util.Collection",
            "java.util.List",
            "java.util.Set",
            "java.util.Map",
            "java.util.HashSet",
            "java.util.ArrayList",
            "java.util.HashMap",
            "java.util.Arrays",
            "java.util.ImmutableCollections",
            "java.util.Objects",
            "java.lang.Class",
            "org.springframework.util.CollectionUtils",
            "org.springframework.util.StringUtils",
            "com.wind.common.util.StringMatchUtils",
            "com.wind.common.util.StringJoinSplitUtils",
            "com.wind.script.spring.SpringExpressionOperators",
            "com.wind.configcenter.core.ConfigFunctionRootObject"
    );

    private static final Set<String> SAFE_CLASSNAMES = new HashSet<>();

    private final Set<String> packages;

    public WindSecurityReflectiveMethodResolver() {
        this(loadPackages());
    }

    public WindSecurityReflectiveMethodResolver(Set<String> packages) {
        this(true, packages);
    }

    public WindSecurityReflectiveMethodResolver(boolean useDistance, Set<String> packages) {
        super(useDistance);
        this.packages = packages;
    }

    @Override
    protected Method[] getMethods(Class<?> type) {
        if (type.isEnum() || type.isArray() || type.isPrimitive()) {
            return super.getMethods(type);
        }
        String name = type.getName();
        AssertUtils.isTrue(packages.stream().anyMatch(name::startsWith), () -> "不允许调用 class name = " + type.getName() + " 的方法");
        return super.getMethods(type);
    }

    private static Set<String> loadPackages() {
        Set<String> result = new HashSet<>(DEFAULT_CLASSNAMES);
        result.addAll(SAFE_CLASSNAMES);
        return result;
    }

    public static void addSafeClassNames(String... classNames) {
        SAFE_CLASSNAMES.addAll(Arrays.asList(classNames));
    }

    public static void removeSafeClassNames(String... classNames) {
        Arrays.asList(classNames).forEach(SAFE_CLASSNAMES::remove);
    }
}
