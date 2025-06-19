package com.wind.common.exception;


import com.wind.common.message.MessageFormatter;
import com.wind.common.message.MessagePlaceholder;
import lombok.Getter;

import java.io.Serial;

/**
 * 通用基础业务异常，不同的业务场景可以继承该类做扩展
 *
 * @author wuxp
 */
@Getter
public class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 319556802147084526L;

    private static final MessageFormatter MESSAGE_FORMATTER = MessageFormatter.java();

    private final ExceptionCode code;

    private final MessagePlaceholder messagePlaceholder;

    private final ExceptionLogLevel logLevel;

    public BaseException(String message) {
        this(DefaultExceptionCode.COMMON_ERROR, ExceptionLogLevel.ERROR, message);
    }

    public BaseException(ExceptionLogLevel level, String message) {
        this(DefaultExceptionCode.COMMON_ERROR, level, message);
    }

    public BaseException(ExceptionCode code, String message) {
        this(code, ExceptionLogLevel.ERROR, message);
    }

    public BaseException(ExceptionCode code, ExceptionLogLevel level, String message) {
        this(code, level, message, null);
    }

    public BaseException(MessagePlaceholder placeholder) {
        this(DefaultExceptionCode.COMMON_ERROR, placeholder);
    }

    public BaseException(ExceptionCode code, MessagePlaceholder placeholder) {
        this(code, placeholder, null);
    }

    public BaseException(ExceptionCode code, String message, Throwable cause) {
        this(code, ExceptionLogLevel.ERROR, message, cause);
    }

    public BaseException(ExceptionCode code, ExceptionLogLevel level, String message, Throwable cause) {
        this(code, MessagePlaceholder.of(message), level, cause);
    }

    public BaseException(ExceptionCode code, MessagePlaceholder placeholder, Throwable cause) {
        this(code, placeholder, ExceptionLogLevel.ERROR, cause);
    }

    public BaseException(ExceptionCode code, MessagePlaceholder placeholder, ExceptionLogLevel level, Throwable cause) {
        super(MESSAGE_FORMATTER.format(placeholder.getPattern(), placeholder.getArgs()), cause);
        this.code = code;
        this.messagePlaceholder = placeholder;
        this.logLevel = level;
    }

    public String getTextCode() {
        return code.getCode();
    }

    public static BaseException badRequest(String message) {
        return new BaseException(DefaultExceptionCode.BAD_REQUEST, message);
    }

    public static BaseException badRequest(MessagePlaceholder message) {
        return new BaseException(DefaultExceptionCode.BAD_REQUEST, message);
    }

    public static BaseException unAuthorized(String message) {
        return new BaseException(DefaultExceptionCode.UNAUTHORIZED, message);
    }

    public static BaseException unAuthorized(ExceptionLogLevel level, String message) {
        return new BaseException(DefaultExceptionCode.UNAUTHORIZED, level, message);
    }

    public static BaseException unAuthorized(MessagePlaceholder message) {
        return new BaseException(DefaultExceptionCode.UNAUTHORIZED, message);
    }

    public static BaseException forbidden(String message) {
        return new BaseException(DefaultExceptionCode.FORBIDDEN, message);
    }

    public static BaseException forbidden(ExceptionLogLevel level, String message) {
        return new BaseException(level, message);
    }

    public static BaseException forbidden(MessagePlaceholder message) {
        return new BaseException(DefaultExceptionCode.FORBIDDEN, message);
    }

    public static BaseException notFound(String message) {
        return new BaseException(DefaultExceptionCode.NOT_FOUND, message);
    }

    public static BaseException notFound(MessagePlaceholder message) {
        return new BaseException(DefaultExceptionCode.NOT_FOUND, message);
    }

    public static BaseException common(String message) {
        return new BaseException(message);
    }

    public static BaseException common(ExceptionLogLevel level, String message) {
        return new BaseException(level, message);
    }

    public static BaseException common(MessagePlaceholder message) {
        return new BaseException(message);
    }

    /**
     * 友好的业务异常
     *
     * @param message 原始错误信息，如果会响应给用户，将会自动转换友好消息
     * @return 业务异常
     * @see DefaultExceptionCode#COMMON_FRIENDLY_ERROR
     */
    public static BaseException friendly(String message) {
        return new BaseException(DefaultExceptionCode.COMMON_FRIENDLY_ERROR, message);
    }

    public static BaseException friendly(MessagePlaceholder message) {
        return new BaseException(DefaultExceptionCode.COMMON_FRIENDLY_ERROR, message);
    }

}
