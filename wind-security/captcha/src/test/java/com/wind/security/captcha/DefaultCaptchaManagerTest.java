package com.wind.security.captcha;

import com.wind.common.exception.BaseException;
import com.wind.security.captcha.configuration.CaptchaProperties;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaContentProvider;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaProperties;
import com.wind.security.captcha.picture.PictureCaptchaContentProvider;
import com.wind.security.captcha.picture.PictureCaptchaProperties;
import com.wind.security.captcha.picture.SimplePictureGenerator;
import com.wind.security.captcha.qrcode.QrCodeCaptchaContentProvider;
import com.wind.security.captcha.qrcode.QrCodeCaptchaProperties;
import com.wind.security.captcha.storage.CacheCaptchaStorage;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.wind.security.captcha.CaptchaI18nMessageKeys.CAPTCHA_GENERATE_MAX_LIMIT_OF_USER_BY_DAY;
import static com.wind.security.captcha.CaptchaI18nMessageKeys.CAPTCHA_VERITY_FAILURE;

class DefaultCaptchaManagerTest {

    private DefaultCaptchaManager captchaManager;

    private CaptchaProperties properties;

    @BeforeEach
    void setup() {
        properties = new CaptchaProperties();
        CaptchaGenerateChecker checker = new SimpleCaptchaGenerateChecker(new ConcurrentMapCacheManager(), properties);
        captchaManager = new DefaultCaptchaManager(getProviders(), getCaptchaStorage(), checker);
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
            String owner = RandomStringUtils.randomAlphanumeric(12);
            Captcha captcha = captchaManager.generate(type, scene, owner);
            Assertions.assertNotNull(captcha);
            captchaManager.verify(captcha.getValue(), type, scene, captcha.getOwner());
            Captcha next = captchaManager.getCaptchaStorage().get(captcha.getType(), captcha.getUseScene(), captcha.getOwner());
            Assertions.assertNull(next);
        }
    }

    @Test
    void testPictureCaptchaWithError() {
        assertCaptchaError(SimpleCaptchaType.PICTURE, 1);
    }

    @Test
    void testMobileCaptchaWithError() {
        assertCaptchaError(SimpleCaptchaType.MOBILE_PHONE, 5);
    }

    @Test
    void testQrCodeCaptchaWithError() {
        assertCaptchaError(SimpleCaptchaType.QR_CODE, 15);
    }

    @Test
    void testMobileCaptchaGenerateFlowControl() {
        String owner = RandomStringUtils.randomAlphanumeric(11);
        for (int i = 0; i < properties.getMobilePhone().getFlowControl().getSpeed(); i++) {
            Captcha captcha = captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
            Assertions.assertNotNull(captcha);
        }
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.REGISTER, owner));
        Assertions.assertEquals(CaptchaI18nMessageKeys.CAPTCHA_FLOW_CONTROL, exception.getMessage());
    }

    @Test
    void testMobileCaptchaCurrentGenerate() throws Exception {
        properties.getMobilePhone().getFlowControl().setSpeed(100);
        properties.getMobilePhone().setMxAllowGenerateTimesOfUserWithDay(100);
        String owner = RandomStringUtils.randomAlphanumeric(11);
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Future<?> future = executorService.submit((Callable<Object>) () -> captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.REGISTER, owner));
            futures.add(future);
        }
        ExecutionException exception = Assertions.assertThrows(ExecutionException.class, () -> {
            for (Future<?> future : futures) {
                future.get();
            }
        });
        Assertions.assertEquals(CaptchaI18nMessageKeys.CAPTCHA_CONCURRENT_GENERATE, exception.getCause().getMessage());
    }

    @Test
    void testMobileCaptchaGenerateLimit() {
        properties.getMobilePhone().getFlowControl().setSpeed(100);
        String owner = RandomStringUtils.randomAlphanumeric(11);
        int maxAllowGenerateTimesOfUserByDay = properties.getMaxAllowGenerateTimesOfUserByDay(SimpleCaptchaType.MOBILE_PHONE);
        for (int i = 0; i < maxAllowGenerateTimesOfUserByDay; i++) {
            Captcha captcha = captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.LOGIN, owner);
            Assertions.assertNotNull(captcha);
        }
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> captchaManager.generate(SimpleCaptchaType.MOBILE_PHONE, SimpleUseScene.REGISTER, owner));
        Assertions.assertEquals(CAPTCHA_GENERATE_MAX_LIMIT_OF_USER_BY_DAY, exception.getMessage());
    }

    private void assertCaptchaError(Captcha.CaptchaType type, int maxAllowVerificationTimes) {
        for (Captcha.CaptchaUseScene scene : SimpleUseScene.values()) {
            String owner = RandomStringUtils.randomAlphanumeric(12);
            Captcha captcha = captchaManager.generate(type, scene, owner);
            Assertions.assertNotNull(captcha);
            String expected = RandomStringUtils.randomAlphanumeric(4);
            BaseException exception = Assertions.assertThrows(BaseException.class, () -> captchaManager.verify(expected, type, scene, owner));
            Assertions.assertEquals(CAPTCHA_VERITY_FAILURE, exception.getMessage());
            Captcha next = captchaManager.getCaptchaStorage().get(captcha.getType(), captcha.getUseScene(), owner);
            if (maxAllowVerificationTimes <= 1) {
                Assertions.assertNull(next);
            } else {
                Assertions.assertNotNull(next);
            }
        }
    }

    private Collection<CaptchaContentProvider> getProviders() {
        return Arrays.asList(
                new PictureCaptchaContentProvider(new PictureCaptchaProperties(), new SimplePictureGenerator()),
                new MobilePhoneCaptchaContentProvider(new MobilePhoneCaptchaProperties()),
                new QrCodeCaptchaContentProvider(() -> "100", new QrCodeCaptchaProperties())
        );
    }

    private static CaptchaStorage getCaptchaStorage() {
        return new CacheCaptchaStorage(new ConcurrentMapCacheManager());
    }
}