package com.wind.server.web.filters;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.Ordered;

/**
 * @author wuxp
 * @date 2023-09-23 07:19
 **/
@AllArgsConstructor
@Getter
public enum WindWebFilterOrdered implements Ordered, DescriptiveEnum {

    /**
     * @see org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter
     */
    REQUEST_CONTEXT(Ordered.HIGHEST_PRECEDENCE + 10, "OrderedRequestContextFilter"),

    TRACE_FILTER(Ordered.HIGHEST_PRECEDENCE + 30, "TraceFilter"),

    INDEX_HTML_RESOURCES_FILTER(Ordered.HIGHEST_PRECEDENCE + 100, "IndexHtmlResourcesFilter"),

    REQUEST_SIGN_FILTER(Ordered.HIGHEST_PRECEDENCE + 1030, "RequestSignFilter"),

    HTTP_REQEUST_IDEMPOTENT_FILTER(REQUEST_SIGN_FILTER.getOrder() + 100, "HttpRequestIdempotentFilter"),
    ;

    private final int order;

    private final String desc;

}
