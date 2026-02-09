package com.wind.signature;

/**
 * 签名接口
 *
 * @author wuxp
 * @date 2026-02-09 16:20
 **/
public interface WindByteSigner {

    /**
     * 签名
     *
     * @param signBytes   生成签名的内容
     * @param secretBytes 签名秘钥
     * @return 签名内容
     */
    byte[] sign(byte[] signBytes, byte[] secretBytes);

    /**
     * 验证签名
     *
     * @param signBytes   验证的签名内容
     * @param secretBytes 签名秘钥
     * @param sign        待验证的签名
     * @return 验证结果
     */
    boolean verify(byte[] signBytes, byte[] secretBytes, byte[] sign);
}
