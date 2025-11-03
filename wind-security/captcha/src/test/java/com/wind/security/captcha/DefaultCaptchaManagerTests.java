package com.wind.security.captcha;

import com.wind.common.exception.BaseException;
import com.wind.security.captcha.configuration.CaptchaProperties;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaContentGenerator;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaProperties;
import com.wind.security.captcha.picture.PictureCaptchaContentGenerator;
import com.wind.security.captcha.picture.PictureCaptchaProperties;
import com.wind.security.captcha.picture.SimplePictureGenerator;
import com.wind.security.captcha.qrcode.QrCodeCaptchaContentGenerator;
import com.wind.security.captcha.qrcode.QrCodeCaptchaProperties;
import com.wind.security.captcha.storage.CacheCaptchaStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.wind.security.captcha.CaptchaI18nMessageKeys.CAPTCHA_GENERATE_MAX_LIMIT_OF_USER_BY_DAY;
import static com.wind.security.captcha.DefaultCaptchaManager.ALLOW_USE_PREVIOUS_CAPTCHA_TYPES;

@Slf4j
class DefaultCaptchaManagerTests {

    private DefaultCaptchaManager captchaManager;

    private CaptchaProperties properties;

    @BeforeEach
    void setup() {
        properties = new CaptchaProperties();
        captchaManager = new DefaultCaptchaManager(getGenerators(), mockSenders(), getCaptchaStorage(), properties);
    }

    @Test
    void testPictureCaptchaWhitPaas() {
        assertCaptchaPaas(SimpleCaptchaType.PICTURE);
    }

    @Test
    void testMobileCaptchaWhitPaas() {
        assertCaptchaPaas(SimpleCaptchaType.MOBILE_PHONE);
    }

    @Test
    void tesQrCodeCaptchaWhitPaas() {
        assertCaptchaPaas(SimpleCaptchaType.QR_CODE);
    }

    private void assertCaptchaPaas(Captcha.CaptchaType type) {
        for (Captcha.CaptchaUseScene scene : SimpleUseScene.values()) {
            String owner = RandomStringUtils.secure().nextAlphanumeric(12);
            Captcha captcha = captchaManager.generate(type, scene, owner);
            Assertions.assertNotNull(captcha);
            if (ALLOW_USE_PREVIOUS_CAPTCHA_TYPES.contains(type)) {
                captcha = captchaManager.generate(type, scene, owner);
            }
            captchaManager.verify(captcha.value(), type, scene, captcha.owner());
            Assertions.assertNull(captchaManager.captchaStorage().get(captcha.type(), captcha.useScene(), captcha.owner()));
        }
    }

    @Test
    void testPictureCaptchaWithError() {
        assertCaptchaError(SimpleCaptchaType.PICTURE, 1);
    }

    @Test
    void testMobileCaptchaWithError() {
        assertCaptchaError(SimpleCaptchaType.MOBILE_PHONE, 3);
    }

    @Test
    void testQrCodeCaptchaWithError() {
        assertCaptchaError(SimpleCaptchaType.QR_CODE, 15);
    }

    @Test
    void testMobileCaptchaSendFlowControl() {
        String owner = RandomStringUtils.secure().nextAlphanumeric(11);
        for (int i = 0; i < properties.getMobilePhone().getFlowControl().getSpeed(); i++) {
            Captcha captcha = captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
            Assertions.assertNotNull(captcha);
            captchaManager.send(captcha);
        }
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> captchaManager.send(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.REGISTER, owner));
        Assertions.assertEquals(CaptchaI18nMessageKeys.CAPTCHA_FLOW_CONTROL, exception.getMessage());
    }

    @Test
    void testMobileCaptchaSendLimit() {
        properties.getMobilePhone().getFlowControl().setSpeed(100);
        String owner = RandomStringUtils.secure().nextAlphanumeric(11);
        int maxAllowGenerateTimesOfUserByDay = properties.getMaxAllowGenerateTimesOfUserByDay(SimpleCaptchaType.MOBILE_PHONE);
        for (int i = 0; i < maxAllowGenerateTimesOfUserByDay; i++) {
            captchaManager.send(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
        }
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> captchaManager.send(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.REGISTER, owner));
        Assertions.assertEquals(CAPTCHA_GENERATE_MAX_LIMIT_OF_USER_BY_DAY, exception.getMessage());
    }

    @Test
    void testMobileCaptchaGenerateRepeatedly() {
        // 测试多次发送，验证通过
        String owner = RandomStringUtils.secure().nextAlphanumeric(11);
        Captcha captcha1 = captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
        Captcha captcha2 = captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
        Captcha captcha3 = captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
        Assertions.assertEquals(captcha1.value(), captcha2.value());
        Assertions.assertEquals(captcha1.value(), captcha3.value());
        captchaManager.verify(captcha3.value(), SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
        Assertions.assertNull(captchaManager.captchaStorage().get(captcha1.type(), SimpleUseScene.LOGIN, owner));
        Captcha captcha4 = captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
        Assertions.assertNotEquals(captcha1.value(), captcha4.value());
    }

    private void assertCaptchaError(Captcha.CaptchaType type, int maxAllowVerificationTimes) {
        for (Captcha.CaptchaUseScene scene : SimpleUseScene.values()) {
            String owner = RandomStringUtils.secure().nextAlphanumeric(12);
            Captcha captcha = captchaManager.generate(type, scene, owner);
            Assertions.assertNotNull(captcha);
            String expected = RandomStringUtils.secure().nextAlphanumeric(4);
            BaseException exception = Assertions.assertThrows(BaseException.class, () -> captchaManager.verify(expected, type, scene, owner));
            Assertions.assertEquals(CaptchaI18nMessageKeys.getCaptchaVerityFailure(type), exception.getMessage());
            Captcha result = captchaManager.captchaStorage().get(captcha.type(), captcha.useScene(), owner);
            if (maxAllowVerificationTimes <= 1) {
                Assertions.assertNull(result);
            } else {
                Assertions.assertNotNull(result);
            }
        }
    }

    private Collection<CaptchaContentGenerator> getGenerators() {
        return Arrays.asList(
                new PictureCaptchaContentGenerator(new PictureCaptchaProperties(), new SimplePictureGenerator()),
                new MobilePhoneCaptchaContentGenerator(new MobilePhoneCaptchaProperties()),
                new QrCodeCaptchaContentGenerator(() -> "100", new QrCodeCaptchaProperties())
        );
    }

    private Collection<CaptchaSender> mockSenders() {
        return List.of(captcha -> log.info("send captcha = {}", captcha));
    }

    private static CaptchaStorage getCaptchaStorage() {
        return new CacheCaptchaStorage(new ConcurrentMapCacheManager());
    }
}