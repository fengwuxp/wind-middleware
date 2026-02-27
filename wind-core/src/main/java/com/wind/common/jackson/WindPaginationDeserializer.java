package com.wind.common.jackson;

import com.wind.common.query.WindPagination;
import com.wind.common.query.cursor.ImmutableCursorPagination;
import com.wind.common.query.supports.ImmutablePagination;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * jackson  {@link WindPagination}反序列化器
 *
 * @author wuxp
 * @date 2026-02-27 10:24
 **/
public final class WindPaginationDeserializer<T extends WindPagination<?>> extends ValueDeserializer<T> {

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode jsonNode = ctxt.readTree(p);
        if (jsonNode.has("prevCursor")) {
            // 根据 prevCursor 字段的存在性决定实现类
            return (T) ctxt.readTreeAsValue(jsonNode, ImmutableCursorPagination.class);
        } else {
            return (T) ctxt.readTreeAsValue(jsonNode, ImmutablePagination.class);
        }
    }
}