package com.wind.common.query.supports;

import org.jspecify.annotations.Nullable;

/**
 * 默认的分页查询参数配置
 *
 * @author wuxp
 * @date 2025-12-09 13:46
 **/
public final class DefaultPageQueryOptions extends AbstractPageQuery<DefaultOrderField> {

    public static DefaultPageQueryOptions desc(DefaultOrderField field) {
        return order(field, QueryOrderType.DESC);
    }

    public static DefaultPageQueryOptions asc(DefaultOrderField field) {
        return order(field, QueryOrderType.ASC);
    }

    public static DefaultPageQueryOptions order(DefaultOrderField field, QueryOrderType orderType) {
        return defaults(1, 20, field, orderType);
    }

    public static DefaultPageQueryOptions defaults() {
        return new DefaultPageQueryOptions();
    }

    public static DefaultPageQueryOptions defaults(int querySize) {
        return defaults(1, querySize);
    }

    public static DefaultPageQueryOptions defaults(int queryPage, int querySize) {
        return of(queryPage, querySize, QueryType.QUERY_BOTH, null, null);
    }

    public static DefaultPageQueryOptions defaults(int queryPage, int querySize, DefaultOrderField orderField, QueryOrderType orderType) {
        return defaults(queryPage, querySize, new DefaultOrderField[]{orderField}, new QueryOrderType[]{orderType});
    }

    public static DefaultPageQueryOptions defaults(int queryPage, int querySize, @Nullable DefaultOrderField[] orderFields,
                                                   @Nullable QueryOrderType[] orderTypes) {
        return of(queryPage, querySize, QueryType.QUERY_BOTH, orderFields, orderTypes);
    }

    public static DefaultPageQueryOptions of(int queryPage, int querySize, QueryType queryType, @Nullable DefaultOrderField[] orderFields,
                                             @Nullable QueryOrderType[] orderTypes) {
        DefaultPageQueryOptions result = new DefaultPageQueryOptions();
        result.setQueryPage(queryPage);
        result.setQuerySize(querySize);
        result.setQueryType(queryType);
        result.setOrderFields(orderFields);
        result.setOrderTypes(orderTypes);
        return result;
    }
}
