package com.wind.signature;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签名算法
 *
 * @author wuxp
 * @date 2026-02-09 16:49
 **/
@Getter
@AllArgsConstructor
public enum SignatureAlgorithm {

    HMAC_SHA256("HmacSHA256"),

    HMAC_SHA512("HmacSHA512"),

    SHA256_WITH_RSA("SHA256WithRSA");

    private final String algorithm;
}
