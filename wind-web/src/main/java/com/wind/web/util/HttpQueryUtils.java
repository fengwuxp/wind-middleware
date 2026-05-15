package com.wind.web.util;

import com.wind.common.WindConstants;
import com.wind.common.util.WindReflectUtils;
import com.wind.core.ProtocolValueFormatter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * http 查询参数工具
 *
 * @author wuxp
 * @date 2024-02-21 15:57
 **/
public final class HttpQueryUtils {

    private static final Set<Class<? extends Annotation>> ANNOTATION_CLASSES = LinkedHashSet.newLinkedHashSet(4);

    static {
        for (String annotationClassName : List.of(
                "com.fasterxml.jackson.annotation.JsonProperty",
                "com.fasterxml.jackson.annotation.JsonAlias",
                "com.alibaba.fastjson.annotation.JSONField",
                "com.alibaba.fastjson2.annotation.JSONField"
        )) {
            try {
                Class clazz = ClassUtils.forName(annotationClassName, HttpQueryUtils.class.getClassLoader());
                ANNOTATION_CLASSES.add(clazz);
            } catch (ClassNotFoundException ignore) {
                // ignore
            }
        }


    }

    private HttpQueryUtils() {
        throw new AssertionError();
    }

    @NonNull
    public static MultiValueMap<String, String> parseQueryParamsFormUri(String uri) {
        if (StringUtils.hasText(uri)) {
            return UriComponentsBuilder.fromUriString(UriUtils.decode(uri, StandardCharsets.UTF_8))
                    .build()
                    .getQueryParams();
        }
        return new LinkedMultiValueMap<>();
    }

    @NonNull
    public static MultiValueMap<String, String> parseQueryParams(String queryString) {
        if (StringUtils.hasText(queryString)) {
            return parseQueryParamsFormUri(String.format("%s%s%s", WindConstants.SLASH, WindConstants.QUESTION_MARK, queryString));
        }
        return new LinkedMultiValueMap<>();
    }

    @NonNull
    public static Map<String, String[]> parseQueryParamsAsMap(String queryString) {
        Map<String, String[]> result = new HashMap<>();
        parseQueryParams(queryString)
                .forEach((key, values) -> {
                    if (!ObjectUtils.isEmpty(values)) {
                        result.put(key, values.toArray(new String[0]));
                    }
                });
        return result;
    }

    /**
     * 格式化查询参数
     *
     * @param queryParams 查询参数
     * @return 格式化后的查询参数
     */
    @NonNull
    public static String formatQueryString(Object queryParams) {
        return formatQueryString(queryParams, CollectionParamMode.REPEAT);
    }

    /**
     * 格式化查询参数
     *
     * @param queryParams    查询参数
     * @param collectionMode 集合参数序列化模式
     * @return 格式化后的查询参数
     */
    @NonNull
    public static String formatQueryString(Object queryParams, CollectionParamMode collectionMode) {
        return formatQueryString(queryParams, collectionMode, true);
    }

    /**
     * 格式化查询参数
     *
     * @param queryParams    查询参数
     * @param collectionMode 集合参数序列化模式
     * @param encoding       是否编码
     * @return 格式化后的查询参数
     */
    @NonNull
    public static String formatQueryString(Object queryParams, CollectionParamMode collectionMode, boolean encoding) {
        if (queryParams == null) {
            return WindConstants.EMPTY;
        }
        if (queryParams instanceof String queryString) {
            return queryString;
        }
        CollectionParamMode mode = collectionMode == null ? CollectionParamMode.REPEAT : collectionMode;
        List<String> pairs = new ArrayList<>();
        appendParams(pairs, toParamMap(queryParams), mode, encoding);
        return String.join(WindConstants.AND, pairs);
    }

    private static Map<String, Object> toParamMap(Object source) {
        if (source instanceof Map<?, ?> map) {
            return extractFromMap(map);
        }
        return extractFromBean(source);
    }

