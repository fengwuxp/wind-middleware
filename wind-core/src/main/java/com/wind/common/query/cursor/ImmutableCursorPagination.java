package com.wind.common.query.cursor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wind.common.query.supports.QueryType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.util.Collections;
import java.util.List;


@Getter
@EqualsAndHashCode
public final class ImmutableCursorPagination<T> implements CursorPagination<T> {

    @Serial
    private static final long serialVersionUID = -8964802111877344289L;

    private final long total;

    private final List<T> records;

    private final int querySize;

    private final QueryType queryType;

    private final String prevCursor;

    private final String nextCursor;

    @JsonProperty("hasPrev")
    @Override
    public boolean hasPrev() {
        return prevCursor != null;
    }

    @JsonProperty("hasNext")
    @Override
    public boolean hasNext() {
        return nextCursor != null;
    }

    @JsonCreator
    public ImmutableCursorPagination(@JsonProperty("total") long total,
                                     @JsonProperty("records") List<T> records,
                                     @JsonProperty("querySize") int querySize,
                                     @JsonProperty("queryType") QueryType queryType,
                                     @JsonProperty("prevCursor") String prevCursor,
                                     @JsonProperty("nextCursor") String nextCursor) {
        this.total = total;
        this.records = records;
        this.querySize = querySize;
        this.queryType = queryType;
        this.prevCursor = prevCursor;
        this.nextCursor = nextCursor;
    }

    /**
     * 为了给序列化框架使用，提供一个空构造
     */
    ImmutableCursorPagination() {
        this(0, Collections.emptyList(), 0, QueryType.QUERY_BOTH, null, null);
    }
}
