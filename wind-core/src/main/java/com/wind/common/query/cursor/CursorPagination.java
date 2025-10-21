package com.wind.common.query.cursor;

import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.util.WindReflectUtils;
import jakarta.annotation.Nullable;
import org.springframework.util.CollectionUtils;

import java.beans.Transient;
import java.util.Collections;
import java.util.List;

import static com.wind.common.query.cursor.CursorQueryUtils.CURSOR_FILED_NAME;

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
        return new ImmutableCursorPagination<>(-1L, Collections.emptyList(), 0, null, null);
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
        if (records == null || records.isEmpty()) {
            return empty();
        }

        boolean queryingPrev = query.getPrevCursor() != null;
        boolean queryingNext = query.getNextCursor() != null;
        int querySize = query.getQuerySize();
        boolean reachedEnd = records.size() < querySize;

        E first = CollectionUtils.firstElement(records);
        E last = CollectionUtils.lastElement(records);

        String prevCursor = null;
        String nextCursor = null;

        if (!queryingPrev && !queryingNext) {
            // 首页
            nextCursor = reachedEnd ? null : CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, last));
        } else if (queryingNext) {
            // 向后翻页
            prevCursor = CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, first));
            nextCursor = reachedEnd ? null : CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, last));
        } else {
            // 向前翻页
            prevCursor = reachedEnd ? null : CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, last));
            nextCursor = CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, first));
        }
        return of(-1L, records, query.getQuerySize(), prevCursor, nextCursor);
    }

    static <E> CursorPagination<E> of(long total, List<E> records, int querySize, String prevCursor, String nextCursor) {
        return new ImmutableCursorPagination<>(total, records, querySize, prevCursor, nextCursor);
    }
}
