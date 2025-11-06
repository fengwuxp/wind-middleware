package com.wind.security.captcha;

import com.wind.common.annotations.VisibleForTesting;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.security.captcha.configuration.CaptchaProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.wind.security.captcha.CaptchaI18nMessageKeys.CAPTCHA_GENERATE_MAX_LIMIT_OF_USER_BY_DAY;

/**
 * 默认验证码管理器
 *
 * @author wuxp
 * @date 2023-09-24 10:13
 */
@Slf4j
public record DefaultCaptchaManager(Collection<CaptchaContentGenerator> delegates, Collection<CaptchaSender> senders, CaptchaStorage captchaStorage,
                                    CaptchaProperties properties) implements CaptchaManager {
    /**
     * 忽略流控的验证码类型
     */
    private static final Set<Captcha.CaptchaType> IGNORE_FLOW_CONTROL_TYPES = Set.of(SimpleCaptchaType.PICTURE, SimpleCaptchaType.QR_CODE);

    /**
     * 生成时允许使用之前的值的验证码类型
     */
    @VisibleForTesting
    static final Set<Captcha.CaptchaType> ALLOW_USE_PREVIOUS_CAPTCHA_TYPES = Set.of(SimpleCaptchaType.EMAIL, SimpleCaptchaType.MOBILE_PHONE);

    @Override
    public void send(Captcha captcha) {
        checkFlowControl(captcha.type(), captcha.owner());
        // 发送验证码
        for (CaptchaSender sender : senders) {
            if (sender.supports(captcha.type())) {
                sender.send(captcha);
                // 累计发送次数
                captchaStorage.store(captcha.increaseSendTimes());
                return;
            }
        }
        throw BaseException.common("unsupported send captcha type  = " + captcha.type());
    }

    /**
     * 生成验证码
     *
     * @param type     验证码类型
     * @param useScene 验证码使用场景
     * @param owner    验证码所有者
     * @return 验证码
     */
    @Override
    public Captcha generate(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene, String owner) {
        AssertUtils.notNull(type, "argument type must not empty");
        AssertUtils.notNull(useScene, "argument useScene must not empty");
        AssertUtils.hasText(owner, "argument owner must not empty");
        // 检查是否允许生成验证码
        String realOwner = owner.trim();
        if (ALLOW_USE_PREVIOUS_CAPTCHA_TYPES.contains(type)) {
            // 允许在未失效之前允许重复发送
            Captcha prevCaptcha = captchaStorage.get(type, useScene, realOwner);
            if (prevCaptcha != null && prevCaptcha.isAllowSend()) {
                return prevCaptcha;
            }
        }
        // 尝试清理历史数据
        captchaStorage.remove(type, useScene, realOwner);
        CaptchaContentGenerator delegate = getDelegate(type, useScene);
        CaptchaValue captchaValue = delegate.getValue(realOwner, useScene);
        Captcha result = ImmutableCaptcha.builder()
                .content(captchaValue.content())
                .value(captchaValue.value())
                .owner(realOwner)
                .type(type)
                .useScene(useScene)
                .sendTimes(0)
                .verificationCount(0)
                .maxVerificationTimes(delegate.getMaxAllowVerificationTimes())
                .expireTime(System.currentTimeMillis() + delegate.getEffectiveTime().toMillis())
                .build();
        captchaStorage.store(result);
        return result;
    }

    /**
     * 验证验证码
     *
     * @param expected 预期的值
     * @param type     验证码类型
     * @param useScene 验证码使用场景
     * @param owner    验证码所有者
     */
    @Override
    public void verify(String expected, Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene, String owner) {
        AssertUtils.hasText(expected, "argument expected must not empty");
        AssertUtils.notNull(type, "argument type must not empty");
        AssertUtils.notNull(useScene, "argument useScene must not empty");
        AssertUtils.hasText(owner, "argument owner must not empty");
        String realOwner = owner.trim();
        Captcha captcha = captchaStorage.get(type, useScene, realOwner);
        AssertUtils.notNull(captcha, CaptchaI18nMessageKeys.getCaptchaNotExistOrExpired(type));
        AssertUtils.isTrue(captcha.isAvailable(), () -> {
            // 验证码已失效，移除
            captchaStorage.remove(type, useScene, realOwner);
            return CaptchaI18nMessageKeys.getCaptchaNotExistOrExpired(type);
        });
        boolean isPass = properties.isVerificationIgnoreCase() ? captcha.value().equalsIgnoreCase(expected.trim()) : captcha.value().equals(expected.trim());
        if (isPass) {
            captchaStorage.remove(type, useScene, realOwner);
        } else {
            // 验证失败
            Captcha next = captcha.increaseVerificationCount();
            if (next.isAvailable()) {
                // 还可以继续用于验证，更新验证码
                captchaStorage.store(next);
            } else {
                // 验证码已失效，移除
                captchaStorage.remove(type, useScene, realOwner);
            }
            throw BaseException.common(CaptchaI18nMessageKeys.getCaptchaVerityFailure(type));
        }
    }

    private CaptchaContentGenerator getDelegate(Captcha.CaptchaType type, Captcha.CaptchaUseScene scene) {
        for (CaptchaContentGenerator delegate : delegates) {
            if (delegate.supports(type, scene)) {
                return delegate;
            }
        }
        throw BaseException.notFound(String.format("un found：type = %s, scene = %s CaptchaContentProvider", type.getDesc(), scene.getDesc()));
    }

    private void checkFlowControl(Captcha.CaptchaType type, String owner) {
        Captcha.CaptchaFlowControl control = properties.getFlowControl(type);
        if (control == null || IGNORE_FLOW_CONTROL_TYPES.contains(type)) {
            return;
        }
        List<Long> times = captchaStorage.getCaptchaSendSlideWindows(type, owner);
        long currentTimeMillis = System.currentTimeMillis();
        long millis = control.getWindow().toMillis();
        // 统计在流控窗口内发送的验证码次数
        int count = (int) times.stream().filter(time -> (currentTimeMillis - time) <= millis).count();
        AssertUtils.isTrue(count < control.getSpeed(), CaptchaI18nMessageKeys.CAPTCHA_FLOW_CONTROL);
        AssertUtils.isTrue(times.size() < properties.getMaxAllowGenerateTimesOfUserByDay(type), CAPTCHA_GENERATE_MAX_LIMIT_OF_USER_BY_DAY);
        times.add(currentTimeMillis);
        captchaStorage.putCaptchaSendSlideWindows(type, owner, times);
    }
}
