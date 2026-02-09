package com.wind.signature;

import org.jspecify.annotations.NonNull;

/**
 * 签名器
 *
 * @author wuxp
 * @date 2026-02-09 16:21
 **/
public interface WindTextSigner {

    /**
     * 签名
     *
     * @param signText  用于生成签名的字符串
     * @param secretKey 签名秘钥
     * @return 签名内容
     */
    @NonNull
    String sign(@NonNull String signText, @NonNull String secretKey);

    /**
     * 校验数字签名
     *
     * @param signText  签名字符串
     * @param secretKey 验签的密钥
     * @param sign      数字签名
     * @return 签名验证是否通过
     */
    boolean verify(String signText, String secretKey, String sign);

}
