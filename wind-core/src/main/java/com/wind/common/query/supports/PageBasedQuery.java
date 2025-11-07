package com.wind.common.query.supports;


import com.wind.common.query.WindQuery;
import jakarta.validation.constraints.NotNull;


/**
 * 基于页码分的页查询对象
 *
 * @author wuxp
 * @date 2025-01-16 16:20
 **/
public interface PageBasedQuery<OrderField> extends WindQuery<OrderField> {

    /**
     * @return 查询页码
     */
    @NotNull
    Integer getQueryPage();

    void setQueryPage(@NotNull Integer queryPage);


}
