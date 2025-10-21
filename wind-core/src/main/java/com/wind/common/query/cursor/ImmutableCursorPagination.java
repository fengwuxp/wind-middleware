package com.wind.common.query.cursor;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.util.List;

/**
 * @author wuxp
 * @date 2025-09-30 13:05
 **/
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public final class ImmutableCursorPagination<T> implements CursorPagination<T> {

    @Serial
    private static final long serialVersionUID = -8964802111877344289L;

    private final long total;

    private final List<T> records;

    private final int querySize;

    private final String prevCursor;

    private final String nextCursor;


    @Override
    public boolean hasPrev() {
        return prevCursor != null;
    }

    @Override
    public boolean hasNext() {
        return nextCursor != null;
    }
}
