package com.wind.security.captcha.configuration;

import com.wind.common.locks.LockFactory;
import com.wind.security.captcha.CaptchaContentProvider;
import com.wind.security.captcha.CaptchaGenerateChecker;
import com.wind.security.captcha.CaptchaManager;
import com.wind.security.captcha.CaptchaManagerLocksWrapper;
import com.wind.security.captcha.CaptchaStorage;
import com.wind.security.captcha.DefaultCaptchaManager;
import com.wind.security.captcha.SimpleCaptchaGenerateChecker;
import com.wind.security.captcha.email.EmailCaptchaContentProvider;
import com.wind.security.captcha.email.EmailCaptchaProperties;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaContentProvider;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaProperties;
import com.wind.security.captcha.picture.PictureCaptchaContentProvider;
import com.wind.security.captcha.picture.PictureCaptchaProperties;
import com.wind.security.captcha.picture.PictureGenerator;
import com.wind.security.captcha.picture.SimplePictureGenerator;
import com.wind.security.captcha.qrcode.QrCodeCaptchaProperties;
import com.wind.security.captcha.storage.CacheCaptchaStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

import static com.wind.common.WindConstants.ENABLED_NAME;
import static com.wind.common.WindConstants.TRUE;

/**
 * @author wuxp
 * @date 2023-09-24 15:33
 **/
@Configuration
@EnableConfigurationProperties(value = {CaptchaProperties.class})
@ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
public class CaptchaAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = CaptchaProperties.PREFIX + ".mobile-phone")
    public MobilePhoneCaptchaProperties mobilePhoneCaptchaProperties() {
        return new MobilePhoneCaptchaProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = CaptchaProperties.PREFIX + ".email")
    public EmailCaptchaProperties emailCaptchaProperties() {
        return new EmailCaptchaProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = CaptchaProperties.PREFIX + ".picture")
    public PictureCaptchaProperties pictureCaptchaProperties() {
        return new PictureCaptchaProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = CaptchaProperties.PREFIX + ".qr-code")
    public QrCodeCaptchaProperties qrCodeCaptchaProperties() {
        return new QrCodeCaptchaProperties();
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    @ConditionalOnMissingBean(CaptchaStorage.class)
    public CacheCaptchaStorage cacheCaptchaStorage(CacheManager cacheManager, CaptchaProperties properties) {
        return new CacheCaptchaStorage(cacheManager, properties.getGroup());
    }

    @Bean
    @ConditionalOnMissingBean({CaptchaGenerateChecker.class})
    public SimpleCaptchaGenerateChecker simpleCaptchaGenerateChecker(CacheManager cacheManager, CaptchaProperties properties) {
        return new SimpleCaptchaGenerateChecker(cacheManager, properties);
    }

    @Bean
    @ConditionalOnMissingBean(PictureGenerator.class)
    public SimplePictureGenerator simplePictureGenerator() {
        return new SimplePictureGenerator();
    }

    @Bean
    @ConditionalOnBean({PictureGenerator.class})
    @ConditionalOnMissingBean(PictureCaptchaContentProvider.class)
    public PictureCaptchaContentProvider pictureCaptchaContentProvider(CaptchaProperties properties, PictureGenerator pictureGenerator) {
        return new PictureCaptchaContentProvider(properties.getPicture(), pictureGenerator);
    }

    @Bean
    @ConditionalOnMissingBean(MobilePhoneCaptchaContentProvider.class)
    public MobilePhoneCaptchaContentProvider mobilePhoneCaptchaContentProvider(CaptchaProperties properties) {
        return new MobilePhoneCaptchaContentProvider(properties.getMobilePhone());
    }

    @Bean
    @ConditionalOnMissingBean(EmailCaptchaContentProvider.class)
    public EmailCaptchaContentProvider emailCaptchaContentProvider(CaptchaProperties properties) {
        return new EmailCaptchaContentProvider(properties.getEmail());
    }

    @Bean
    @ConditionalOnBean({CaptchaContentProvider.class, CaptchaStorage.class, CaptchaGenerateChecker.class, LockFactory.class})
    public CaptchaManager defaultCaptchaManager(Collection<CaptchaContentProvider> delegates, CaptchaStorage captchaStorage,
                                                CaptchaGenerateChecker generateLimiter, CaptchaProperties properties, LockFactory lockFactory) {
        DefaultCaptchaManager result = new DefaultCaptchaManager(delegates, captchaStorage, generateLimiter, properties.isVerificationIgnoreCase());
        return new CaptchaManagerLocksWrapper(result, lockFactory);
    }

}

