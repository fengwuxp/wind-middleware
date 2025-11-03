package com.wind.security.captcha;

/**
 * 验证码内容生成器
 *
 * @author wuxp
 * @date 2023-09-24 09:39
 **/
public interface CaptchaContentGenerator extends Captcha.CaptchaConfiguration {

    /**
     * @return 获取验证码值
     */
    CaptchaValue getValue(String owner, Captcha.CaptchaUseScene useScene);

    /**
     * 使用支持处理
     *
     * @param type     验证码类型
     * @param useScene 验证码使用场景
     * @return if <code>true</code> 支持
     */
    boolean supports(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene);
}
