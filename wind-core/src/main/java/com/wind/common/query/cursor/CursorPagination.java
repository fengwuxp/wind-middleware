package com.wind.common.query.cursor;

import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.query.supports.QueryType;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import java.beans.Transient;
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
     * @return {@link #getRecords()}是否为 null 或空集合
     */
    @Transient
    default boolean isEmpty() {
        return CollectionUtils.isEmpty(getRecords());
    }

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
    static <E> CursorPagination<E> of(List<E> records, AbstractCursorQuery<? extends QueryOrderField> query) {
        return of(-1, records, query);
    }

    /**
     * 创建游标分页对象
     *
     * @param total   总记录数
     * @param records 分页数据
     * @param query   查询参数
     * @param <E>     分页数据类型
     * @return 分页对象
     */
    static <E> CursorPagination<E> of(long total, List<E> records, AbstractCursorQuery<? extends QueryOrderField> query) {
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

    static <E> CursorPagination<E> of(long total, List<E> records, int querySize, QueryType queryType, String prevCursor, String nextCursor) {
        return new ImmutableCursorPagination<>(total, records, querySize, queryType, prevCursor, nextCursor);
    }
}
