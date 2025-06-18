package com.wind.script.expression;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 表达式操作数，{@link #value}用于在表达式中计算
 *
 * @author wuxp
 * @date 2024-05-10 13:38
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Operand {

    /**
     * 操作数 value
     */
    @NotNull
    private Object value;

    /**
     * 操作数类型
     */
    @NotNull
    private OperandType type;


    /**
     * 生成一个常量操作数
     *
     * @param value 常量值
     * @return Operand 实例
     */
    public static Operand ofConst(Object value) {
        return new Operand(value, OperandType.CONSTANT);
    }

    public static Operand of(Object value, OperandType type) {
        return new Operand(value, type);
    }
}
