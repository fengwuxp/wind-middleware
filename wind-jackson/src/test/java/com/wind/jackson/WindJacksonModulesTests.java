package com.wind.jackson;

import com.wind.api.core.ApiResponse;
import com.wind.api.core.ImmutableApiResponse;
import com.wind.common.exception.ExceptionCode;
import com.wind.common.query.WindPagination;
import com.wind.common.query.cursor.CursorPagination;
import com.wind.common.query.cursor.ImmutableCursorPagination;
import com.wind.common.query.supports.ImmutablePagination;
import com.wind.common.query.supports.Pagination;
import com.wind.common.query.supports.QueryType;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindJacksonModulesTests {

    @Test
    void testDeserializeApiResponseInterface() {
        String json = """
                {"data":{"name":"wind"},"errorCode":"%s","errorMessage":null,"traceId":"trace-1"}
                """.formatted(ExceptionCode.SUCCESSFUL.getCode());

        ApiResponse<Item> response = WindJson.parseObject(json, new TypeReference<ApiResponse<Item>>() {
        });

        assertInstanceOf(ImmutableApiResponse.class, response);
        assertEquals(new Item("wind"), response.getData());
        assertEquals("trace-1", response.getTraceId());
    }

    @Test
    void testDeserializePaginationInterfaceWithGenericRecords() {
        String json = """
                {"total":1,"records":[{"name":"wind"}],"queryPage":1,"querySize":20,"queryType":"QUERY_BOTH"}
                """;

        Pagination<Item> pagination = WindJson.parseObject(json, new TypeReference<Pagination<Item>>() {
        });

        assertInstanceOf(ImmutablePagination.class, pagination);
        assertEquals(1, pagination.getTotal());
        assertEquals(List.of(new Item("wind")), pagination.getRecords());
        assertEquals(1, pagination.getQueryPage());
        assertEquals(20, pagination.getQuerySize());
        assertEquals(QueryType.QUERY_BOTH, pagination.getQueryType());
    }

    @Test
    void testDeserializeWindPaginationPageWithGenericRecords() {
        String json = """
                {"total":1,"records":[{"name":"wind"}],"queryPage":1,"querySize":20,"queryType":"QUERY_BOTH"}
                """;

        WindPagination<Item> pagination = WindJson.parseObject(json, new TypeReference<WindPagination<Item>>() {
        });

        assertInstanceOf(ImmutablePagination.class, pagination);
        assertInstanceOf(Item.class, pagination.getRecords().getFirst());
        assertEquals(List.of(new Item("wind")), pagination.getRecords());
    }

    @Test
    void testDeserializeWindPaginationCursorWithGenericRecords() {
        String json = """
                {"total":1,"records":[{"name":"wind"}],"querySize":20,"queryType":"QUERY_BOTH",\
                "prevCursor":"prev-1","nextCursor":"next-1"}
                """;

        WindPagination<Item> pagination = WindJson.parseObject(json, new TypeReference<WindPagination<Item>>() {
        });

        CursorPagination<?> cursor = assertInstanceOf(ImmutableCursorPagination.class, pagination);
        assertEquals("prev-1", cursor.getPrevCursor());
        assertEquals("next-1", cursor.getNextCursor());
        assertInstanceOf(Item.class, cursor.getRecords().getFirst());
        assertEquals(List.of(new Item("wind")), cursor.getRecords());
    }

    @Test
    void testRoundTripFirstCursorPageWithNullPrevCursor() {
        CursorPagination<Item> source = CursorPagination.of(
                1,
                List.of(new Item("wind")),
                20,
                QueryType.QUERY_BOTH,
                null,
                "next-1");

        String json = WindJson.toJsonString(source);

        assertTrue(json.contains("\"prevCursor\":null"));
        assertTrue(json.contains("\"nextCursor\":\"next-1\""));
        assertTrue(json.contains("\"hasPrev\":false"));
        assertTrue(json.contains("\"hasNext\":true"));

        WindPagination<Item> pagination = WindJson.parseObject(json, new TypeReference<WindPagination<Item>>() {
        });

        CursorPagination<?> cursor = assertInstanceOf(ImmutableCursorPagination.class, pagination);
        assertEquals("next-1", cursor.getNextCursor());
        assertInstanceOf(Item.class, cursor.getRecords().getFirst());
        assertEquals(List.of(new Item("wind")), cursor.getRecords());
    }

    @Test
    void testDeserializeFirstCursorPageWithoutPrevCursor() {
        String json = """
                {"total":1,"records":[{"name":"wind"}],"querySize":20,"queryType":"QUERY_BOTH",\
                "nextCursor":"next-1","hasPrev":false,"hasNext":true}
                """;

        WindPagination<Item> pagination = WindJson.parseObject(json, new TypeReference<WindPagination<Item>>() {
        });

        CursorPagination<?> cursor = assertInstanceOf(ImmutableCursorPagination.class, pagination);
        assertEquals("next-1", cursor.getNextCursor());
        assertInstanceOf(Item.class, cursor.getRecords().getFirst());
        assertEquals(List.of(new Item("wind")), cursor.getRecords());
    }

    private record Item(String name) {
    }
}
