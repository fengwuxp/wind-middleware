package com.wind.security.captcha;

import com.wind.common.enums.DescriptiveEnum;
import lombok.Data;

import java.beans.Transient;
import java.time.Duration;

/**
 * 验证码，在特定场景保证安全或验证身份
 *
 * @author wuxp
 * @date 2023-09-23 21:15
 **/
public interface Captcha extends CaptchaValue {

    /**
     * 可以是用户的手机号码、邮箱，或者 uuid
     *
     * @return 验证码所有者
     */
    String owner();

    /**
     * @return 验证码类型
     */
    CaptchaType type();

    /**
     * @return 验证码使用场景
     */
    CaptchaUseScene useScene();

    /**
     * @return 已验证次数
     */
    int verificationCount();

    /**
     * @return 已发送次数
     */
    int sendTimes();

    /**
     * @return 允许验证的最大验证次数
     */
    int maxVerificationTimes();

    /**
     * @return 允许发送的最大次数
     */
    default int maxSendTimes() {
        return 3;
    }

    /**
     * @return 验证码失效时间戳
     */
    long expireTime();

    /**
     * 累计一次发送次数
     *
     * @return {@link #sendTimes()} 值更新后的验证码对象
     */
    Captcha increaseSendTimes();

    /**
     * 累计一次验证次数
     *
     * @return {@link #verificationCount()} 值更新后的验证码对象
     */
    Captcha increaseVerificationCount();

    /**
     * @return 是否允许发送
     */
    default boolean isAllowSend() {
        return sendTimes() < maxSendTimes() && isAvailable();
    }

    /**
     * @return 是否有效，如果有效则可以进行验证
     */
    @Transient
    default boolean isAvailable() {
        if (expireTime() <= 0L) {
            // 过期时间没有，表示位过期
            return false;
        }
        //  验证次数 < 最大允许验证次数 & 失效时间 > 当前时间
        return verificationCount() < maxVerificationTimes() && expireTime() > System.currentTimeMillis();
    }

    /**
     * 验证码类型
     *
     * @author wuxp
     * @date 2023-09-24 09:41
     **/
    interface CaptchaType extends DescriptiveEnum {

    }

    /**
     * 验证码使用场景
     *
     * @author wuxp
     * @date 2023-09-24 09:41
     **/
    interface CaptchaUseScene extends DescriptiveEnum {
    }

    /**
     * 验证码配置
     *
     * @author wuxp
     * @date 2023-09-24 12:25
     **/
    interface CaptchaConfiguration {

        /**
         * @return 验证码有效时长
         */
        Duration getEffectiveTime();

        /**
         * @return 最大可以验证次数
         */
        int getMaxAllowVerificationTimes();

    }

    /**
     * 验证码发送流控配置
     */
    @Data
    class CaptchaFlowControl {

        /**
         * 流控统计窗口
         */
        private Duration window = Duration.ofHours(1);

        /**
         * 在流控统计间隔时间内允许发送的次数
         */
        private int speed = 5;
    }

}
