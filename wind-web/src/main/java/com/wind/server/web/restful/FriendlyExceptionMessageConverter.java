package com.wind.server.web.restful;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.exception.ExceptionCode;
import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.common.message.MessagePlaceholder;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * 友好的异常消息转换器
 *
 * @author wuxp
 * @date 2024-09-19 16:24
 **/
public interface FriendlyExceptionMessageConverter {

    /**
     * 转换异常消息
     *
     * @param throwable 异常信息
     * @return 转换后便于用户理解的消息
     */
    @Nullable
    String convert(Throwable throwable);

    /**
     * 转换异常消息
     *
     * @param throwable      异常信息
     * @param defaultMessage 默认消息
     * @return 转换后便于用户理解的消息
     */
    default String convert(Throwable throwable, String defaultMessage) {
        String result = convert(throwable);
        return result == null ? defaultMessage : result;
    }

    /**
     * 默认的 FriendlyExceptionMessageConverter
     */
    static FriendlyExceptionMessageConverter defaults() {
        return throwable -> {
            if (throwable instanceof BaseException baseException) {
                ExceptionCode code = baseException.getCode();
                if (Objects.equals(code, DefaultExceptionCode.COMMON_FRIENDLY_ERROR)) {
                    return code.getDesc();
                }
            }
            return throwable.getMessage();
        };
    }

    /**
     * 异常消息国际化
     *
     * @dosc https://www.yuque.com/suiyuerufeng-akjad/wind/vzoygfi6ehphzhvh
     */
    static FriendlyExceptionMessageConverter i18n() {
        return throwable -> {
            if (throwable instanceof BaseException baseException) {
                if (Objects.equals(baseException.getCode(), DefaultExceptionCode.COMMON_FRIENDLY_ERROR)) {
                    return SpringI18nMessageUtils.getMessage(baseException.getCode().getDesc());
                }
                MessagePlaceholder placeholder = baseException.getMessagePlaceholder();
                if (placeholder != null) {
                    return SpringI18nMessageUtils.getMessage(placeholder.getPattern(), placeholder.getArgs());
                }
            }
            return SpringI18nMessageUtils.getMessage(throwable.getMessage());
        };
    }
}
