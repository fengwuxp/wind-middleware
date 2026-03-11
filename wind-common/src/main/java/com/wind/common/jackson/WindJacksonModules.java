package com.wind.common.jackson;

import com.wind.api.core.ApiResponse;
import com.wind.api.core.ImmutableApiResponse;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.Pagination;
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
 * 对 jackson 模块的扩展配置
 *
 * @author wuxp
 * @date 2026-03-02 11:04
 **/
public final class WindJacksonModules {

    private WindJacksonModules() {
        throw new AssertionError();
    }


    /**
     * 创建 like ISO8601 时间格式的 jackson 模块
     *
     * @return 模块
     */
    public static JacksonModule iso8601LikeJavaTimeModule() {
        SimpleModule timeModule = new SimpleModule();
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
        timeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        timeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
        return timeModule;
    }

    /**
     * 创建 api 模块
     *
     * @return 模块
     */
    public static JacksonModule apiModule() {
        SimpleModule module = new SimpleModule();
        // 配置 Wind 相关 jackson 反序列化
        module.addDeserializer(WindPagination.class, new WindPaginationDeserializer<>());
        module.addDeserializer(Pagination.class, new WindPaginationDeserializer<>());
        module.addAbstractTypeMapping(ApiResponse.class, ImmutableApiResponse.class);
        return module;
    }
}
