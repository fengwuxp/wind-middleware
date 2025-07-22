package com.wind.client.retrofit.query;

import com.wind.common.query.supports.QueryType;

import java.io.Serial;
import java.util.HashMap;

/**
 * 由于 retrofit GET 请求默认不支持对象参数，使用 map 对象 hack 模拟对象行为
 * 参见：https://github.com/square/retrofit/issues/2293
 *
 * @author wuxp
 * @date 2024-02-27 16:22
 **/
public abstract class AbstractPageQueryMap extends HashMap<String, Object> {

    @Serial
    private static final long serialVersionUID = -32552044324827597L;

    protected AbstractPageQueryMap() {
        setQueryPage(1);
        setQuerySize(20);
        setQueryType(QueryType.QUERY_BOTH);
    }

    public void setQuerySize(Integer querySize) {
        put("querySize", querySize);
    }

    public void setQueryPage(Integer queryPage) {
        put("queryPage", queryPage);
    }

    public void setQueryType(QueryType queryType) {
        put("queryType", queryType);
    }
}
