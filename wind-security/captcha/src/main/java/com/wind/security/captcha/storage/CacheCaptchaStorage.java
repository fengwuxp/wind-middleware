package com.wind.security.captcha.storage;

import com.wind.common.WindConstants;
import com.wind.common.WindDateFormater;
import com.wind.common.exception.AssertUtils;
import com.wind.security.captcha.Captcha;
import com.wind.security.captcha.CaptchaConstants;
import com.wind.security.captcha.CaptchaStorage;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @param group 业务模块分组
 * @author wuxp
 * @date 2023-09-24 14:43
 */
public record CacheCaptchaStorage(CacheManager cacheManager, String group) implements CaptchaStorage {

    public CacheCaptchaStorage(CacheManager cacheManager) {
        this(cacheManager, WindConstants.DEFAULT_TEXT.toUpperCase());
    }

    @Override
    public void store(Captcha captcha) {
        requiredCache(captcha.type(), captcha.useScene()).put(encoding(captcha.owner()), captcha);
    }

    @Override
    public Captcha get(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene, String owner) {
        return requiredCache(type, useScene).get(encoding(owner), Captcha.class);
    }

    @Override
    public void remove(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene, String owner) {
        requiredCache(type, useScene).evict(encoding(owner));
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public List<Long> getCaptchaSendSlideWindows(Captcha.CaptchaType type, String owner) {
        Cache cache = requireCaptchaGenTimesCache(type);
        List<Long> times = cache.get(getCaptchaGenTimesCacheKey(owner), List.class);
        if (times == null) {
            times = new ArrayList<>();
        }
        return times;
    }

    @Override
    public void putCaptchaSendSlideWindows(Captcha.CaptchaType type, String owner, List<Long> times) {
        Cache cache = requireCaptchaGenTimesCache(type);
        cache.put(getCaptchaGenTimesCacheKey(owner), times);
    }

    @NonNull
    private Cache requiredCache(Captcha.CaptchaType captchaTyp, Captcha.CaptchaUseScene useScene) {
        String name = CaptchaConstants.getCaptchaCacheName(group, captchaTyp, useScene);
        Cache result = cacheManager.getCache(name);
        AssertUtils.notNull(result, String.format("get captcha cache failure，cacha name = %s", name));
        return result;
    }

    @NonNull
    private Cache requireCaptchaGenTimesCache(Captcha.CaptchaType type) {
        String name = CaptchaConstants.getCaptchaAllowGenTimesCacheName(group, type);
        Cache result = cacheManager.getCache(name);
        AssertUtils.notNull(result, String.format("get captcha send times cache failure, cache name = %s", name));
        return result;
    }

    private String getCaptchaGenTimesCacheKey(String owner) {
        return String.format("%s_%s", encoding(owner), WindDateFormater.YYYYMMDDHH.format(LocalDateTime.now()));
    }

    private String encoding(String key) {
        return Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }
}

