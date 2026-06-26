package com.wind.common.query.cursor;

import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.query.supports.QueryType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于游标分页查询的分页对象
 *
 * @author wuxp
 * @date 2025-09-30 13:01
 **/
public interface CursorPagination<T> extends WindPagination<T> {

    /**
     * @return 总记录数据，如果不关心总数，请返回 -1
     */
    default long getTotal() {
        return -1L;
    }

    /**
     * @return 上一页游标，如果没有则返回 null
     */
    @Nullable
    String getPrevCursor();

    /**
     * @return 下一页游标，如果没有则返回 null
     */
    @Nullable
    String getNextCursor();

    /**
     * @return 是否有上一页
     */
    boolean hasPrev();

    /**
     * @return 是否有下一页
     */
    boolean hasNext();

    /**
     * 创建一个空的分页对象
     *
     * @param <E> 分页数据类型
     * @return 分页对象
     */
    static <E> CursorPagination<E> empty() {
        return of(-1L, Collections.emptyList(), 0, QueryType.QUERY_RESET, null, null);
    }

    /**
     * 创建游标分页对象，查询数据的结果对象必须有 id 字段
     *
     * @param records 分页数据
     * @param query   查询参数
     * @param <E>     分页数据类型
     * @return 分页对象
     */
    static <E> CursorPagination<E> withQuery(List<E> records, AbstractCursorQuery<? extends QueryOrderField> query) {
        return withQuery(-1, records, query);
    }

    /**
     * 在查询中创建游标分页对象，对于向上翻页时，数据需要翻转
     *
     * @param total   总记录数
     * @param records 分页数据
     * @param query   查询参数
     * @param <E>     分页数据类型
     * @return 分页对象
     */
    static <E> CursorPagination<E> withQuery(long total, List<E> records, AbstractCursorQuery<? extends QueryOrderField> query) {
        if (records == null || records.isEmpty()) {
            return empty();
        }
        if (query.getPrevCursor() != null) {
            // 向前翻页和向后翻页的排序方式相反，为了保证数据排序一致(游标一致)，需要翻转数据
            records = new ArrayList<>(records);
            Collections.reverse(records);
        }
        String[] cursors = CursorQueryUtils.generateCursors(query, records);
        return of(total, records, query.getQuerySize(), query.getQueryType(), cursors[0], cursors[1]);
    }

    static <E> CursorPagination<E> of(long total, List<E> records, AbstractCursorQuery<? extends QueryOrderField> query) {
        String[] cursors = CursorQueryUtils.generateCursors(query, records);
        return of(total, records, query.getQuerySize(), query.getQueryType(), cursors[0], cursors[1]);
    }

    static <E> CursorPagination<E> of(long total, List<E> records, int querySize, QueryType queryType, String prevCursor, String nextCursor) {
        return new ImmutableCursorPagination<>(total, records, querySize, queryType, prevCursor, nextCursor);
    }

    /**
     * 替换分页数据
     *
     * @param pagination 原分页数据
     * @param records    分页数据
     * @param <E>        分页数据类型
     * @return 分页对象
     */
    static <E> CursorPagination<E> withRecords(WindPagination<?> pagination, List<E> records) {
        if (pagination instanceof CursorPagination<?> cursorPagination) {
            return of(pagination.getTotal(), records, pagination.getQuerySize(), pagination.getQueryType(), cursorPagination.getPrevCursor(), cursorPagination.getNextCursor());
        }
        throw new IllegalArgumentException("pagination must be CursorPagination");
    }
}
