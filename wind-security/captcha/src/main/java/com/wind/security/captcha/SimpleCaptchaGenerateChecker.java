package com.wind.security.captcha;

import com.google.common.collect.ImmutableSet;
import com.wind.common.exception.AssertUtils;
import com.wind.security.captcha.configuration.CaptchaProperties;
import lombok.AllArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.wind.security.captcha.CaptchaI18nMessageKeys.CAPTCHA_GENERATE_MAX_LIMIT_OF_USER_BY_DAY;
import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT;

/**
 * @author wuxp
 * @date 2023-09-25 08:43
 **/
@AllArgsConstructor
public class SimpleCaptchaGenerateChecker implements CaptchaGenerateChecker {

    private static final Set<Captcha.CaptchaType> IGNORES = ImmutableSet.of(SimpleCaptchaType.PICTURE, SimpleCaptchaType.QR_CODE);

    /**
     * 缓存管理器
     */
    private final CacheManager cacheManager;

    /**
     * 每个用户每天允许发送验证码的最大次数
     */
    private CaptchaProperties properties;

    @Override
    @SuppressWarnings("unchecked")
    public void preCheck(String owner, Captcha.CaptchaType type) {
        if (IGNORES.contains(type)) {
            return;
        }
        String key = String.format("%s_%s", owner, ISO_8601_EXTENDED_DATE_FORMAT.format(new Date()));
        Cache cache = requireCache(type);
        List<Long> times = cache.get(key, List.class);
        if (times == null) {
            times = new ArrayList<>();
        }
        checkFlowControl(times, type);
        AssertUtils.isTrue(times.size() < properties.getMaxAllowGenerateTimesOfUserByDay(type), CAPTCHA_GENERATE_MAX_LIMIT_OF_USER_BY_DAY);
        times.add(System.currentTimeMillis());
        cache.put(key, times);
    }

    private void checkFlowControl(List<Long> times, Captcha.CaptchaType type) {
        Captcha.CaptchaFlowControl control = properties.getFlowControl(type);
        if (control == null) {
            // 不需要流控
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        long millis = control.getWindow().toMillis();
        // 统计在流控窗口内发送的验证码次数
        int count = (int) times.stream().filter(time -> (currentTimeMillis - time) <= millis).count();
        AssertUtils.isTrue(count < control.getSpeed(), CaptchaI18nMessageKeys.CAPTCHA_FLOW_CONTROL);

    }

    @NonNull
    private Cache requireCache(Captcha.CaptchaType captchaTyp) {
        String name = CaptchaConstants.getCaptchaAllowGenTimesCacheName(properties.getGroup(), captchaTyp);
        Cache result = cacheManager.getCache(name);
        AssertUtils.notNull(result, String.format("获取验证码生成次数 Cache 失败，CacheName = %s", name));
        return result;
    }
}
