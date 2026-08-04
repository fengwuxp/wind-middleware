package com.wind.jackson;

import com.wind.api.core.ApiResponse;
import com.wind.api.core.ImmutableApiResponse;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.ImmutablePagination;
import com.wind.common.query.supports.Pagination;
import com.wind.jackson.deserializer.WindPaginationDeserializer;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.wind.common.WindDateFormatPatterns.HH_MM_SS;
import static com.wind.common.WindDateFormatPatterns.YYYY_MM_DD;
import static com.wind.common.WindDateFormatPatterns.YYYY_MM_DD_HH_MM_SS;

/**
 * 提供 Wind 领域对象和日期时间格式的 Jackson 模块。
 *
 * @author wuxp
 * @since 2026-08-03
 */
public final class WindJacksonModules {

    private WindJacksonModules() {
        throw new AssertionError();
    }

    /**
     * 创建使用 Wind 日期时间格式的 Jackson 模块。
     *
     * @return 日期时间模块
     */
    public static JacksonModule iso8601LikeJavaTimeModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
        return module;
    }

    /**
     * 创建支持 Wind API 响应和分页接口的 Jackson 模块。
     *
     * @return Wind API 模块
     */
    public static JacksonModule apiModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(WindPagination.class, new WindPaginationDeserializer());
        module.addAbstractTypeMapping(Pagination.class, ImmutablePagination.class);
        module.addAbstractTypeMapping(ApiResponse.class, ImmutableApiResponse.class);
        return module;
    }
}
