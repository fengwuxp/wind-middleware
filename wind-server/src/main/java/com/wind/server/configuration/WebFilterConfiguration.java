package com.wind.server.configuration;

import com.wind.server.web.filters.IndexHtmlResourcesFilter;
import com.wind.server.web.filters.WindWebFilterOrdered;
import com.wind.web.trace.TraceFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.RequestContextFilter;

import java.util.EnumSet;
import java.util.function.Function;

import static com.wind.common.WindConstants.ENABLED_NAME;
import static com.wind.common.WindConstants.INDEX_HTML_FILTER_EXPRESSION;
import static com.wind.common.WindConstants.TRACE_FILTER_EXPRESSION;
import static com.wind.common.WindConstants.TRUE;

/**
 * web filter 相关配置
 *
 * @author wuxp
 * @date 2023-09-23 07:13
 **/
@Configuration
public class WebFilterConfiguration {

    /**
     * index html 资源加载器的 bean name
     */
    public static final String INDEX_HTML_RESOURCE_LOADER_BEAN_NAME = "webIndexHtmlStaticResourceLoader";

    @Bean
    @ConditionalOnProperty(prefix = TRACE_FILTER_EXPRESSION, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
    public FilterRegistrationBean<OrderedRequestContextFilter> orderedRequestContextFilter(RequestContextFilter requestContextFilter) {
        // 设置 OrderedRequestContextFilter 在 TraceFilter 之前
        FilterRegistrationBean<OrderedRequestContextFilter> result = new FilterRegistrationBean<>();
        OrderedRequestContextFilter filter = (OrderedRequestContextFilter) requestContextFilter;
        filter.setOrder(WindWebFilterOrdered.REQUEST_CONTEXT.getOrder());
        result.setFilter(filter);
        result.setOrder(WindWebFilterOrdered.REQUEST_CONTEXT.getOrder());
        // 支持异步请求
        result.setAsyncSupported(true);
        result.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
        return result;
    }

    @Bean
    @ConditionalOnProperty(prefix = TRACE_FILTER_EXPRESSION, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
    public FilterRegistrationBean<TraceFilter> traceFilter() {
        FilterRegistrationBean<TraceFilter> result = new FilterRegistrationBean<>();
        result.setFilter(new TraceFilter());
        result.setOrder(WindWebFilterOrdered.TRACE_FILTER.getOrder());
        // 支持异步请求
        result.setAsyncSupported(true);
        // 同时处理 REQUEST / ASYNC / ERROR
        result.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
        return result;
    }

    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(prefix = INDEX_HTML_FILTER_EXPRESSION, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
    @ConditionalOnBean(name = INDEX_HTML_RESOURCE_LOADER_BEAN_NAME)
    public FilterRegistrationBean<IndexHtmlResourcesFilter> webIndexHtmlResourcesFilter(ApplicationContext context) {
        FilterRegistrationBean<IndexHtmlResourcesFilter> result = new FilterRegistrationBean<>();
        result.setFilter(new IndexHtmlResourcesFilter(context.getBean(INDEX_HTML_RESOURCE_LOADER_BEAN_NAME, Function.class)));
        result.setOrder(WindWebFilterOrdered.INDEX_HTML_RESOURCES_FILTER.getOrder());
        return result;
    }

}
