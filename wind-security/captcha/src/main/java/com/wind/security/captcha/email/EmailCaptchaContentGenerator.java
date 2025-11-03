package com.wind.security.captcha.email;

import com.wind.security.captcha.Captcha;
import com.wind.security.captcha.SimpleCaptchaType;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaContentGenerator;

import java.util.Objects;

/**
 * 邮箱验证码内容生成器
 *
 * @author wuxp
 * @date 2023-09-24 13:31
 **/
public class EmailCaptchaContentGenerator extends MobilePhoneCaptchaContentGenerator {

    public EmailCaptchaContentGenerator(EmailCaptchaProperties properties) {
        super(properties);
    }

    @Override
    public boolean supports(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene) {
        return Objects.equals(type, SimpleCaptchaType.EMAIL);
    }
}
