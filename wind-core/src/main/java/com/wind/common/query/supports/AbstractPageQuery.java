package com.wind.common.query.supports;


import com.wind.common.exception.AssertUtils;
import lombok.Data;
import org.springframework.lang.NonNull;

import jakarta.validation.constraints.NotNull;
import java.beans.Transient;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分页查询对象
 *
 * @author wxup
 */
@Data
public abstract class AbstractPageQuery<OrderField extends QueryOrderField> implements WindPageQuery<OrderField> {

    /**
     * 避免查询页面数据过大，拖垮数据库
     */
    private static final AtomicInteger MAX_QUERY_SIZE = new AtomicInteger(3000);

    /**
     * 查询页码
     */
    @NotNull
    private Integer queryPage = 1;

    /**
     * 查询大小
     */
    @NotNull
    private Integer querySize = 20;

    /**
     * 查询类型
     */
    private QueryType queryType = QueryType.QUERY_BOTH;

    /**
     * 排序字段
     */
    private OrderField[] orderFields;

    /**
     * 排序类型
     */
    private QueryOrderType[] orderTypes;

    @Override
    public void setQuerySize(@NonNull Integer querySize) {
        AssertUtils.isTrue(querySize <= getMaxQuerySize(), () -> String.format("查询大小不能超过：%d", MAX_QUERY_SIZE.get()));
        this.querySize = querySize;
    }

    @NotNull
    public QueryType getQueryType() {
        return queryType == null ? QueryType.QUERY_BOTH : queryType;
    }

    /**
     * 是否需要处理排序
     *
     * @return <code>true</code> 需要处理排序
     */
    public boolean requireOrderBy() {
        if (orderFields == null || orderTypes == null) {
            return false;
        }
        return orderFields.length > 0 && orderFields.length == orderTypes.length;
    }

    /**
     * 配置查询大小最大值
     *
     * @param querySize 查询大小
     */
    public static void configureMaxQuerySize(int querySize) {
        AssertUtils.isTrue(querySize > 0, "查询大小必须大于 0");
        MAX_QUERY_SIZE.set(querySize);
    }

    /**
     * @return 查询大小最大值
     */
    @Transient
    public int getMaxQuerySize() {
        return MAX_QUERY_SIZE.get();
    }

}
