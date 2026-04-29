package com.wind.common.math;

import com.wind.common.exception.AssertUtils;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * BigDecimal 计算的数学工具
 *
 * @author wuxp
 * @date 2025-12-11 14:26
 **/
public final class WindBigDecimalUtils {

    /**
     * 正无穷，使用 Double#MAX_VALUE 模拟表示
     */
    private static final BigDecimal POSITIVE_INFINITY = BigDecimal.valueOf(Double.MAX_VALUE);

    private WindBigDecimalUtils() {
        throw new AssertionError();
    }

    /**
     * 百分比转数字，将百分比值除以 100 并保留 4 位小数
     *
     * @param percent 百分比
     * @return 转换后的数字（四舍五入）
     */
    @NotNull
    public static BigDecimal percentToNum(@NotNull BigDecimal percent) {
        AssertUtils.notNull(percent, "argument percent must not null");
        // 将百分比值转换为小数值
        return percent.scaleByPowerOfTen(-2).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 数字转百分比，将数值乘以 100 并保留 2 位小数
     *
     * @param num 数值
     * @return 转换后的百分比值（四舍五入）
     */
    @NotNull
    public static BigDecimal numToPercent(@NotNull BigDecimal num) {
        AssertUtils.notNull(num, "argument num must not null");
        return num.scaleByPowerOfTen(2).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 两数相除并将结果转为百分比
     *
     * @param dividend 被除数
     * @param divisor  除数
     * @return 返回百分比值，保留 2 位小数（四舍五入）
     */
    @NotNull
    public static BigDecimal divideToPercent(@NotNull Integer dividend, @NotNull Integer divisor) {
        AssertUtils.notNull(dividend, "argument dividend must not be null");
        AssertUtils.notNull(divisor, "argument divisor must not be null");
        return divideToPercent(BigDecimal.valueOf(dividend), BigDecimal.valueOf(divisor));
    }

    /**
     * 两数相除并将结果转为百分比，
     * 1：如果除数等于 0，被除数大于 0，将返回正无穷
     * 2：被除数、除数都小于 0，将直接返回 0 (避免 2个负数相除得到正数结果)，业务上不允许这种情况出现
     *
     * @param dividend 被除数，需要大于0
     * @param divisor  除数，需要大于等于 0
     * @return 返回百分比值，保留 2 位小数（四舍五入）
     */
    @NotNull
    public static BigDecimal divideToPercent(@NotNull BigDecimal dividend, @NotNull BigDecimal divisor) {
        AssertUtils.notNull(dividend, "argument dividend must not be null");
        AssertUtils.notNull(divisor, "argument divisor must not be null");
        if (Objects.equals(dividend, BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        if (Objects.equals(divisor, BigDecimal.ZERO)) {
            // 除数为 0 ，被除数大于 0，返回正无穷 TODO 待确认
            return dividend.compareTo(BigDecimal.ZERO) > 0 ? POSITIVE_INFINITY : BigDecimal.ZERO;
        }
        if (dividend.compareTo(BigDecimal.ZERO) < 0 && divisor.compareTo(BigDecimal.ZERO) < 0) {
            // 除数和被除数都小于 0 时，直接返回 0，避免负负得正
            return BigDecimal.ZERO;
        }
        // 直接计算百分比，保留更高的精度进行运算，最后再处理 scale
        return numToPercent(dividend.divide(divisor, 4, RoundingMode.HALF_UP));
    }

    /**
     * 判断是否为正无穷
     * 注意：由于正无穷是通过 {@link #POSITIVE_INFINITY} 模拟实现的，所以需要有可能出现误判
     *
     * @param val 数值
     * @return if true 是正无穷
     */
    public static boolean isPositiveInfinity(BigDecimal val) {
        return Objects.equals(val, POSITIVE_INFINITY);
    }

    /**
     * 将金额从元（BigDecimal）转换为分（Integer）的工具方法。
     * 注意：此方法会四舍五入到最接近的分值。
     *
     * @param yuan 金额（元），不可为 null
     * @return 金额（分）
     * @throws NullPointerException 如果输入为 null
     * @throws ArithmeticException  如果转换后的分值超出 Long 范围
     */
    public static long yuanToFen(BigDecimal yuan) {
        // 1. 检查输入是否为 null
        AssertUtils.notNull(yuan, "argument yuan must not null");
        // 2. 元转分：乘以 100，并四舍五入到整数
        BigDecimal fenDecimal = yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
        // 3. 检查是否超出 Integer 范围
        // 精确转换为 int，溢出时抛异常
        return fenDecimal.longValueExact();
    }

    /**
     * 将金额从元（BigDecimal）转换为分（Integer）的工具方法。
     * 注意：此方法会四舍五入到最接近的分值。
     *
     * @param yuan 金额（元），不可为 null
     * @return 金额（分）
     * @throws NullPointerException 如果输入为 null
     * @throws ArithmeticException  如果转换后的分值超出 Integer 范围
     */
    public static int yuanToFenAsInt(BigDecimal yuan) {
        // 1. 检查输入是否为 null
        AssertUtils.notNull(yuan, "argument yuan must not null");
        // 2. 元转分：乘以 100，并四舍五入到整数
        BigDecimal fenDecimal = yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
        // 3. 检查是否超出 Integer 范围
        // 精确转换为 int，溢出时抛异常
        return fenDecimal.intValueExact();
    }

    /**
     * fen 转 yuan
     */
    public static BigDecimal fenToYuan(Long fen) {
        AssertUtils.notNull(fen, "argument fen must not null");
        return BigDecimal.valueOf(fen).scaleByPowerOfTen(-2);
    }

    /**
     * 两数相除
     */
    public static BigDecimal divide(long dividend, long divisor, int scale) {
        return divide(BigDecimal.valueOf(dividend), BigDecimal.valueOf(divisor), scale);
    }

    /**
     * 两数相除
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale) {
        return dividend.divide(divisor, scale, RoundingMode.HALF_UP);
    }
}
