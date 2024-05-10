package com.wind.script.expression;

/**
 * 指标类型的操作数
 * 可以通过 指标{@link #getName()}名称快速获取指标值
 *
 * @author wuxp
 * @date 2024-05-10 14:19
 **/
public interface MetricsOperand extends Operand {

    /**
     * @return 操作数名称（唯一标识）
     */
    String getName();
}