    private static Map<String, Object> extractFromMap(Map<?, ?> source) {
        if (source.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = LinkedHashMap.newLinkedHashMap(source.size());
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static Map<String, Object> extractFromBean(Object bean) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 已读过的属性
        Map<String, String> readProperties = new LinkedHashMap<>();
        for (Field field : WindReflectUtils.getFields(bean.getClass())) {
            Object value = WindReflectUtils.getFieldValue(field, bean);
            String paramName = resolveParamName(field.getName(), field);
            result.put(paramName, value);
            readProperties.put(field.getName(), paramName);
        }
        for (Method method : WindReflectUtils.getGetterMethods(bean.getClass())) {
            String nameWithMethod = WindReflectUtils.normalGetSetMethodNameToFiledName(resolveParamName(method.getName(), method));
            String filedName = WindReflectUtils.normalGetSetMethodNameToFiledName(method.getName());
            String propertyName = readProperties.get(filedName);
            if (!Objects.equals(filedName, propertyName) || Objects.equals(nameWithMethod, propertyName)) {
                // 注解在字段上 或 通过方法解析的属性名称和字段解析的名称相同
                continue;
            } else {
                readProperties.remove(filedName);
                result.remove(propertyName);
            }
            Object value = WindReflectUtils.invokeMethod(method, bean);
            result.put(nameWithMethod, value);
        }
        return result;
    }

    private static void appendParams(List<String> pairs, Map<String, Object> params, CollectionParamMode collectionMode, boolean encoding) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            appendParam(pairs, entry.getKey(), entry.getValue(), collectionMode, encoding);
        }
    }

    private static void appendParam(List<String> pairs, String key, Object value, CollectionParamMode collectionMode, boolean encoding) {
        if (!StringUtils.hasText(key) || value == null) {
            return;
        }

        if (isCollectionLike(value)) {
            List<String> values = flattenCollectionValues(value);
            if (values.isEmpty()) {
                return;
            }
            switch (collectionMode) {
                case REPEAT:
                    for (String item : values) {
                        pairs.add(buildPair(key, item, encoding));
                    }
                    return;
                case BRACKETS:
                    for (String item : values) {
                        pairs.add(buildPair(key + "[]", item, encoding));
                    }
                    return;
                case COMMA_SEPARATED:
                    pairs.add(buildPair(key, String.join(",", values), encoding));
                    return;
                default:
                    throw new IllegalArgumentException("Unsupported collection mode: " + collectionMode);
            }
        }

        String formattedValue = formatSingleValue(value);
        if (formattedValue != null) {
            pairs.add(buildPair(key, formattedValue, encoding));
        }
    }

    private static List<String> flattenCollectionValues(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String formatted = formatSingleValue(item);
                if (formatted != null) {
                    result.add(formatted);
                }
            }
            return result;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                String formatted = formatSingleValue(Array.get(value, i));
                if (formatted != null) {
                    result.add(formatted);
                }
            }
        }
        return result;
    }

    private static boolean isCollectionLike(Object value) {
        return value instanceof Iterable<?> || value.getClass().isArray();
    }

    private static String formatSingleValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof ProtocolValueFormatter serializable) {
            return serializable.format();
        }
        return String.valueOf(value);
    }

    private static String buildPair(String key, String value, boolean encoding) {
        if (encoding) {
            return urlEncode(key) + "=" + urlEncode(value);
        }
        return key + "=" + value;
    }

    private static String resolveParamName(String defaultName, AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            return defaultName;
        }
        for (Class<? extends Annotation> annotationType : ANNOTATION_CLASSES) {
            Annotation annotation = AnnotationUtils.findAnnotation(annotatedElement, annotationType);
            if (annotation == null) {
                continue;
            }
            Map<String, @Nullable Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
            Object value = attributes.get("value");
            if (!String.class.isInstance(value)) {
                if (value.getClass().isArray()) {
                    return (String) Array.get(value, 0);
                }
                value = attributes.get("name");
            }
            if (value != null) {
                if (value instanceof String v) {
                    return v;
                }
                if (value.getClass().isArray()) {
                    return (String) Array.get(value, 0);
                }
            }
        }
        return defaultName;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }


    public enum CollectionParamMode {

        /**
         * ids=1&ids=2
         */
        REPEAT,

        /**
         * ids[]=1&ids[]=2
         */
        BRACKETS,

        /**
         * ids=1,2
         */
        COMMA_SEPARATED
    }


}
