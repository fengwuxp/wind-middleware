package com.wind.common.message;

import java.text.MessageFormat;

/**
 * 消息格式化处理
 *
 * @author wuxp
 * @date 2023-10-30 07:36
 **/
public interface MessageFormatter {

    /**
     * 格式化消息
     *
     * @param pattern 表达式
     * @param args    参数
     * @return 格式化替换后的消息
     */
    String format(String pattern, Object... args);

    static MessageFormatter none() {
        return (pattern, args) -> pattern;
    }

    /**
     * 返回一个 java 默认的消息 formatter
     *
     * @return 消息 formatter
     * @docs https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html
     */
    static MessageFormatter java() {
        return MessageFormat::format;
    }

    static MessageFormatter simple() {
        return new SimpleMessageFormatter();
    }

    /**
     * slf4j 消息格式化处理（沿用 {} 语法）
     *
     * @return MessageFormatter
     */
    static MessageFormatter slf4j() {
        return (pattern, args) -> org.slf4j.helpers.MessageFormatter.format(pattern, args).getMessage();
    }
}
