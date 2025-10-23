package com.wind.common.query.cursor;

import com.wind.common.query.supports.DefaultOrderField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wuxp
 * @date 2025-09-30 13:43
 **/
class AbstractCursorQueryTests {

    private final Long lastRecordId = 10L;

    @Test
    void testGenerateCursors() {
        ExampleQuery query = creteExampleQuery(null);
        Assertions.assertEquals(String.valueOf(lastRecordId), query.asNextTextId());
        Assertions.assertEquals(lastRecordId, query.asNextNumberId());
        Assertions.assertNull(query.asPrevNumberId());
    }

    @Test
    void testNextPageCursor() {
        ExampleQuery query = creteExampleQuery(null);
        ExampleQuery nextQuery = creteExampleQuery(query.getNextCursor());
        Assertions.assertEquals(2, CursorQueryUtils.getQueryCurrentPageNum(query));
        Assertions.assertEquals(2, CursorQueryUtils.parseCursorPageNum(query.getNextCursor()));
        Assertions.assertEquals(2, CursorQueryUtils.parseCursorPageNum(nextQuery.getPrevCursor()));
        Assertions.assertEquals(3, CursorQueryUtils.parseCursorPageNum(nextQuery.getNextCursor()));
    }

    private ExampleQuery creteExampleQuery(String queryNextCursor) {
        ExampleQuery query = new ExampleQuery();
        query.setName("zhans");
        query.setNextCursor(queryNextCursor);
        query.setMinGmtCreate(LocalDateTime.now());
        query.setQuerySize(2);
        String[] cursors = CursorQueryUtils.generateCursors(query, List.of(new ExampleEntity(1L), new ExampleEntity(lastRecordId)));
        query.setPrevCursor(cursors[0]);
        query.setNextCursor(cursors[1]);
        return query;
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class ExampleQuery extends AbstractCursorQuery<DefaultOrderField> {

        private String name;

        private List<String> tags;

        private LocalDateTime minGmtCreate;

        private LocalDateTime maxGmtCreate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExampleEntity {

        private Long id;
    }
}
