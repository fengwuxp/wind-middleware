package com.wind.common.query.supports;

/**
 * 页面分页查询配置，一般用于控制器接收分页配置需动态指定的排序字段类型
 *
 * @author wuxp
 * @date 2026-02-03 17:25
 **/
public class SortablePageQuery<O extends QueryOrderField> extends AbstractPageQuery<O> {
}
