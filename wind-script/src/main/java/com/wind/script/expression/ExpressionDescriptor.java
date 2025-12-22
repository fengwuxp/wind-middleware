package com.wind.script.expression;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 条件表达式
 *
 * @author wuxp
 * @date 2023-09-23 07:44
 **/
@Data
public class ExpressionDescriptor implements Serializable {

    @Serial
    private static final long serialVersionUID = 9181900345416728145L;

    /**
     * 逻辑运算关系 AND、OR
     */
    private LogicalOp conjunctions;

    /**
     * 左操作数
     */
    private Operand left;

    /**
     * 操作符
     */
    private Op op;

    /**
     * 右操作数
     */
    private Operand right;

    /**
     * 子表达式
     */
    private List<ExpressionDescriptor> children;
}
