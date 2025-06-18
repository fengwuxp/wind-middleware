package com.wind.script.spring;

import com.wind.common.WindRange;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.ClassUtils;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * spring expression 表达式操作符扩展
 *
 * @author wuxp
 * @date 2024-09-12 12:59
 **/
public final class SpringExpressionOperators {

    private SpringExpressionOperators() {
        throw new AssertionError();
    }

    /**
     * 包含操作，仅支持字符串、数组、集合类型数据
     *
     * @param left  做操作数
     * @param right 右操作数
     * @return if true 包含
     */
    public static boolean contains(@NotNull Object left, @NotNull Object right) {
        AssertUtils.notNull(left, "argument left must no null");
        AssertUtils.notNull(right, "argument right must no null");
        left = tryEval(left);
        right = tryEval(right);
        if (left instanceof CharSequence && right instanceof CharSequence) {
            // 字符串类型
            return ((String) left).contains((String) right);
        } else if (left instanceof Collection) {
            // 集合
            return ((Collection<?>) left).contains(right);
        } else if (right instanceof Collection) {
            // 集合
            return ((Collection<?>) right).contains(left);
        } else if (left instanceof Object[]) {
            // 对象数组
            return Arrays.asList((Object[]) left).contains(right);
        } else if (right instanceof Object[]) {
            // 对象数组
            return Arrays.asList((Object[]) right).contains(left);
        } else if (ClassUtils.isPrimitiveArray(left.getClass())) {
            // 原子类型数组
            return containsWithPrimitiveArray(left, right);
        } else if (ClassUtils.isPrimitiveArray(right.getClass())) {
            // 原子类型数组
            return containsWithPrimitiveArray(right, left);
        }
        throw new BaseException(DefaultExceptionCode.COMMON_ERROR,
                "不支持的类型: left class = " + left.getClass().getName() + " ,right class " + right.getClass().getName());
    }

    private static boolean containsWithPrimitiveArray(Object array, Object val) {
        Class<?> clazz = array.getClass().getComponentType();
        if (clazz == char.class) {
            return ArrayUtils.contains((char[]) array, (char) val);
        } else if (clazz == byte.class) {
            return ArrayUtils.contains((byte[]) array, (byte) val);
        } else if (clazz == short.class) {
            return ArrayUtils.contains((short[]) array, (short) val);
        } else if (clazz == int.class) {
            return ArrayUtils.contains((int[]) array, (int) val);
        } else if (clazz == long.class) {
            return ArrayUtils.contains((long[]) array, (long) val);
        } else if (clazz == float.class) {
            return ArrayUtils.contains((float[]) array, (float) val);
        } else if (clazz == double.class) {
            return ArrayUtils.contains((double[]) array, (double) val);
        } else if (clazz == boolean.class) {
            return ArrayUtils.contains((boolean[]) array, (boolean) val);
        }
        throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "非预期的数组类型: " + clazz.getName());
    }


    /**
     * 在... 范围之内，区间范围：前闭后闭
     *
     * @param element 判断元素
     * @param range   区间
     * @return if true 在区间内
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean inRange(@NotNull Object element, @NotNull Object range) {
        AssertUtils.notNull(element, "argument element must no null");
        AssertUtils.notNull(range, "argument range must no null");
        range = tryEval(range);
        if (range instanceof List) {
            List<Comparable> rang = (List<Comparable>) range;
            AssertUtils.isTrue(rang.size() == 2, "range Operand size must eq 2");
            return WindRange.between(rang.get(0), rang.get(1)).contains((Comparable) element);
        } else if (range instanceof Object[]) {
            // 对象数组
            return WindRange.between((Comparable[]) range).contains((Comparable) element);
        } else if (ClassUtils.isPrimitiveArray(range.getClass())) {
            // 原子类型数组
            return inRangeWithPrimitiveArray(range, element);
        }
        throw new BaseException(DefaultExceptionCode.COMMON_ERROR,
                "不支持的类型: left class = " + element.getClass().getName() + " ,range class " + range.getClass().getName());
    }

    private static boolean inRangeWithPrimitiveArray(Object array, Object val) {
        Class<?> clazz = array.getClass().getComponentType();
        if (clazz == byte.class) {
            return WindRange.between((byte[]) array).contains((byte) val);
        } else if (clazz == short.class) {
            return WindRange.between((short[]) array).contains((short) val);
        } else if (clazz == int.class) {
            return WindRange.between((int[]) array).contains((int) val);
        } else if (clazz == long.class) {
            return WindRange.between((long[]) array).contains((long) val);
        } else if (clazz == float.class) {
            return WindRange.between((float[]) array).contains((float) val);
        } else if (clazz == double.class) {
            return WindRange.between((double[]) array).contains((double) val);
        }
        throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "非预期的数组类型: " + clazz.getName());
    }

    private static Object tryEval(Object operand) {
        if (operand instanceof String) {
            // 尝试对 new 表达式进行执行
            String expression = (String) operand;
            return expression.startsWith("new ") ? SpringExpressionEvaluator.DEFAULT.eval(expression) : operand;
        }
        return operand;
    }
}
