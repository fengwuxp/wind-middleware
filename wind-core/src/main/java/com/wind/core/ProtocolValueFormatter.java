package com.wind.core;

/**
 * 协议值格式化能力。
 *
 * <p>用于将对象转换为外部协议（如第三方 API、银行通道、卡组织）
 * 所要求的字段值格式，常用于请求参数、枚举值或特定编码值输出。</p>
 *
 * <p>例如：</p>
 *
 * <pre>{@code
 * YES -> "Y"
 * NO  -> "N"
 *
 * USD -> "840"
 * HKD -> "344"
 *
 * SUCCESS -> "00"
 * DECLINED -> "05"
 * }</pre>
 *
 * <p>注意：该接口用于协议值映射，不等同于 JSON 或 Java 对象序列化。</p>
 *
 * @author wuxp
 * @date 2026-05-11 10:10
 */
public interface ProtocolValueFormatter {

    /**
     * 返回协议要求的值格式。
     *
     * @return 协议值
     */
    String format();
}