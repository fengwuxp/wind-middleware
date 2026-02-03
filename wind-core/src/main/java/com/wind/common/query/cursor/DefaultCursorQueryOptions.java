package com.wind.common.query.cursor;

import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.QueryOrderType;
import com.wind.common.query.supports.QueryType;
import org.jspecify.annotations.NonNull;

/**
 * 默认的游标查询对象
 *
 * @author wuxp
 * @date 2025-12-09 13:40
 **/
public final class DefaultCursorQueryOptions extends SortableCursorQuery<DefaultCursorQueryOrderField> {

    /**
     * 创建游标查询参数
     *
     * @param field 排序字段
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions desc(@NonNull DefaultCursorQueryOrderField field) {
        return order(field, QueryOrderType.DESC);
    }

    /**
     * 创建游标查询参数
     *
     * @param field 排序字段
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions asc(@NonNull DefaultCursorQueryOrderField field) {
        return order(field, QueryOrderType.ASC);
    }

    /**
     * 创建游标查询参数
     *
     * @param field     排序字段
     * @param orderType 排序类型
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions order(@NonNull DefaultCursorQueryOrderField field, @NonNull QueryOrderType orderType) {
        return of(null, null, 20, QueryType.QUERY_RESET, new DefaultCursorQueryOrderField[]{field}, new QueryOrderType[]{orderType});
    }

    /**
     * 创建游标查询参数
     *
     * @param preCursor 上一页游标
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions prev(String preCursor) {
        return prev(preCursor, 20);
    }

    /**
     * 创建游标查询参数
     *
     * @param preCursor 上一页游标
     * @param querySize 查询数量
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions prev(String preCursor, int querySize) {
        return prev(preCursor, querySize, DefaultCursorQueryOrderField.ID, QueryOrderType.DESC);
    }

    /**
     * 创建游标查询参数
     *
     * @param preCursor  上一页游标
     * @param querySize  查询数量
     * @param orderField 排序字段
     * @param orderType  排序类型
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions prev(String preCursor, int querySize, @NonNull DefaultCursorQueryOrderField orderField, @NonNull QueryOrderType orderType) {
        return of(preCursor, null, querySize, QueryType.QUERY_RESET, new DefaultCursorQueryOrderField[]{orderField}, new QueryOrderType[]{orderType});
    }

    /**
     * 创建游标查询参数
     *
     * @param nextCursor 下一页游标
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions next(String nextCursor) {
        return next(nextCursor, 20);
    }

    /**
     * 创建游标查询参数
     *
     * @param nextCursor 下一页游标
     * @param querySize  查询数量
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions next(String nextCursor, int querySize) {
        return next(nextCursor, querySize, DefaultCursorQueryOrderField.ID, QueryOrderType.DESC);
    }

    /**
     * 创建游标查询参数
     *
     * @param nextCursor 下一页游标
     * @param querySize  查询数量
     * @param orderField 排序字段
     * @param orderType  排序类型
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions next(String nextCursor, int querySize, @NonNull DefaultCursorQueryOrderField orderField, @NonNull QueryOrderType orderType) {
        return next(nextCursor, querySize, new DefaultCursorQueryOrderField[]{orderField}, new QueryOrderType[]{orderType});
    }

    /**
     * 创建游标查询参数
     *
     * @param nextCursor  下一页游标
     * @param querySize   查询数量
     * @param orderFields 排序字段
     * @param orderTypes  排序类型
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions next(String nextCursor, int querySize, @NonNull DefaultCursorQueryOrderField[] orderFields, @NonNull QueryOrderType[] orderTypes) {
        return next(nextCursor, querySize, QueryType.QUERY_RESET, orderFields, orderTypes);
    }

    /**
     * 创建游标查询参数
     *
     * @param nextCursor  下一页游标
     * @param querySize   查询数量
     * @param queryType   查询类型
     * @param orderFields 排序字段
     * @param orderTypes  排序类型
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions next(String nextCursor, int querySize, QueryType queryType, @NonNull DefaultCursorQueryOrderField[] orderFields,
                                                 @NonNull QueryOrderType[] orderTypes) {
        return of(null, nextCursor, querySize, queryType, orderFields, orderTypes);
    }

    /**
     * 创建游标查询参数
     *
     * @param prevCursor  上一页游标
     * @param nextCursor  下一页游标
     * @param querySize   查询数量
     * @param queryType   查询类型
     * @param orderFields 排序字段
     * @param orderTypes  排序类型
     * @return DefaultCursorQueryOptions
     */
    @NonNull
    public static DefaultCursorQueryOptions of(String prevCursor, String nextCursor, int querySize, QueryType queryType,
                                               @NonNull DefaultCursorQueryOrderField[] orderFields, @NonNull QueryOrderType[] orderTypes) {
        AssertUtils.isTrue(querySize > 0, "querySize must ge 0");
        AssertUtils.notEmpty(orderFields, "argument orderFields must not empty");
        AssertUtils.notEmpty(orderTypes, "argument orderTypes must not empty");
        DefaultCursorQueryOptions result = new DefaultCursorQueryOptions();
        result.setPrevCursor(prevCursor);
        result.setNextCursor(nextCursor);
        result.setQuerySize(querySize);
        result.setQueryType(queryType);
        result.setOrderFields(orderFields);
        result.setOrderTypes(orderTypes);
        return result;
    }

}
