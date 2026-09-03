package com.wind.common.jackson;

import com.wind.api.core.ApiResponse;
import com.wind.api.core.ImmutableApiResponse;
import com.wind.common.exception.ExceptionCode;
import com.wind.common.query.supports.ImmutablePagination;
import com.wind.common.query.supports.Pagination;
import com.wind.common.query.supports.QueryType;
import com.wind.jackson.WindJacksonModules;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WindJacksonModulesCompatibilityTests {

    private final JsonMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addModules(WindJacksonModules.iso8601LikeJavaTimeModule(), WindJacksonModules.apiModule())
            .build();

    @Test
    void testWindTimeModuleProvidesWindDateTimeFormat() {
        LocalDateTime value = LocalDateTime.of(2026, 8, 3, 12, 34, 56);

        String json = mapper.writeValueAsString(value);

        assertEquals("\"2026-08-03 12:34:56\"", json);
        assertEquals(value, mapper.readValue(json, LocalDateTime.class));
    }

    @Test
    void testWindApiModulePreservesApiResponseGenericType() {
        String json = """
                {"data":{"name":"wind"},"errorCode":"%s","errorMessage":null,"traceId":"trace-1"}
                """.formatted(ExceptionCode.SUCCESSFUL.getCode());

        ApiResponse<Item> response = mapper.readValue(json, new TypeReference<ApiResponse<Item>>() {
        });

        assertInstanceOf(ImmutableApiResponse.class, response);
        assertEquals(new Item("wind"), response.getData());
        assertEquals("trace-1", response.getTraceId());
    }

    @Test
    void testPaginationInterfaceWithCursorFieldUsesImmutablePagination() {
        String json = """
                {
                  "total":1,
                  "records":[{"name":"wind"}],
                  "queryPage":1,
                  "querySize":20,
                  "queryType":"QUERY_BOTH",
                  "prevCursor":"cursor-1"
                }
                """;

        Pagination<Item> pagination = mapper.readValue(json, new TypeReference<Pagination<Item>>() {
        });

        // 缺陷修正：Pagination<T> 不再因载荷包含游标字段而切换为不兼容的游标分页实现。
        assertInstanceOf(ImmutablePagination.class, pagination);
        assertEquals(List.of(new Item("wind")), pagination.getRecords());
        assertEquals(1, pagination.getQueryPage());
        assertEquals(20, pagination.getQuerySize());
        assertEquals(QueryType.QUERY_BOTH, pagination.getQueryType());
    }

    private record Item(String name) {
    }
}
