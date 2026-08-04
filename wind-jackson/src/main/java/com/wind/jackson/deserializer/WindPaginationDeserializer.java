package com.wind.jackson.deserializer;

import com.wind.common.query.WindPagination;
import com.wind.common.query.cursor.ImmutableCursorPagination;
import com.wind.common.query.supports.ImmutablePagination;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.type.TypeFactory;

/**
 * 保留 {@link WindPagination} 记录元素泛型的反序列化器。
 *
 * @author wuxp
 * @since 2026-08-03
 */
public final class WindPaginationDeserializer extends ValueDeserializer<WindPagination<?>> {

    private final JavaType itemType;

    public WindPaginationDeserializer() {
        this(TypeFactory.unknownType());
    }

    private WindPaginationDeserializer(JavaType itemType) {
        this.itemType = itemType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        JavaType paginationType = context.getContextualType();
        if (paginationType == null && property != null) {
            paginationType = property.getType();
        }
        JavaType contextualItemType = paginationType == null
                ? TypeFactory.unknownType()
                : paginationType.containedTypeOrUnknown(0);
        return new WindPaginationDeserializer(contextualItemType);
    }

    @Override
    public WindPagination<?> deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        JsonNode jsonNode = context.readTree(parser);
        boolean cursorPagination = jsonNode.has("prevCursor")
                || jsonNode.has("nextCursor")
                || jsonNode.has("hasPrev")
                || jsonNode.has("hasNext");
        Class<?> implementationType = cursorPagination
                ? ImmutableCursorPagination.class
                : ImmutablePagination.class;
        JavaType targetType = context.getTypeFactory().constructParametricType(implementationType, itemType);
        return context.readTreeAsValue(jsonNode, targetType);
    }
}
