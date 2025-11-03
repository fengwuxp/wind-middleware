package com.wind.security.captcha;

import java.beans.Transient;
import java.io.Serializable;

/**
 * 验证码值
 *
 * @author wuxp
 * @date 2023-09-24 12:52
 **/
public interface CaptchaValue extends Serializable {

    /**
     * @return 验证码值，用于验证
     */
    String value();

    /**
     * 不进行持久化，减少存储压力
     *
     * @return 验证码内容，用于展示或发送给用户
     */
    @Transient
    String content();

    static CaptchaValue of(String value, String content) {
        return new ImmtableCaptchaValue(value, content);
    }


    /**
     * @param value   验证码值
     * @param content 验证码内容
     */
    record ImmtableCaptchaValue(String value, String content) implements CaptchaValue {

        /**
         * 不进行持久化
         */
        @Override
        @Transient
        public String content() {
            return content;
        }
    }
}
