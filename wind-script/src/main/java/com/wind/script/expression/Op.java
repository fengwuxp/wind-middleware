package com.wind.script.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuxp
 * @date 2024-09-10 14:30
 **/
@AllArgsConstructor
@Getter
public enum Op {

    /**
     * 等于
     */
    EQ("==", "等于"),

    /**
     * 不等于
     */
    NOT_EQ("!=", "不等于"),

    /**
     * 大于
     */
    GT(">", "大于"),

    /**
     * 小于
     */
    LT("<", "小于"),

    /**
     * 大于等于
     */
    GE(">=", "大于等于"),

    /**
     * 小于等于
     */
    LE("<=", "小于等于"),

    /**
     * xxx 为Null
     */
    IS_NULL("== null", "为Null"),

    /**
     * xxx 不为Null
     */
    NOT_NULL("!= null", "不为Null"),

    /**
     * 字符串、 集合、数组、Map 等类型包含操作
     */
    CONTAINS("contains", "包含"),

    NOT_CONTAINS("not contains", "包含"),

    /**
     * 数字范围操作，日期类型可以转换为时间戳处理
     */
    IN_RANG("in rang", "在范围...之内"),

    NOT_IN_RANG("not in rang", "不在范围...之内"),

    /**
     * 全局（root）方法
     */
    GLOBAL_METHOD("global", "全局方法");

    private final String operator;

    private final String desc;
}
