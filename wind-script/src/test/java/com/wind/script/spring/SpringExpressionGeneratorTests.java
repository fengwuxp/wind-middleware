package com.wind.script.spring;

import com.google.common.collect.ImmutableMap;
import com.wind.common.exception.BaseException;
import com.wind.jackson.WindJson;
import com.wind.script.expression.ExpressionDescriptor;
import com.wind.script.expression.Op;
import com.wind.script.expression.Operand;
import com.wind.script.expression.OperandType;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ResourceUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author wuxp
 * @date 2023-09-23 07:58
 **/
class SpringExpressionGeneratorTests {

    private static final String EXPECTED_EXPRESSION = "#name == '张三' AND (T(com.wind.script.spring.SpringExpressionOperators).contains({'dev'," +
            "'sit'},#env) AND (T(com.wind.script.spring.SpringExpressionOperators).inRange(#age,{16,45}) OR (T(com.wind.script.spring" +
            ".SpringExpressionOperators).contains({'杭州','上海'},#city) OR #tags['example'] != 'demo')))";

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Test
    void generate() throws Exception {
        URI filepath = ResourceUtils.getURL("classpath:conditional-nodes.json").toURI();
        String json = IOUtils.toString(Files.newInputStream(Paths.get(filepath)), StandardCharsets.UTF_8);
        ExpressionDescriptor descriptor = WindJson.parseObject(json, ExpressionDescriptor.class);
        String spel = SpringExpressionGenerator.generate(descriptor);
        Assertions.assertEquals(EXPECTED_EXPRESSION, spel);
        Expression expression = expressionParser.parseExpression(spel);
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("tags", ImmutableMap.of("example", "demo"));
        context.setVariable("city", "北京");
        context.setVariable("age", 26);
        context.setVariable("env", "dev");
        context.setVariable("name", "张三");
        Assertions.assertEquals(Boolean.TRUE, expression.getValue(context, Boolean.class));
    }

    @Test
    void generateOpByExpression() {
        ExpressionDescriptor node = mockGlobalMethodNode(".success");
        String spel = SpringExpressionGenerator.generate(node);
        Assertions.assertEquals("#root.execCmd('test-cmd',#name,'张三',{'a':'b'}).success", spel);
    }

    @Test
    void generateOpByExpressionNoArgs() {
        ExpressionDescriptor node = new ExpressionDescriptor();
        node.setOp(Op.GLOBAL_METHOD);
        node.setLeft(new Operand("root.getExample", OperandType.EXPRESSION));
        node.setRight(new Operand("{}", OperandType.EXPRESSION));
        String spel = SpringExpressionGenerator.generate(node);
        Assertions.assertEquals("#root.getExample()", spel);
    }

    @Test()
    void generateOpByExpressionWithDisabled1() {
        ExpressionDescriptor node = mockGlobalMethodNode("success == 1 and new xxx.File('/xxx').delete()");
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> SpringExpressionGenerator.generate(node));
        Assertions.assertEquals("不允许使用：new 操作符", exception.getMessage());
    }


    @Test()
    void generateOpByExpressionWithDisabled2() {
        ExpressionDescriptor node = mockGlobalMethodNode("success == 1 and T(java.lang.System.exit(0))");
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> SpringExpressionGenerator.generate(node));
        Assertions.assertEquals("不允许使用：T( 操作符", exception.getMessage());
    }

    @Test
    void testSpringExpression() {
        Expression expression = expressionParser.parseExpression("{'java','Struts','Spring'}.contains('java')");
        Assertions.assertEquals(Boolean.TRUE, expression.getValue(Boolean.class));
        expression = expressionParser.parseExpression("{1,2,3}.contains(2)");
        Assertions.assertEquals(Boolean.TRUE, expression.getValue(Boolean.class));
        expression = expressionParser.parseExpression("{'a':'大学'}.containsKey('a')");
        Assertions.assertEquals(Boolean.TRUE, expression.getValue(Boolean.class));
    }

    @Test
    void testSpringExpressionWithNull() {
        Expression expression = expressionParser.parseExpression("{1,2,3}.contains(#test)");
        Assertions.assertEquals(Boolean.FALSE, expression.getValue(new StandardEvaluationContext(), Boolean.class));
    }

    private static ExpressionDescriptor mockGlobalMethodNode(String expression) {
        ExpressionDescriptor result = new ExpressionDescriptor();
        result.setOp(Op.GLOBAL_METHOD);
        result.setLeft(new Operand("root.execCmd", OperandType.EXPRESSION));
        List<String> args = new ArrayList<>();
        args.add("#name");
        args.add("'张三'");
        args.add("{'a':'b'}");
        Map<String, String> config = ImmutableMap.of(
                "cmd", "test-cmd",
                "args", WindJson.toJsonString(args),
                "result", expression
        );
        result.setRight(new Operand(WindJson.toJsonString(config), OperandType.EXPRESSION));
        String jsonString = WindJson.toJsonString(result);
        Assertions.assertNotNull(jsonString);
        return result;
    }
}
