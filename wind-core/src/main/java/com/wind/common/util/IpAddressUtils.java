package com.wind.common.util;


import com.wind.common.WindConstants;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.function.Predicate;

/**
 * IP地址工具类，用于获取HTTP请求的来源IP地址
 *
 * @author wxup
 */
@Slf4j
public final class IpAddressUtils {

    private static final String HOST_IP_V4 = getLocalIpv4();
    private static final String HOST_IP_V6 = getLocalIpv6();

    // IPv4地址段数
    private static final int IPV4_SECTION_COUNT = 4;
    // IPv6最小段数（含IPv4内嵌表示法）
    private static final int IPV6_MIN_SECTION_COUNT = 7;
    // IPv6标准段数
    private static final int IPV6_STANDARD_SECTION_COUNT = 8;

    // 双冒号压缩表示
    private static final String DOUBLE_COLON = "::";

    private IpAddressUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 检查是否为有效的IP地址
     *
     * @param ip IP地址
     * @return 如果是有效IP地址返回true
     */
    public static boolean isValidIp(String ip) {
        return isIpV4(ip) || isIpV6(ip);
    }

    /**
     * IPv6地址验证
     * <p>
     * IPv6的地址长度为128位，是IPv4地址长度的4倍。有3种表示方法：
     * 1. 冒分十六进制表示法：X:X:X:X:X:X:X:X
     * 2. 0位压缩表示法：将连续的0压缩为"::"（只能出现一次）
     * 3. 内嵌IPv4地址表示法：X:X:X:X:X:X:d.d.d.d
     *
     * @param ip IP地址
     * @return 如果是IPv6地址返回true
     */
    public static boolean isIpV6(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // 快速检查是否包含冒号
        if (!ip.contains(WindConstants.COLON)) {
            return false;
        }
        // 特殊地址：全0地址
        if (DOUBLE_COLON.equals(ip)) {
            return true;
        }
        // 检查0位压缩表示法
        if (ip.contains(DOUBLE_COLON)) {
            return validateZeroCompressedIpV6(ip);
        }

        // 检查标准IPv6或内嵌IPv4的IPv6
        return validateStandardOrEmbeddedIpV6(ip);
    }

    /**
     * IPv4地址验证
     *
     * @param ip IP地址
     * @return 如果是IPv4地址返回true
     */
    public static boolean isIpV4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String[] sections = ip.split("\\.", -1); // 使用-1保留尾部的空字符串
        if (sections.length != IPV4_SECTION_COUNT) {
            return false;
        }

        for (String section : sections) {
            if (!isValidIpV4Section(section)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取本机IPv4地址
     * 直接根据第一个非回环网卡地址作为内网IPv4地址，避免返回127.0.0.1
     *
     * @return 本机IPv4地址，获取失败返回未知地址
     */
    public static String getLocalIpv4() {
        InetAddress address = findFirstNonLoopbackAddress(Inet4Address.class::isInstance);
        return address != null ? address.getHostAddress() : WindConstants.UNKNOWN;
    }

    /**
     * 获取本机IPv6地址
     *
     * @return 本机IPv6地址，获取失败返回未知地址
     */
    public static String getLocalIpv6() {
        InetAddress address = findFirstNonLoopbackAddress(Inet6Address.class::isInstance);
        return address != null ? address.getHostAddress() : WindConstants.UNKNOWN;
    }

    /**
     * 从缓存中获取本机IPv4地址
     *
     * @return 本机IPv4地址
     */
    public static String getLocalIpv4WithCache() {
        return HOST_IP_V4;
    }

    /**
     * 从缓存中获取本机IPv6地址
     *
     * @return 本机IPv6地址
     */
    public static String getLocalIpv6WithCache() {
        return HOST_IP_V6;
    }

    /**
     * 查找第一个非回环网络地址
     *
     * @param predicate 地址类型判断条件
     * @return 符合条件的网络地址，未找到返回null
     */
    @Nullable
    private static InetAddress findFirstNonLoopbackAddress(Predicate<InetAddress> predicate) {
        try {
            for (Enumeration<NetworkInterface> network = NetworkInterface.getNetworkInterfaces(); network.hasMoreElements(); ) {
                NetworkInterface item = network.nextElement();
                if (item.isLoopback() || !item.isUp()) {
                    continue;
                }
                for (InterfaceAddress address : item.getInterfaceAddresses()) {
                    InetAddress inetAddress = address.getAddress();
                    if (predicate.test(inetAddress) && !inetAddress.isLoopbackAddress()) {
                        return inetAddress;
                    }
                }
            }
            return InetAddress.getLocalHost();
        } catch (SocketException | UnknownHostException exception) {
            log.warn("获取本机 host 失败", exception);
        }
        return null;
    }

    /**
     * 验证IPv4地址段是否为有效的十进制数
     */
    private static boolean isValidIpV4Section(String section) {
        try {
            int value = Integer.parseInt(section);
            return value >= 0 && value <= 255;
        } catch (NumberFormatException ignore) {
            return false;
        }
    }

    /**
     * 验证0位压缩的IPv6地址
     */
    private static boolean validateZeroCompressedIpV6(String ip) {
        // 检查是否包含多个双冒号
        if (ip.indexOf(DOUBLE_COLON) != ip.lastIndexOf(DOUBLE_COLON)) {
            return false;
        }
        // 使用 -1 保留尾部的空字符串
        String[] sections = ip.split(WindConstants.COLON, -1);
        for (String section : sections) {
            if (section.isEmpty() || (section.contains(WindConstants.DOT) && isIpV4(section))) {
                continue;
            }
            if (!isValidHexSection(section)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证标准IPv6或内嵌IPv4的IPv6地址
     */
    private static boolean validateStandardOrEmbeddedIpV6(String ip) {
        String[] sections = ip.split(WindConstants.COLON);
        boolean hasEmbeddedIpV4 = ip.contains(WindConstants.DOT);
        // 检查段数是否合法
        if ((hasEmbeddedIpV4 && sections.length != IPV6_MIN_SECTION_COUNT) || (!hasEmbeddedIpV4 && sections.length != IPV6_STANDARD_SECTION_COUNT)) {
            return false;
        }

        for (String section : sections) {
            if (section.contains(WindConstants.DOT) && isIpV4(section)) {
                continue;
            }
            if (!isValidHexSection(section)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证IPv6地址段是否为有效的十六进制数
     */
    private static boolean isValidHexSection(String section) {
        if (section.isEmpty()) {
            // 允许空段（在压缩表示中）
            return true;
        }
        try {
            // 检查是否为有效的 16 进制数且在 0 ~ FFFF 范围内
            int value = Integer.parseInt(section, 16);
            return value >= 0 && value <= 0xFFFF;
        } catch (NumberFormatException ignore) {
            return false;
        }
    }

}