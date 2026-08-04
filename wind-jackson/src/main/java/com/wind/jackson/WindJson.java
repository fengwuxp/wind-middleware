package com.wind.jackson;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 基于 Jackson 的 Wind JSON 序列化、反序列化和对象转换入口。
 * Jackson 原生异常及其 cause 仍可能包含 getter、creator 或自定义序列化器中的业务数据；
 * 默认 parser 的错误内容限制并不提供全量异常脱敏保证。
 *
 * @author wuxp
 * @since 2026-08-03
 */
public final class WindJson {

    private static final int MAX_NESTING_DEPTH = 200;

    private static final long MAX_DOCUMENT_LENGTH = 16L * 1024 * 1024;

    private static final long MAX_TOKEN_COUNT = 1_000_000;

    private static final int MAX_NUMBER_LENGTH = 1_000;

    private static final int MAX_STRING_LENGTH = 8 * 1024 * 1024;

    private static final int MAX_PROPERTY_NAME_LENGTH = 16 * 1024;

    private static final AtomicReference<JsonMapper> MAPPER = new AtomicReference<>(defaultMapper());

    private static final Object MAPPER_CONFIGURATION_LOCK = new Object();

    private WindJson() {
        throw new AssertionError();
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串，{@code value} 为 {@code null} 时返回 {@code "null"}
     * @throws JacksonException 序列化失败
     */
    public static @NonNull String toJsonString(@Nullable Object value) {
        JsonMapper mapper = MAPPER.get();
        return mapper.writeValueAsString(value);
    }

    /**
     * 将 JSON 字符串反序列化为指定类型。
     *
     * @param json       JSON 字符串
     * @param targetType 目标类型
     * @param <T>        目标类型
     * @return 反序列化结果，JSON 为 {@code null} 字面量时返回 {@code null}
     * @throws IllegalArgumentException 参数为空或 JSON 为空白字符串
     * @throws JacksonException         反序列化失败
     */
    public static <T> @Nullable T parseObject(@NonNull String json, @NonNull Class<T> targetType) {
        requireJson(json);
        requireTargetType(targetType);
        JsonMapper mapper = MAPPER.get();
        return mapper.readValue(json, targetType);
    }

    /**
     * 将 JSON 字符串反序列化为带泛型信息的指定类型。
     *
     * @param json       JSON 字符串
     * @param targetType Jackson 类型引用
     * @param <T>        目标类型
     * @return 反序列化结果，JSON 为 {@code null} 字面量时返回 {@code null}
     * @throws IllegalArgumentException 参数为空或 JSON 为空白字符串
     * @throws JacksonException         反序列化失败
     */
    public static <T> @Nullable T parseObject(@NonNull String json, @NonNull TypeReference<T> targetType) {
        requireJson(json);
        requireTargetType(targetType);
        JsonMapper mapper = MAPPER.get();
        return mapper.readValue(json, targetType);
    }

    /**
     * 将 JSON 字符串反序列化为 Java 反射类型。
     *
     * @param json       JSON 字符串
     * @param targetType Java 反射类型
     * @param <T>        调用方期望的目标类型
     * @return 反序列化结果，JSON 为 {@code null} 字面量时返回 {@code null}
     * @throws IllegalArgumentException 参数为空或 JSON 为空白字符串
     * @throws JacksonException         反序列化失败
     */
    public static <T> @Nullable T parseObject(@NonNull String json, @NonNull Type targetType) {
        requireJson(json);
        requireTargetType(targetType);
        JsonMapper mapper = MAPPER.get();
        return mapper.readValue(json, mapper.getTypeFactory().constructType(targetType));
    }

    /**
     * 将 JSON 字符串反序列化为运行时构造的 Jackson 类型。
     *
     * @param json       JSON 字符串
     * @param targetType Jackson 运行时类型
     * @param <T>        调用方期望的目标类型
     * @return 反序列化结果，JSON 为 {@code null} 字面量时返回 {@code null}
     * @throws IllegalArgumentException 参数为空或 JSON 为空白字符串
     * @throws JacksonException         反序列化失败
     */
    public static <T> @Nullable T parseObject(@NonNull String json, @NonNull JavaType targetType) {
        requireJson(json);
        requireTargetType(targetType);
        JsonMapper mapper = MAPPER.get();
        return mapper.readValue(json, targetType);
    }

    /**
     * 将 JSON 数组反序列化为元素列表。
     *
     * @param json        JSON 数组字符串
     * @param elementType 元素类型
     * @param <E>         元素类型
     * @return 元素列表，JSON 为 {@code null} 字面量时返回 {@code null}
     * @throws IllegalArgumentException 参数为空或 JSON 为空白字符串
     * @throws JacksonException         反序列化失败
     */
    public static <E> @Nullable List<E> parseArray(@NonNull String json, @NonNull Class<E> elementType) {
        requireJson(json);
        requireTargetType(elementType);
        JsonMapper mapper = MAPPER.get();
        JavaType targetType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return mapper.readValue(json, targetType);
    }

    /**
     * 将对象转换为指定类型。
     *
     * @param value      待转换对象
     * @param targetType 目标类型
     * @param <T>        目标类型
     * @return 转换结果
     * @throws IllegalArgumentException 目标类型为空或转换失败
     * @throws JacksonException         转换失败
     */
    public static <T> @Nullable T convertValue(@Nullable Object value, @NonNull Class<T> targetType) {
        requireTargetType(targetType);
        JsonMapper mapper = MAPPER.get();
        return mapper.convertValue(value, targetType);
    }

    /**
     * 将对象转换为带泛型信息的指定类型。
     *
     * @param value      待转换对象
     * @param targetType Jackson 类型引用
     * @param <T>        目标类型
     * @return 转换结果
     * @throws IllegalArgumentException 目标类型为空或转换失败
     * @throws JacksonException         转换失败
     */
    public static <T> @Nullable T convertValue(@Nullable Object value, @NonNull TypeReference<T> targetType) {
        requireTargetType(targetType);
        JsonMapper mapper = MAPPER.get();
        return mapper.convertValue(value, targetType);
    }

    /**
     * 将对象转换为 Java 反射类型。
     *
     * @param value      待转换对象
     * @param targetType Java 反射类型
     * @param <T>        调用方期望的目标类型
     * @return 转换结果
     * @throws IllegalArgumentException 目标类型为空或转换失败
     * @throws JacksonException         转换失败
     */
    public static <T> @Nullable T convertValue(@Nullable Object value, @NonNull Type targetType) {
        requireTargetType(targetType);
        JsonMapper mapper = MAPPER.get();
        return mapper.convertValue(value, mapper.getTypeFactory().constructType(targetType));
    }

    /**
     * 将对象转换为运行时构造的 Jackson 类型。
     *
     * @param value      待转换对象
     * @param targetType Jackson 运行时类型
     * @param <T>        调用方期望的目标类型
     * @return 转换结果
     * @throws IllegalArgumentException 目标类型为空或转换失败
     * @throws JacksonException         转换失败
     */
    public static <T> @Nullable T convertValue(@Nullable Object value, @NonNull JavaType targetType) {
        requireTargetType(targetType);
        JsonMapper mapper = MAPPER.get();
        return mapper.convertValue(value, targetType);
    }

    /**
     * 获取当前 {@link JsonMapper}。
     *
     * @return 当前 JSON Mapper
     */
    public static @NonNull JsonMapper getJsonMapper() {
        return MAPPER.get();
    }

    /**
     * 完整替换 {@link WindJson} 使用的 {@link JsonMapper}。
     * 调用方将同时接管 Wind modules、资源约束和其他安全配置。建议在应用启动期完成配置；
     * 运行期替换时，在途调用继续使用其已获取的旧 Mapper 快照，后续调用使用新 Mapper。
     *
     * @param mapper JSON Mapper
     * @throws NullPointerException {@code mapper} 为空
     */
    public static void setJsonMapper(@NonNull JsonMapper mapper) {
        Objects.requireNonNull(mapper, "mapper");
        synchronized (MAPPER_CONFIGURATION_LOCK) {
            MAPPER.set(mapper);
        }
    }

    /**
     * 基于当前 {@link JsonMapper} 增量调整配置，并在构建成功后原子替换。
     * 建议在应用启动期完成配置；运行期调整时，在途调用继续使用其已获取的旧 Mapper 快照，
     * 后续调用使用新 Mapper。
     *
     * @param customizer Mapper Builder 配置函数
     * @throws NullPointerException {@code customizer} 为空
     */
    public static void configureJsonMapper(@NonNull Consumer<JsonMapper.Builder> customizer) {
        Objects.requireNonNull(customizer, "customizer");
        synchronized (MAPPER_CONFIGURATION_LOCK) {
            JsonMapper.Builder builder = MAPPER.get().rebuild();
            customizer.accept(builder);
            MAPPER.set(builder.build());
        }
    }

    private static JsonMapper defaultMapper() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxNestingDepth(MAX_NESTING_DEPTH)
                .maxDocumentLength(MAX_DOCUMENT_LENGTH)
                .maxTokenCount(MAX_TOKEN_COUNT)
                .maxNumberLength(MAX_NUMBER_LENGTH)
                .maxStringLength(MAX_STRING_LENGTH)
                .maxNameLength(MAX_PROPERTY_NAME_LENGTH)
                .build();
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(constraints)
                .errorReportConfiguration(ErrorReportConfiguration.builder()
                        .maxErrorTokenLength(0)
                        .maxRawContentLength(0)
                        .build())
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        return JsonMapper.builder(jsonFactory)
                .findAndAddModules()
                .addModules(WindJacksonModules.iso8601LikeJavaTimeModule(), WindJacksonModules.apiModule())
                .build();
    }

    private static void requireJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("json must not be null or blank");
        }
    }

    private static void requireTargetType(Object targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("targetType must not be null");
        }
    }

}
