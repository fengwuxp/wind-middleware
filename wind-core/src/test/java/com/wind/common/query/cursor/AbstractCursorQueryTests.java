package com.wind.common.query.cursor;

import com.wind.common.query.supports.DefaultOrderField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wuxp
 * @date 2025-09-30 13:43
 **/
class AbstractCursorQueryTests {


    @Test
    void testCursor() {
        ExampleQuery query = new ExampleQuery();
        query.setName("zhans");
        query.setMinGmtCreate(LocalDateTime.now());
        long firstRecordId = 20L, lastRecordId = 200L;
        String prevCursor = QueryCursorUtils.generateCursor(query, firstRecordId);
        String nextCursor = QueryCursorUtils.generateCursor(query, lastRecordId);
        query.setPrevCursor(prevCursor);
        query.setNextCursor(nextCursor);
        Assertions.assertEquals(String.valueOf(lastRecordId), query.asNextTextId());
        Assertions.assertEquals(firstRecordId, query.asPrevNumberId());
        Assertions.assertEquals(lastRecordId, query.asNextNumberId());
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class ExampleQuery extends AbstractCursorQuery<DefaultOrderField> {

        private String name;

        private List<String> tags;

        private LocalDateTime minGmtCreate;

        private LocalDateTime maxGmtCreate;
    }
}
