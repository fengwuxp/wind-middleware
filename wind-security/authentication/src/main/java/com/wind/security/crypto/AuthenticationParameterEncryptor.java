package com.wind.security.crypto;

import com.wind.common.annotations.VisibleForTesting;
import com.wind.security.authentication.WindAuthenticationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 非常规用法，主要是为了方便使用
 * 鉴权参数加加解密者
 *
 * @author wuxp
 * @date 2023-11-02 20:24
 **/
@Slf4j
public final class AuthenticationParameterEncryptor implements ApplicationListener<ApplicationStartedEvent> {

    /**
     * 加密配置
     */
    @VisibleForTesting
    static final AtomicReference<RasTextEncryptor> TEXT_ENCRYPTOR = new AtomicReference<>();

    public static String tryEncrypt(String content) {
        RasTextEncryptor encryptor = TEXT_ENCRYPTOR.get();
        if (encryptor == null) {
            return content;
        }
        return StringUtils.hasLength(content) ? encryptor.encrypt(content) : content;
    }

    public static String tryDecrypt(String content) {
        RasTextEncryptor encryptor = TEXT_ENCRYPTOR.get();
        if (encryptor == null) {
            return content;
        }
        return StringUtils.hasLength(content) ? encryptor.decrypt(content) : content;
    }

    /**
     * 认证相关参数（公钥加密）
     *
     * @param principal   登录主体，例如：手机号码、用户名、邮箱等
     * @param credentials 登录证书，例如：密码、验证码等
     * @return 认证参数
     */
    public static AuthenticationParameter encrypt(String principal, String credentials) {
        return new AuthenticationParameter(tryEncrypt(principal), tryEncrypt(credentials));
    }

    /**
     * 解密认证相关参数（私钥加密）
     *
     * @param principal   登录主体，例如：手机号码、用户名、邮箱等
     * @param credentials 登录验证密码
     * @return 认证参数
     */
    public static AuthenticationParameter decrypt(String principal, String credentials) {
        return new AuthenticationParameter(tryDecrypt(principal), tryDecrypt(credentials));
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            WindAuthenticationProperties properties = event.getApplicationContext().getBean(WindAuthenticationProperties.class);
            KeyPair keyPair = properties.getKeyPair();
            TEXT_ENCRYPTOR.set(RasTextEncryptor.ofPublicEncrypt(keyPair.getPublic(), keyPair.getPrivate()));
        } catch (BeansException e) {
            log.info("un enable authentication parameter crypto");
        }
    }


    /**
     * @param principal   登录主体，例如：手机号码、用户名、邮箱等
     * @param credentials 登录证书，例如：密码、验证码等
     */
    public record AuthenticationParameter(String principal, String credentials) {

    }
}
