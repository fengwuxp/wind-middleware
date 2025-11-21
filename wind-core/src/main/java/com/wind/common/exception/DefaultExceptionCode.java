package com.wind.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 默认提供的通用异常
 *
 * @author wuxp
 * @date 2023-09-22 11:31
 **/
@Getter
@AllArgsConstructor
public enum DefaultExceptionCode implements ExceptionCode {

    BAD_REQUEST("400", "请求无效"),

    UNAUTHORIZED("401", "未登录或身份校验失败"),

    FORBIDDEN("403", "无权限访问该资源"),

    PAYLOAD_TOO_LARGE("413", "上传内容超出大小限制"),

    TO_MANY_REQUESTS("429", "请求过于频繁，请稍后再试"),

    NOT_FOUND("404", "资源未找到"),

    COMMON_ERROR("500", "系统繁忙，请稍后再试"),

    /**
     * 对于某些场景的通用业务异常不希望被用户感知到。
     * 在异常消息处理时 {@link com.wind.server.web.restful.FriendlyExceptionMessageConverter}返回一个通用（易于理解）的业务错误信息
     */
    COMMON_FRIENDLY_ERROR("500", "业务繁忙，请稍后重试");

    private final String code;

    private final String desc;

}
