package com.wind.security.captcha.mobile;

import com.wind.common.WindConstants;
import com.wind.security.captcha.Captcha;
import com.wind.security.captcha.CaptchaContentGenerator;
import com.wind.security.captcha.CaptchaValue;
import com.wind.security.captcha.SimpleCaptchaType;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomUtils;

import java.time.Duration;
import java.util.Objects;

/**
 * 手机验证码内容生成器
 *
 * @author wuxp
 * @date 2023-09-24 13:31
 **/
@AllArgsConstructor
public class MobilePhoneCaptchaContentGenerator implements CaptchaContentGenerator {

    private final MobilePhoneCaptchaProperties properties;

    @Override
    public Duration getEffectiveTime() {
        return properties.getEffectiveTime();
    }

    @Override
    public int getMaxAllowVerificationTimes() {
        return properties.getMaxAllowVerificationTimes();
    }

    @Override
    public CaptchaValue getValue(String owner, Captcha.CaptchaUseScene useScene) {
        return CaptchaValue.of(genCaptchaValue(), WindConstants.EMPTY);
    }

    private String genCaptchaValue() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < properties.getLength(); i++) {
            result.append(RandomUtils.nextInt(0, 9));
        }
        return result.toString();
    }

    @Override
    public boolean supports(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene) {
        return Objects.equals(type, SimpleCaptchaType.MOBILE_PHONE);
    }
}
