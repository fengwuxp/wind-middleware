package com.wind.script.spring;

import com.google.common.collect.ImmutableMap;
import com.wind.common.exception.BaseException;
import com.wind.script.expression.Op;
import com.wind.script.expression.Operand;
import com.wind.script.expression.OperandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class SpringExpressionConditionalExpressionJoinerTest {

    private final SpringExpressionConditionalExpressionJoiner joiner = new SpringExpressionConditionalExpressionJoiner();

    @Test
    void testJoin() {
        Assertions.assertEquals("#user.age == 10", joiner.join(createContextVariable("user.age"), createConstant(10), Op.EQ));
        Assertions.assertEquals("#user.age > 10", joiner.join(createContextVariable("user.age"), createConstant(10), Op.GT));
        Assertions.assertEquals("#user.age >= 10", joiner.join(createContextVariable("user.age"), createConstant(10), Op.GE));
        Assertions.assertEquals("#user.age < 10", joiner.join(createContextVariable("user.age"), createConstant(10), Op.LT));
        Assertions.assertEquals("#user.age <= 10", joiner.join(createContextVariable("user.age"), createConstant(10), Op.LE));
        Assertions.assertEquals("T(com.wind.script.spring.SpringExpressionOperators).inRange(#user.age,{1,100})",
                joiner.join(createContextVariable("user.age"), createConstant(new Integer[]{1, 100}), Op.IN_RANG));
        Assertions.assertEquals("!T(com.wind.script.spring.SpringExpressionOperators).inRange(#user.age,{1,100})",
                joiner.join(createContextVariable("user.age"), createConstant(new Integer[]{1, 100}), Op.NOT_IN_RANG));
        Assertions.assertEquals("T(com.wind.script.spring.SpringExpressionOperators).contains(#user.age,{1,100})",
                joiner.join(createContextVariable("user.age"), createConstant(Arrays.asList(1, 100)), Op.CONTAINS));
        Assertions.assertEquals("!T(com.wind.script.spring.SpringExpressionOperators).contains(#user.age,{1,100})",
                joiner.join(createContextVariable("user.age"), createConstant(Arrays.asList(1, 100)), Op.NOT_CONTAINS));
        Assertions.assertEquals("#user == null", joiner.join(createContextVariable("user"), null, Op.IS_NULL));
        Assertions.assertEquals("#user != null", joiner.join(createContextVariable("user"), null, Op.NOT_NULL));
        Assertions.assertEquals("#user.name == getUserName(#user))", joiner.join(createContextVariable("user.name"), createContextVariable(
                "getUserName(#user))", OperandType.EXPRESSION), Op.EQ));
    }

    @Test()
    void testCallSpringBeanError() {
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> joiner.join(createContextVariable("user.name"),
                createContextVariable("@example.getUserName(#user))", OperandType.EXPRESSION), Op.EQ));
        Assertions.assertEquals("不允许使用 @ 开头，访问 spring context bean 对象", exception.getMessage());
    }

    @Test
    void testContains() {
        String expression = joiner.join(Operand.ofConst(1), Operand.of(new Integer[]{1, 2}, OperandType.CONSTANT), Op.CONTAINS);
        Boolean result = SpringExpressionEvaluator.DEFAULT.eval(expression);
        Assertions.assertEquals(Boolean.TRUE, result);

        expression = joiner.join(Operand.ofConst(1), Operand.ofConst("new Integer[]{1, 2}"), Op.CONTAINS);
        Assertions.assertEquals("T(com.wind.script.spring.SpringExpressionOperators).contains(1,'new Integer[]{1, 2}')", expression);
        result = SpringExpressionEvaluator.DEFAULT.eval(expression);
        Assertions.assertEquals(Boolean.TRUE, result);

        expression = joiner.join(Operand.ofConst(1), Operand.of("values", OperandType.VARIABLE), Op.CONTAINS);
        Assertions.assertEquals("T(com.wind.script.spring.SpringExpressionOperators).contains(1,#values)", expression);
        result = SpringExpressionEvaluator.DEFAULT.eval(expression, ImmutableMap.of("values", Arrays.asList(1, 2, 4)));
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testNotContains() {
        String expression = joiner.join(Operand.ofConst(3), Operand.of(new Integer[]{1, 2}, OperandType.CONSTANT), Op.NOT_CONTAINS);
        Boolean result = SpringExpressionEvaluator.DEFAULT.eval(expression);
        Assertions.assertEquals(Boolean.TRUE, result);

        expression = joiner.join(Operand.ofConst(3), Operand.ofConst("new Integer[]{1, 2}"), Op.NOT_CONTAINS);
        Assertions.assertEquals("!T(com.wind.script.spring.SpringExpressionOperators).contains(3,'new Integer[]{1, 2}')", expression);
        result = SpringExpressionEvaluator.DEFAULT.eval(expression);
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testInRange() {
        String expression = joiner.join(Operand.ofConst(1), Operand.of(new Integer[]{1, 2}, OperandType.CONSTANT), Op.IN_RANG);
        Boolean result = SpringExpressionEvaluator.DEFAULT.eval(expression);
        Assertions.assertEquals(Boolean.TRUE, result);

        expression = joiner.join(Operand.ofConst(1), Operand.ofConst("new Integer[]{1, 2}"), Op.IN_RANG);
        Assertions.assertEquals("T(com.wind.script.spring.SpringExpressionOperators).inRange(1,'new Integer[]{1, 2}')", expression);
        result = SpringExpressionEvaluator.DEFAULT.eval(expression);
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testNotInRange() {
        String expression = joiner.join(Operand.ofConst(1), Operand.of(new Integer[]{1, 2}, OperandType.CONSTANT), Op.NOT_CONTAINS);
        Boolean result = SpringExpressionEvaluator.DEFAULT.eval(expression);
        Assertions.assertNotEquals(Boolean.TRUE, result);
    }


    private static Operand createConstant(Object val) {
        return Operand.ofConst(val);
    }

    private static Operand createContextVariable(String text) {
        return createContextVariable(text, OperandType.VARIABLE);
    }

    private static Operand createContextVariable(String text, OperandType type) {
        return new Operand(text, type);
    }


}