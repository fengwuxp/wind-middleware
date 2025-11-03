package com.wind.security.captcha.qrcode;

import com.wind.security.captcha.Captcha;
import com.wind.security.captcha.CaptchaContentGenerator;
import com.wind.security.captcha.CaptchaValue;
import com.wind.security.captcha.SimpleCaptchaType;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 二维码验证码生成器
 *
 * @param qrCodeValueSupplier 二维码值提供者
 * @author wuxp
 * @date 2023-09-24 14:12
 */
public record QrCodeCaptchaContentGenerator(Supplier<String> qrCodeValueSupplier, QrCodeGenerator qrCodeGenerator,
                                            QrCodeCaptchaProperties properties) implements CaptchaContentGenerator {

    public QrCodeCaptchaContentGenerator(Supplier<String> qrCodeValueSupplier, QrCodeCaptchaProperties properties) {
        this(qrCodeValueSupplier, new ZxingQrCodeGenerator(), properties);
    }

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
        String value = qrCodeValueSupplier.get();
        String content = qrCodeGenerator.generateAsBas64(value, properties.getQrCode());
        return CaptchaValue.of(value, content);
    }

    @Override
    public boolean supports(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene) {
        return Objects.equals(type, SimpleCaptchaType.QR_CODE);
    }
}
