package com.wind.security.captcha;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 验证码管理器
 *
 * @author wuxp
 * @date 2023-11-13 21:24
 **/
public interface CaptchaManager {


    /**
     * 生成验证码
     *
     * @param type     验证码类型
     * @param useScene 验证码使用场景
     * @param owner    验证码所有者
     * @return 验证码
     */
    Captcha generate(@NotNull Captcha.CaptchaType type, @NotNull Captcha.CaptchaUseScene useScene,  @NotBlank String owner);

    /**
     * 验证验证码
     *
     * @param expected 预期的值
     * @param type     验证码类型
     * @param useScene 验证码使用场景
     * @param owner    验证码所有者
     */
    void verify(@NotBlank String expected, @NotNull Captcha.CaptchaType type,  @NotNull Captcha.CaptchaUseScene useScene,  @NotBlank String owner);
}

