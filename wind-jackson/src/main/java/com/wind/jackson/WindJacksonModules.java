package com.wind.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wind.api.core.ApiResponse;
import com.wind.api.core.ImmutableApiResponse;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.ImmutablePagination;
import com.wind.common.query.supports.Pagination;
import com.wind.jackson.deserializer.WindPaginationDeserializer;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import static com.wind.common.WindDateFormatPatterns.HH_MM_SS;
import static com.wind.common.WindDateFormatPatterns.LOCAL_DATETIME_SPACE_OR_T;
import static com.wind.common.WindDateFormatPatterns.YYYY_MM_DD;
import static com.wind.common.WindDateFormatPatterns.YYYY_MM_DD_HH_MM_SS;

/**
 * 提供 Wind JSON 日期时间格式和 API 公共类型转换的 Jackson 模块。
 *
 * <p>时间模块保持 Wind 既有的序列化输出：使用空格分隔日期和时间，
 * 非零纳秒追加小数秒；反序列化额外兼容 ISO-8601 {@code T} 分隔符和
 * 供应商常见的紧凑时间文本。</p>
 *
 * <p>API 模块负责 {@link com.wind.api.core.ApiResponse}、{@link com.wind.common.query.supports.Pagination}
 * 等公共抽象类型的默认实现映射。</p>
 *
 * @author wuxp
 * @since 2026-08-03
 */
public final class WindJacksonModules {

    private static final DateTimeFormatter LOCAL_DATE_TIME_SERIALIZER_FORMATTER =
            createLocalDateTimeSerializerFormatter();

    private static final DateTimeFormatter LOCAL_DATE_TIME_DESERIALIZER_FORMATTER =
            createLocalDateTimeDeserializerFormatter();

    private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD);

    private static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern(HH_MM_SS);

    private WindJacksonModules() {
        throw new AssertionError();
    }

    /**
     * 创建使用 Wind 日期时间格式的 Jackson 模块。
     * 序列化使用空格分隔，非零纳秒追加小数秒；反序列化同时接受空格和
     * {@code T} 分隔符。
     *
     * @return 日期时间模块
     */
    public static JacksonModule iso8601LikeJavaTimeModule() {
        SimpleModule module = new SimpleModule("wind-iso8601-like-java-time");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(LOCAL_DATE_TIME_SERIALIZER_FORMATTER));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(LOCAL_DATE_FORMATTER));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(LOCAL_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(LOCAL_DATE_TIME_DESERIALIZER_FORMATTER));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(LOCAL_DATE_FORMATTER));
        module.addDeserializer(LocalTime.class, new CompactLocalTimeDeserializer(LOCAL_TIME_FORMATTER));
        return module;
    }

    /**
     * 创建 Wind {@link LocalDateTime} 序列化 formatter。
     * 序列化固定使用空格分隔；纳秒为零时不输出小数部分，非零时最多输出 9 位纳秒。
     *
     * @return 线程安全且不可变的序列化 formatter
     */
    private static DateTimeFormatter createLocalDateTimeSerializerFormatter() {
        return new DateTimeFormatterBuilder()
                .appendPattern(YYYY_MM_DD_HH_MM_SS)
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .toFormatter();
    }

    /**
     * 创建 Wind {@link LocalDateTime} 反序列化 formatter。
     * 接受空格或 {@code T} 分隔符，并接受可选的 1-9 位小数秒；秒数本身仍然必需。
     *
     * @return 线程安全且不可变的反序列化 formatter
     */
    private static DateTimeFormatter createLocalDateTimeDeserializerFormatter() {
        return new DateTimeFormatterBuilder()
                .appendPattern(LOCAL_DATETIME_SPACE_OR_T)
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
                .optionalEnd()
                .toFormatter();
    }

    /**
     * 委托 Jackson 标准时间解析，并在标准格式失败时兼容供应商返回的紧凑
     * HHMMSS/HMMSS 文本。
     * contextual 委托保证字段级 {@link JsonFormat} 配置仍然生效。
     */
    private static final class CompactLocalTimeDeserializer extends ValueDeserializer<LocalTime> {

        private final LocalTimeDeserializer delegate;

        private CompactLocalTimeDeserializer(DateTimeFormatter formatter) {
            this(new LocalTimeDeserializer(formatter));
        }

        private CompactLocalTimeDeserializer(LocalTimeDeserializer delegate) {
            this.delegate = delegate;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
            ValueDeserializer<?> contextualDelegate = delegate.createContextual(context, property);
            if (contextualDelegate == delegate) {
                return this;
            }
            if (contextualDelegate instanceof LocalTimeDeserializer localTimeDeserializer) {
                return new CompactLocalTimeDeserializer(localTimeDeserializer);
            }
            return contextualDelegate;
        }

        @Override
        public Class<?> handledType() {
            return LocalTime.class;
        }

        @Override
        public LocalTime deserialize(JsonParser parser, DeserializationContext context)
                throws JacksonException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return delegate.deserialize(parser, context);
            }
            String value = parser.getString();
            try {
                return delegate.deserialize(parser, context);
            } catch (JacksonException original) {
                LocalTime compactTime = parseCompactTime(value);
                if (compactTime != null) {
                    return compactTime;
                }
                throw original;
            }
        }

        private static LocalTime parseCompactTime(String value) {
            String text = value.trim();
            if (!((text.length() == 5 || text.length() == 6)
                    && text.chars().allMatch(character -> character >= '0' && character <= '9'))) {
                return null;
            }
            try {
                int hour;
                int minute;
                int second;
                if (text.length() == 5) {
                    // 部分供应商省略小时的前导零，例如 33454 表示 03:34:54。
                    hour = Integer.parseInt(text.substring(0, 1));
                    minute = Integer.parseInt(text.substring(1, 3));
                    second = Integer.parseInt(text.substring(3, 5));
                } else {
                    hour = Integer.parseInt(text.substring(0, 2));
                    minute = Integer.parseInt(text.substring(2, 4));
                    second = Integer.parseInt(text.substring(4, 6));
                }
                return LocalTime.of(hour, minute, second);
            } catch (DateTimeException | NumberFormatException ignored) {
                return null;
            }
        }
    }

    /**
     * 创建支持 Wind API 响应和分页接口的 Jackson 模块。
     *
     * @return Wind API 模块
     */
    public static JacksonModule apiModule() {
        SimpleModule module = new SimpleModule("wind-api");
        module.addDeserializer(WindPagination.class, new WindPaginationDeserializer());
        module.addAbstractTypeMapping(Pagination.class, ImmutablePagination.class);
        module.addAbstractTypeMapping(ApiResponse.class, ImmutableApiResponse.class);
        return module;
    }
}
