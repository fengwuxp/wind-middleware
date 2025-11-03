package com.wind.security.captcha;

import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * 验证码存储器
 *
 * @author wuxp
 * @date 2023-09-24 10:06
 **/
public interface CaptchaStorage {

    /**
     * 保存验证码
     *
     * @param captcha 验证码
     */
    void store(Captcha captcha);

    /**
     * 获取验证码
     *
     * @param type     验证码类型
     * @param useScene 验证码使用场景
     * @param owner    验证码 owner
     */
    @Nullable
    Captcha get(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene, String owner);

    /**
     * 删除验证码
     *
     * @param type     验证码类型
     * @param useScene 验证码使用场景
     * @param owner    验证码 owner
     */
    void remove(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene, String owner);

    /**
     * 获取验证码发送流控滑动窗口
     *
     * @param type  验证码类型
     * @param owner 验证码 owner
     */
    @NotNull
    List<Long> getCaptchaSendSlideWindows(Captcha.CaptchaType type, String owner);

    /**
     * 更新验证码发送流控滑动窗口
     *
     * @param type  验证码类型
     * @param owner 验证码 owner
     * @param times 验证码发送次数
     */
    void putCaptchaSendSlideWindows(Captcha.CaptchaType type, String owner, List<Long> times);

}
