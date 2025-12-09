package com.wind.common.query.supports;

import org.jspecify.annotations.Nullable;

/**
 * 默认的分页查询参数配置
 *
 * @param <O> 排序字段类型
 * @author wuxp
 * @date 2025-12-09 13:46
 **/
public class DefaultPageQueryOptions<O extends QueryOrderField> extends AbstractPageQuery<O> {

    public static DefaultPageQueryOptions<DefaultOrderField> defaults() {
        return new DefaultPageQueryOptions<>();
    }

    public static DefaultPageQueryOptions<DefaultOrderField> defaults(int querySize) {
        return defaults(1, querySize);
    }

    public static DefaultPageQueryOptions<DefaultOrderField> defaults(int queryPage, int querySize) {
        return of(queryPage, querySize, QueryType.QUERY_BOTH, null, null);
    }

    public static DefaultPageQueryOptions<DefaultOrderField> defaults(int queryPage, int querySize, DefaultOrderField orderField, QueryOrderType orderType) {
        return defaults(queryPage, querySize, new DefaultOrderField[]{orderField}, new QueryOrderType[]{orderType});
    }

    public static DefaultPageQueryOptions<DefaultOrderField> defaults(int queryPage, int querySize, @Nullable DefaultOrderField[] orderFields,
                                                                      @Nullable QueryOrderType[] orderTypes) {
        return of(queryPage, querySize, QueryType.QUERY_BOTH, orderFields, orderTypes);
    }

    public static <F extends QueryOrderField> DefaultPageQueryOptions<F> of(int queryPage, int querySize, QueryType queryType, @Nullable F[] orderFields,
                                                                            @Nullable QueryOrderType[] orderTypes) {
        DefaultPageQueryOptions<F> result = new DefaultPageQueryOptions<>();
        result.setQueryPage(queryPage);
        result.setQuerySize(querySize);
        result.setQueryType(queryType);
        result.setOrderFields(orderFields);
        result.setOrderTypes(orderTypes);
        return result;
    }
}
