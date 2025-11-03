package com.wind.security.captcha;

import jakarta.validation.constraints.NotNull;

/**
 * 验证码发送者
 *
 * @author wuxp
 * @date 2025-11-03 10:42
 **/
public interface CaptchaSender {

    /**
     * 发送验证码
     *
     * @param captcha 验证码
     */
    void send(@NotNull Captcha captcha);

    /**
     * 使用支持处理
     *
     * @param type 验证码类型
     * @return if <code>true</code> 支持
     */
    default boolean supports(Captcha.CaptchaType type) {
        return true;
    }
}
