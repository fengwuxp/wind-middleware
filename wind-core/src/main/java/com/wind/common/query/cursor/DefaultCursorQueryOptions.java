package com.wind.common.query.cursor;

import com.wind.common.query.supports.QueryOrderType;
import com.wind.common.query.supports.QueryType;
import org.jspecify.annotations.Nullable;

/**
 * 默认的游标查询对象
 *
 * @author wuxp
 * @date 2025-12-09 13:40
 **/
public final class DefaultCursorQueryOptions extends AbstractCursorQuery<DefaultCursorQueryOrderField> {

    public static DefaultCursorQueryOptions desc(DefaultCursorQueryOrderField field) {
        return order(field, QueryOrderType.DESC);
    }

    public static DefaultCursorQueryOptions asc(DefaultCursorQueryOrderField field) {
        return order(field, QueryOrderType.ASC);
    }

    public static DefaultCursorQueryOptions order(DefaultCursorQueryOrderField field, QueryOrderType orderType) {
        return of(null, null, 20, QueryType.QUERY_RESET, new DefaultCursorQueryOrderField[]{field}, new QueryOrderType[]{orderType});
    }

    public static DefaultCursorQueryOptions prev(String preCursor) {
        return prev(preCursor, 20);
    }

    public static DefaultCursorQueryOptions prev(String preCursor, int querySize) {
        return prev(preCursor, querySize, DefaultCursorQueryOrderField.ID, QueryOrderType.DESC);
    }

    public static DefaultCursorQueryOptions prev(String preCursor, int querySize, DefaultCursorQueryOrderField orderField, QueryOrderType orderType) {
        return of(preCursor, null, querySize, QueryType.QUERY_RESET, new DefaultCursorQueryOrderField[]{orderField}, new QueryOrderType[]{orderType});
    }

    public static DefaultCursorQueryOptions next(String nextCursor) {
        return next(nextCursor, 20);
    }

    public static DefaultCursorQueryOptions next(String nextCursor, int querySize) {
        return next(nextCursor, querySize, DefaultCursorQueryOrderField.ID, QueryOrderType.DESC);
    }

    public static DefaultCursorQueryOptions next(String nextCursor, int querySize, DefaultCursorQueryOrderField orderField, QueryOrderType orderType) {
        return next(nextCursor, querySize, new DefaultCursorQueryOrderField[]{orderField}, new QueryOrderType[]{orderType});
    }

    public static DefaultCursorQueryOptions next(String nextCursor, int querySize, @Nullable DefaultCursorQueryOrderField[] orderFields,
                                                 @Nullable QueryOrderType[] orderTypes) {
        return next(nextCursor, querySize, QueryType.QUERY_RESET, orderFields, orderTypes);
    }

    public static DefaultCursorQueryOptions next(String nextCursor, int querySize, QueryType queryType, @Nullable DefaultCursorQueryOrderField[] orderFields,
                                                 @Nullable QueryOrderType[] orderTypes) {
        return of(null, nextCursor, querySize, queryType, orderFields, orderTypes);
    }

    public static DefaultCursorQueryOptions of(String prevCursor, String nextCursor, int querySize, QueryType queryType,
                                               @Nullable DefaultCursorQueryOrderField[] orderFields, @Nullable QueryOrderType[] orderTypes) {
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
