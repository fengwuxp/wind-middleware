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
public class DefaultCursorQueryOptions extends AbstractCursorQuery<DefaultCursorQueryOrderField> {

    public static DefaultCursorQueryOptions next(String nextCursor) {
        return next(nextCursor, 20);
    }

    public static DefaultCursorQueryOptions next(String nextCursor, int querySize) {
        return of(nextCursor, querySize, QueryType.QUERY_RESET, null, null);
    }

    public static DefaultCursorQueryOptions next(String nextCursor, int querySize, DefaultCursorQueryOrderField orderField, QueryOrderType orderType) {
        return next(nextCursor, querySize, new DefaultCursorQueryOrderField[]{orderField}, new QueryOrderType[]{orderType});
    }

    public static DefaultCursorQueryOptions next(String nextCursor, int querySize, @Nullable DefaultCursorQueryOrderField[] orderFields,
                                                 @Nullable QueryOrderType[] orderTypes) {
        return of(nextCursor, querySize, QueryType.QUERY_RESET, orderFields, orderTypes);
    }

    public static DefaultCursorQueryOptions of(String nextCursor, int querySize, QueryType queryType, @Nullable DefaultCursorQueryOrderField[] orderFields,
                                               @Nullable QueryOrderType[] orderTypes) {
        DefaultCursorQueryOptions result = new DefaultCursorQueryOptions();
        result.setNextCursor(nextCursor);
        result.setQuerySize(querySize);
        result.setQueryType(queryType);
        result.setOrderFields(orderFields);
        result.setOrderTypes(orderTypes);
        return result;
    }

}
