package com.wind.security.captcha.configuration;

import com.wind.common.WindConstants;
import com.wind.security.captcha.CaptchaContentProvider;
import com.wind.security.captcha.CaptchaGenerateChecker;
import com.wind.security.captcha.CaptchaStorage;
import com.wind.security.captcha.DefaultCaptchaManager;
import com.wind.security.captcha.SimpleCaptchaGenerateChecker;
import com.wind.security.captcha.email.EmailCaptchaContentProvider;
import com.wind.security.captcha.mobile.MobilePhoneCaptchaContentProvider;
import com.wind.security.captcha.picture.PictureCaptchaContentProvider;
import com.wind.security.captcha.picture.PictureGenerator;
import com.wind.security.captcha.storage.CacheCaptchaStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

/**
 * @author wuxp
 * @date 2023-09-24 15:33
 **/
@Configuration
@EnableConfigurationProperties(value = {CaptchaProperties.class})
@ConditionalOnBean({CacheManager.class})
@ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class CaptchaAutoConfiguration {

    @Bean
    @ConditionalOnBean({CaptchaStorage.class, CaptchaGenerateChecker.class})
    public DefaultCaptchaManager defaultCaptchaManager(Collection<CaptchaContentProvider> delegates, CaptchaStorage captchaStorage,
                                                       CaptchaGenerateChecker generateLimiter, CaptchaProperties properties) {
        return new DefaultCaptchaManager(delegates, captchaStorage, generateLimiter, properties.isVerificationIgnoreCase());
    }

    @Bean
    @ConditionalOnBean({CaptchaStorage.class})
    public DefaultCaptchaManager defaultCaptchaManager(Collection<CaptchaContentProvider> delegates, CaptchaStorage captchaStorage, CaptchaProperties properties) {
        return new DefaultCaptchaManager(delegates, captchaStorage, properties.isVerificationIgnoreCase());
    }

    @Bean
    @ConditionalOnBean({PictureGenerator.class})
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
    @ConditionalOnBean(CacheManager.class)
    @ConditionalOnMissingBean(CacheCaptchaStorage.class)
    public CacheCaptchaStorage cacheCaptchaStorage(CacheManager cacheManager, CaptchaProperties properties) {
        return new CacheCaptchaStorage(cacheManager, properties.getGroup());
    }

    @Bean
    @ConditionalOnBean(CacheManager.class)
    @ConditionalOnMissingBean(CaptchaGenerateChecker.class)
    public CaptchaGenerateChecker simpleCaptchaGenerateChecker(CacheManager cacheManager, CaptchaProperties properties) {
        return new SimpleCaptchaGenerateChecker(cacheManager, WindConstants.DEFAULT_TEXT.toUpperCase(), properties::getMxAllowGenerateTimesOfUserWithDay);
    }
}

