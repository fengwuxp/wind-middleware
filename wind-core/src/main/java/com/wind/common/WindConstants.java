package com.wind.common;

/**
 * 通用常量类
 *
 * @author wuxp
 */
public final class WindConstants {

    private WindConstants() {
        throw new AssertionError();
    }

    public static final String WIND = "Wind";

    public static final String SPACE = " ";
    public static final String TAB = "\t";
    public static final String DOT = ".";
    public static final String AT = "@";
    public static final String SHARP = "#";
    public static final String EQ = "=";

    public static final String AND = "&";

    public static final String DOUBLE_DOT = "..";
    public static final String SLASH = "/";
    public static final String QUESTION_MARK = "?";
    public static final String BACKSLASH = "\\";
    public static final String EMPTY = "";
    public static final String NULL = "null";
    public static final String CR = "\r";
    public static final String LF = "\n";
    public static final String CRLF = "\r\n";
    public static final String UNDERLINE = "_";
    public static final String DASHED = "-";
    public static final String COMMA = ",";
    public static final String DELIM_START = "{";
    public static final String DELIM_END = "}";
    public static final String BRACKET_START = "[";
    public static final String BRACKET_END = "]";
    public static final String COLON = ":";
    public static final String EMPTY_JSON = "{}";
    public static final String EMPTY_JSON_ARRAY = "[]";


    public static final String DEFAULT_TEXT = "default";

    /**
     * 未知 text
     */
    public static final String UNKNOWN = "unknown";

    /**
     * 开发环境
     */
    public static final String DEV = "dev";

    /**
     * 集成测试环境
     */
    public static final String SIT = "sit";

    /**
     * 联调（稳定）环境
     */
    public static final String STABLE = "stable";

    /**
     * 用户验收测试环境
     */
    public static final String UAT = "uat";

    /**
     * 预付（灰度）环境
     */
    public static final String PRE = "pre";

    /**
     * 生产环境
     */
    public static final String PROD = "prod";

    /**
     * 应用当前激活的环境
     */
    public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    /**
     * 应用名称
     */
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    /**
     * 使用的中间件类型
     */
    public static final String WIND_SERVER_USED_MIDDLEWARE = "wind.server.use.middleware";

    /**
     * 中间在多个应用下共享是，统一配置的配置名称
     */
    public static final String WIND_MIDDLEWARE_SHARE_NAME = "wind.server.middleware.share.name";

    /**
     * Mysql 配置名称
     */
    public static final String WIND_MYSQL_NAME = "wind.mysql.name";

    /**
     * redis 配置名称
     */
    public static final String WIND_REDIS_NAME = "wind.redis.name";

    /**
     * redisson 配置 PropertySource 名称
     */
    public static final String WIND_REDISSON_PROPERTY_SOURCE_NAME = "redissonProperties";

    /**
     * redisson 配置名称
     */
    public static final String SPRING_REDISSON_CONFIG_NAME = "spring.redis.redisson.config";

    /**
     * rocketmq 配置名称
     */
    public static final String WIND_ROCKETMQ_NAME = "wind.rocketmq.name";

    /**
     * sentinel 配置名称
     */
    public static final String WIND_SENTINEL_NAME = "wind.sentinel.name";

    /**
     * elastic-job 名称
     */
    public static final String WIND_ELASTIC_JOB_NAME = "wind.elastic-job.name";

    /**
     * oss 配置名称
     */
    public static final String WIND_OSS_NAME = "wind.oss.name";

    /**
     * dynamic-tp 配置名称
     */
    public static final String WIND_DYNAMIC_TP = "wind.dynamic-tp.name";

    /**
     * redisson 名称
     */
    public static final String REDISSON_NAME = "redisson";

    /**
     * i18n 消息配置
     */
    public static final String WIND_I18N_MESSAGE_PREFIX = "wind.i18n.messages";

    /**
     * 配置前缀
     */
    public static final String WIND_SERVER_PROPERTIES_PREFIX = "wind.server";

    public static final String ENABLED_NAME = "enabled";

    public static final String FALSE = "false";

    public static final String TRUE = "true";

    /**
     * http status 同步设置 advice
     */
    public static final String WIND_SERVER_HTTP_RESPONSE_STATUS_ADVICE = "wind.server.http-status.advice";

    /**
     * 对象脱敏 advice
     */
    public static final String WIND_SERVER_OBJECT_MASK_ADVICE = "wind.server.object-mask.advice";

    /**
     * 配置中心支持
     */
    public static final String WIND_SERVER_CONFIG_CENTER_PROPERTIES_PREFIX = "wind.server.config-center";

    /**
     * index html Filter 开启表达式
     */
    public static final String INDEX_HTML_FILTER_EXPRESSION = "wind.server.filter.index-html-filter";

    /**
     * 请求幂等性过滤器开启表达式
     */
    public static final String HTTP_REQUEST_IDEMPOTENT_FILTER_EXPRESSION = "wind.server.filter.request-idempotent-filter";

    /**
     * trace Filter 开启表达式
     */
    public static final String TRACE_FILTER_EXPRESSION = "wind.server.filter.trace-filter";

    /**
     * 控制器方法拦截配置
     */
    public static final String CONTROLLER_METHOD_ASPECT_NAME = "wind.server.controller-method-aspect";

    /**
     * 应用配置名称
     */
    public static final String GLOBAL_CONFIG_NAME = "wind-global";

    /**
     * 全局配置分组
     */
    public static final String GLOBAL_CONFIG_GROUP = "GLOBAL";

    /**
     * 应用配置分组
     */
    public static final String APP_CONFIG_GROUP = "APP";

    /**
     * 应用共享配置分组
     */
    public static final String APP_SHARE_CONFIG_GROUP = "APP_SHARE";

    /**
     * traceId 变量名称
     */
    public static final String TRACE_ID_NAME = "traceId";

    /**
     * trace id http header name
     */
    public static final String WIND_TRANCE_ID_HEADER_NAME = "Wind-Trace-Id";

    /**
     * http 请求 url 变量
     */
    public static final String HTTP_REQUEST_URL_TRACE_NAME = "requestUrl";

    /**
     * 本机 ipv4 地址
     */
    public static final String LOCAL_HOST_IP_V4 = "localhostIpv4";

    /**
     * 本机 ipv6 地址
     */
    public static final String LOCAL_HOST_IP_V6 = "localhostIpv6";

}
