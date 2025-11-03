package com.wind.security.captcha;

import org.springframework.lang.NonNull;

/**
 * 图片掩码相关常量
 *
 * @author wuxp
 * @date 2023-10-04 12:16
 **/
public final class CaptchaConstants {

    /**
     * 图片验证码缓存缓存 key
     */
    private static final String CACHE_CAPTCHA_STORE_KEY = "CAPTCHA_CACHE";

    /**
     * 验证码生成次数缓存缓存 key
     */
    private static final String CACHE_CAPTCHA_GENERATE_COUNT_STORE_KEY = "CAPTCHA_COUNT_CACHE";

    private CaptchaConstants() {
        throw new AssertionError();
    }

    /**
     * @param group      业务分组
     * @param captchaTyp 验证码类型
     * @param useScene   验证码使用场景
     * @return 验证码缓存名称
     */
    @NonNull
    public static String getCaptchaCacheName(String group, Captcha.CaptchaType captchaTyp, Captcha.CaptchaUseScene useScene) {
        return String.format("%s_%s_%s_%s", group, captchaTyp.name(), useScene.name(), CACHE_CAPTCHA_STORE_KEY);
    }

    /**
     * @param group      业务分组
     * @param captchaTyp 验证码类型
     * @return 对于同一个用户验证码可发送生成次数缓存名称
     */
    @NonNull
    public static String getCaptchaAllowGenTimesCacheName(String group, Captcha.CaptchaType captchaTyp) {
        return String.format("%s_%s_%s", group, captchaTyp.name(), CACHE_CAPTCHA_GENERATE_COUNT_STORE_KEY);
    }
}
