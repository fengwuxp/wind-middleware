package com.wind.security.captcha;

import lombok.Builder;

import java.beans.Transient;

/**
 * @param owner                  验证码所有者
 * @param type                   验证码类型
 * @param useScene               验证码使用场景
 * @param value                  验证码值
 * @param content                验证码内容
 * @param verificationCount       已验证次数
 * @param sendTimes               生成次数，从 1 开始
 * @param maxVerificationTimes  允许验证次数
 * @param expireTime             过期时间
 * @author wuxp
 * @date 2023-09-24 09:48
 */
@Builder
public record ImmutableCaptcha(String owner, CaptchaType type, CaptchaUseScene useScene, String value, String content,
                               int verificationCount, int sendTimes, int maxVerificationTimes, long expireTime) implements Captcha {

    /**
     * 为了给序列化框架使用，提供一个空构造
     */
    ImmutableCaptcha() {
        this(null, null, null, null, null, 0, 1, 0, 0);
    }

    /**
     * 统计已发送次数
     *
     * @return 新的验证码
     */
    public ImmutableCaptcha increaseSendTimes() {
        // 重新发送，过期时间延长 5 分钟
        return new ImmutableCaptcha(owner, type, useScene, value, content, verificationCount, sendTimes + 1, maxVerificationTimes, expireTime);
    }

    /**
     * 统计已验证次数
     *
     * @return 新的验证码
     */
    public ImmutableCaptcha increaseVerificationCount() {
        return new ImmutableCaptcha(owner, type, useScene, value, content, verificationCount + 1, sendTimes, maxVerificationTimes, expireTime);
    }

    /**
     * 不进行持久化
     */
    @Override
    @Transient
    public String content() {
        return content;
    }
}
