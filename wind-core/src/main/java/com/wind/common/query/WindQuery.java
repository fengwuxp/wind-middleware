package com.wind.common.query;

import com.wind.common.query.supports.QueryOrderType;
import com.wind.common.query.supports.QueryType;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotNull;


/**
 * win query 对象
 *
 * @param <OrderField> 排序字段类型
 * @author wuxp
 * @date 2025-09-30 14:18
 **/
public interface WindQuery<OrderField> {

    /**
     * @return 查询大小
     */
    @NotNull
    Integer getQuerySize();

    void setQuerySize(@NotNull Integer querySize);

    /**
     * @return 查询类型
     */
    @NotNull
    QueryType getQueryType();

    void setQueryType(@NotNull QueryType queryType);

    /**
     * 排序字段和排序类型按数组顺序一一对应
     *
     * @return 排序字段
     */
    OrderField[] getOrderFields();

    void setOrderFields(OrderField[] orderFields);

    /**
     * @return 排序类型
     */
    @Nullable
    QueryOrderType[] getOrderTypes();

    void setOrderTypes(@NotNull QueryOrderType[] orderTypes);

    /**
     * 是否需要处理排序
     *
     * @return <code>true</code> 需要处理排序
     */
    default boolean shouldOrderBy() {
        OrderField[] orderFields = getOrderFields();
        QueryOrderType[] orderTypes = getOrderTypes();
        if (orderFields == null || orderTypes == null) {
            return false;
        }
        return orderFields.length > 0 && orderFields.length == orderTypes.length;
    }

    /**
     * 是否需要统计总数
     *
     * @return <code>true</code> 需要统计总数
     */
    default boolean shouldCountTotal() {
        return getQueryType().isCountTotal();
    }
}
