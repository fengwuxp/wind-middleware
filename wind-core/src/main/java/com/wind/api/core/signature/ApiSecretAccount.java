package com.wind.api.core.signature;

import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Api 访问的服务账号
 *
 * @author wuxp
 * @date 2024-02-02 16:11
 **/
public interface ApiSecretAccount {

    /**
     * AccessKey or AppId
     *
     * @return 账号访问唯一标识
     */
    @NotBlank
    String getAccessId();

    /**
     * @return 获取签名密钥
     */
    @NotBlank
    String getSecretKey();

    /**
     * 签名秘钥版本，秘钥轮转时可以使用该字段确认使用哪个秘钥
     *
     * @return 签名秘钥版本
     */
    @Nullable
    default String getSecretKeyVersion() {
        return null;
    }

    /**
     * @return 签名算法实现
     */
    @NotNull
    ApiSignAlgorithm getSigner();

    /**
     * 使用 HmacSHA256算法签名
     *
     * @param accessId  AccessKey or AppId
     * @param secretKey 签名秘钥
     * @return 不可变的 ApiSecretAccount 实例
     */
    static ApiSecretAccount hmacSha256(String accessId, String secretKey) {
        return immutable(accessId, secretKey, null, ApiSignAlgorithm.HMAC_SHA256);
    }

    static ApiSecretAccount sha256WithRsa(String accessId, String secretKey) {
        return sha256WithRsa(accessId, secretKey, null);
    }

    /**
     * 使用 SHA256_WITH_RSA 算法签名
     *
     * @param accessId         AccessKey or AppId
     * @param secretKey        签名秘钥
     * @param secretKeyVersion 签名秘钥版本
     * @return 不可变的 ApiSecretAccount 实例
     */
    static ApiSecretAccount sha256WithRsa(String accessId, String secretKey, @Nullable String secretKeyVersion) {
        return immutable(accessId, secretKey, secretKeyVersion, ApiSignAlgorithm.SHA256_WITH_RSA);
    }

    /**
     * 创建一个 不可变的 ApiSecretAccount
     *
     * @param accessId         AccessKey or AppId
     * @param secretKey        签名秘钥
     * @param secretKeyVersion 签名秘钥版本
     * @param signer           签名算法实现
     * @return 不可变的 ApiSecretAccount 实例
     */
    static ApiSecretAccount immutable(String accessId, String secretKey, @Nullable String secretKeyVersion, ApiSignAlgorithm signer) {
        Objects.requireNonNull(accessId, "argument accessId must not null");
        Objects.requireNonNull(secretKey, "argument secretKey must not null");
        Objects.requireNonNull(signer, "argument signer must not null");
        return new ApiSecretAccount() {
            @Override
            public String getAccessId() {
                return accessId;
            }

            @Override
            public String getSecretKey() {
                return secretKey;
            }

            @Override
            public String getSecretKeyVersion() {
                return secretKeyVersion;
            }

            @Override
            public ApiSignAlgorithm getSigner() {
                return signer;
            }
        };
    }
}
