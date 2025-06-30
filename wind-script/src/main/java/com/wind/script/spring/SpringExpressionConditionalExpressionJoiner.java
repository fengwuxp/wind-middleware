package com.wind.script.spring;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.google.common.collect.ImmutableSet;
import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.script.ConditionalExpressionJoiner;
import com.wind.script.expression.ExpressionDescriptor;
import com.wind.script.expression.Op;
import com.wind.script.expression.Operand;
import com.wind.script.expression.OperandType;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 spring expression 语法实现的条件表达式连接器
 * 由于表达式是根据 {@link ExpressionDescriptor} 生成的，只能有限性支持 spring expression 的语法
 * 以下语法不会被生成
 * 1：new 关键字，例如：new xxx.File('/xxx').delete()
 * 2：调用静态类方法，例如：T(java.lang.System.exit(0))
 *
 * @author wuxp
 * @version SpringExpressionConditionalJoiner.java, v 0.1 2022年08月16日 18:08 wuxp
 * @docs https://docs.spring.io/spring-framework/reference/core/expressions.html
 * @see ConditionalExpressionJoiner
 */
public class SpringExpressionConditionalExpressionJoiner implements ConditionalExpressionJoiner<Operand> {

    private static final List<Op> NONE_RIGHT_OPS = Arrays.asList(Op.IS_NULL, Op.NOT_NULL);

    private static final Map<Op, ConditionalExpressionJoiner<String>> JOINERS = new EnumMap<>(Op.class);

    private static final String SPRING_CONTAINS_OP_EXPRESSION = "T(com.wind.script.spring.SpringExpressionOperators).contains(%s,%s)";

    private static final String SPRING_IN_RANGE_OP_EXPRESSION = "T(com.wind.script.spring.SpringExpressionOperators).inRange(%s,%s)";

    /**
     * 不允许使用的操作符号
     */
    private static final Set<String> NOT_ALLOW_OPS = ImmutableSet.of("new", "T(");

    static {
        JOINERS.put(Op.IS_NULL, (left, right, op) -> String.format("%s %s", left, Op.IS_NULL.getOperator()));
        JOINERS.put(Op.NOT_NULL, (left, right, op) -> String.format("%s %s", left, Op.NOT_NULL.getOperator()));

        JOINERS.put(Op.CONTAINS, (left, right, op) -> String.format(SPRING_CONTAINS_OP_EXPRESSION, left, right));
        JOINERS.put(Op.NOT_CONTAINS, (left, right, op) -> "!" + JOINERS.get(Op.CONTAINS).join(left, right, Op.CONTAINS));

        JOINERS.put(Op.IN_RANG, (left, right, op) -> String.format(SPRING_IN_RANGE_OP_EXPRESSION, left, right));
        JOINERS.put(Op.NOT_IN_RANG, (left, right, op) -> "!" + JOINERS.get(Op.IN_RANG).join(left, right, Op.CONTAINS));
        JOINERS.put(Op.GLOBAL_METHOD, (left, right, op) -> {
            Map<String, String> configs = JSON.parseObject(right, new TypeReference<Map<String, String>>() {
            });
            String result = configs.get("result");
            if (StringUtils.hasText(result)) {
                checkExpression(result);
                return String.format("#%s(%s)%s", left, String.join(WindConstants.COMMA, parseGlobalMethodArgs(configs)), result);
            }
            return String.format("#%s(%s)", left, String.join(WindConstants.COMMA, parseGlobalMethodArgs(configs)));
        });
    }

    private static List<String> parseGlobalMethodArgs(Map<String, String> configs) {
        List<String> result = new ArrayList<>();
        if (StringUtils.hasText(configs.get("cmd"))) {
            result.add(String.format("'%s'", configs.get("cmd")));
        }
        String argsJson = configs.get("args");
        if (!StringUtils.hasText(argsJson)) {
            return result;
        }
        try {
            result.addAll(JSON.parseArray(argsJson, String.class));
        } catch (Exception e) {
            // 忽略 json 解析异常
            return result;
        }
        return result;
    }

    @Override
    public String join(Operand left, Operand right, Op op) {
        AssertUtils.notNull(op, "操作运算符不能为 null");
        AssertUtils.notNull(left, "左操作数不能为 null");
        if (!NONE_RIGHT_OPS.contains(op)) {
            AssertUtils.notNull(right, "右操作数不能为 null");
        }
        String leftCode = convertToExpression(left);
        String rightCode = convertToExpression(right);
        if (leftCode == null && rightCode == null) {
            return WindConstants.EMPTY;
        }
        ConditionalExpressionJoiner<String> joiner = JOINERS.getOrDefault(op, DEFAULT_JOINER);
        return joiner.join(leftCode, rightCode, op);
    }

    private String convertToExpression(Operand operand) {
        if (operand == null) {
            return null;
        }
        Object value = operand.getValue();
        AssertUtils.notNull(value, "操作数的值不能为 null");
        if (Objects.equals(OperandType.VARIABLE, operand.getType())) {
            // context 上下文变量
            return String.format("%s%s", WindConstants.SHARP, value);
        }
        if (value instanceof Collection) {
            return toSpringExpressionArray((Collection<?>) value, operand.getType());
        }
        if (value.getClass().isArray()) {
            return toSpringExpressionArray(Arrays.asList((Object[]) value), operand.getType());
        }
        return toExpression(value, operand.getType());
    }

    private String toSpringExpressionArray(Collection<?> collection, OperandType type) {
        String val = collection.stream()
                .map(item -> toExpression(item, type))
                .collect(Collectors.joining(WindConstants.COMMA));
        return String.format("{%s}", val);
    }

    private String toExpression(Object o, OperandType type) {
        // TODO 其他类型判断
        if (o instanceof String text) {
            if (Objects.equals(type, OperandType.EXPRESSION)) {
                AssertUtils.isTrue(!text.startsWith(WindConstants.AT), "不允许使用 @ 开头，访问 spring context bean 对象");
                return text;
            } else {
                return String.format("'%s'", o);
            }
        }
        return String.format("%s", o.toString());
    }

    /**
     * 将 spel 表达式集合转换为数组
     *
     * @param expression spring expression 集合格式，例如： {1,100}
     * @return 数组
     */
    private static String[] parseExpressionToArray(String expression) {
        return expression.substring(1, expression.length() - 1).split(WindConstants.COMMA);
    }

    private static void checkExpression(String expression) {
        for (String op : NOT_ALLOW_OPS) {
            AssertUtils.isTrue(!expression.contains(op), String.format("不允许使用：%s 操作符", op));
        }
    }
}
