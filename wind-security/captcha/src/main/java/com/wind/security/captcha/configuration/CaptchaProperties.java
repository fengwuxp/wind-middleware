package com.wind.security.captcha.configuration;

import com.wind.common.WindConstants;
import com.wind.security.captcha.Captcha;
import com.wind.security.captcha.SimpleCaptchaType;
import com.wind.security.captcha.email.EmailCaptchaProperties;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaProperties;
import com.wind.security.captcha.picture.PictureCaptchaProperties;
import com.wind.security.captcha.qrcode.QrCodeCaptchaProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * @author wuxp
 * @date 2023-09-24 15:33
 **/
@Data
@ConfigurationProperties(prefix = CaptchaProperties.PREFIX)
public class CaptchaProperties {

    /**
     * 配置 prefix
     */
    public static final String PREFIX = "wind.security.captcha";

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 验证时忽略大小写
     */
    private boolean verificationIgnoreCase = true;

    /**
     * 业务分组
     */
    private String group = WindConstants.DEFAULT_TEXT.toUpperCase();

    /**
     * 手机短信验证码
     */
    private MobilePhoneCaptchaProperties mobilePhone = new MobilePhoneCaptchaProperties();

    /**
     * 邮箱验证码
     */
    private EmailCaptchaProperties email = new EmailCaptchaProperties();

    /**
     * 图片验证码
     */
    private PictureCaptchaProperties picture = new PictureCaptchaProperties();

    /**
     * 二维码
     */
    private QrCodeCaptchaProperties qrCode = new QrCodeCaptchaProperties();

    /**
     * 一个用户每天允许生成的验证码的最大次数
     *
     * @param type 验证码类型
     * @return 发送次数
     */
    public int getMaxAllowGenerateTimesOfUserByDay(Captcha.CaptchaType type) {
        if (Objects.equals(SimpleCaptchaType.MOBILE_PHONE, type)) {
            return mobilePhone.getMxAllowGenerateTimesOfUserWithDay();
        }

        if (Objects.equals(SimpleCaptchaType.EMAIL, type)) {
            return email.getMxAllowGenerateTimesOfUserWithDay();
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 获取验证码发送的流控配置
     *
     * @param type 验证码类型
     * @return 流控配置，null 表示不限制
     */
    @Nullable
    public Captcha.CaptchaFlowControl getFlowControl(Captcha.CaptchaType type) {
        if (Objects.equals(SimpleCaptchaType.MOBILE_PHONE, type)) {
            return mobilePhone.getFlowControl();
        }

        if (Objects.equals(SimpleCaptchaType.EMAIL, type)) {
            return email.getFlowControl();
        }
        return null;
    }
}
