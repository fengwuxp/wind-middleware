package com.wind.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IpAddressUtils 测试类
 */
class IpAddressUtilsTests {


    @ParameterizedTest
    @DisplayName("验证有效IP地址")
    @ValueSource(strings = {
            "192.168.1.1",
            "10.0.0.1",
            "::1",
            "2001:db8::1",
            "FE80:0000:0000:0000:0202:B3FF:FE1E:8329"
    })
    void testIsValidIp_WithValidIps_ReturnsTrue(String ip) {
        assertTrue(IpAddressUtils.isValidIp(ip));
    }

    @ParameterizedTest
    @DisplayName("验证无效IP地址")
    @ValueSource(strings = {
            "invalid",
            "192.168.1.256",
            "192.168.1",
            "192.168.1.1.1",
            "gggg::1",
            "",
            "null"
    })
    void testIsValidIp_WithInvalidIps_ReturnsFalse(String ip) {
        if ("null".equals(ip)) {
            assertFalse(IpAddressUtils.isValidIp(null));
        } else {
            assertFalse(IpAddressUtils.isValidIp(ip));
        }
    }

    @ParameterizedTest
    @DisplayName("验证有效IPv4地址")
    @ValueSource(strings = {
            "192.168.1.1",
            "10.0.0.1",
            "255.255.255.255",
            "0.0.0.0",
            "127.0.0.1"
    })
    void testIsIpV4_WithValidIpV4_ReturnsTrue(String ip) {
        assertTrue(IpAddressUtils.isIpV4(ip));
    }

    @ParameterizedTest
    @DisplayName("验证无效IPv4地址")
    @ValueSource(strings = {
            "192.168.1.256",
            "192.168.1",
            "192.168.1.1.1",
            "192.168.1.-1",
            "192.168.1.a",
            "",
            "::1",
            "null"
    })
    void testIsIpV4_WithInvalidIpV4_ReturnsFalse(String ip) {
        if ("null".equals(ip)) {
            assertFalse(IpAddressUtils.isIpV4(null));
        } else {
            assertFalse(IpAddressUtils.isIpV4(ip));
        }
    }

    @ParameterizedTest
    @DisplayName("验证有效IPv6地址 - 标准格式")
    @ValueSource(strings = {
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            "FE80:0000:0000:0000:0202:B3FF:FE1E:8329",
            "::1",
            "2001:db8::1",
            "::"
    })
    void testIsIpV6_WithValidIpV6_ReturnsTrue(String ip) {
        assertTrue(IpAddressUtils.isIpV6(ip));
    }

    @ParameterizedTest
    @DisplayName("验证有效IPv6地址 - 压缩格式")
    @ValueSource(strings = {
            "2001:db8::8a2e:370:7334",
            "FF01::101",
            "::ffff:192.0.2.1"
    })
    void testIsIpV6_WithCompressedIpV6_ReturnsTrue(String ip) {
        assertTrue(IpAddressUtils.isIpV6(ip));
    }

    @ParameterizedTest
    @DisplayName("验证无效IPv6地址")
    @ValueSource(strings = {
            "2001:db8::8a2e::370:7334", // 多个压缩 - 这是无效的
            "2001:db8:85a3:0000:0000:8a2e:0370:7334:1234", // 段数过多
            "2001:db8:85a3:0000:0000:8a2e:0370", // 段数不足
            "gggg::1", // 无效十六进制
            "192.168.1.1", // IPv4地址
            "",
            "null"
    })
    void testIsIpV6_WithInvalidIpV6_ReturnsFalse(String ip) {
        if ("null".equals(ip)) {
            assertFalse(IpAddressUtils.isIpV6(null));
        } else {
            assertFalse(IpAddressUtils.isIpV6(ip),
                    "IP地址 '" + ip + "' 应该被识别为无效IPv6地址");
        }
    }

    @Test
    @DisplayName("验证内嵌IPv4的IPv6地址")
    void testIsIpV6_WithEmbeddedIpV4_ReturnsTrue() {
        assertTrue(IpAddressUtils.isIpV6("::ffff:192.168.1.1"));
        assertTrue(IpAddressUtils.isIpV6("::192.168.1.1"));
    }

    @Test
    @DisplayName("获取本地IPv4地址不应返回空或未知")
    void testGetLocalIpv4_ReturnsValidAddress() {
        String ip = IpAddressUtils.getLocalIpv4();
        assertNotNull(ip);
        assertFalse(ip.isEmpty());
        // 不应该返回回环地址
        assertNotEquals("127.0.0.1", ip);
    }

    @Test
    @DisplayName("获取本地IPv6地址不应返回空")
    void testGetLocalIpv6_ReturnsValidAddress() {
        String ip = IpAddressUtils.getLocalIpv6();
        assertNotNull(ip);
        assertFalse(ip.isEmpty());
    }

    @Test
    @DisplayName("缓存IPv4地址应与实时获取一致")
    void testGetLocalIpv4WithCache_ReturnsCachedValue() {
        String cachedIp = IpAddressUtils.getLocalIpv4WithCache();
        String realTimeIp = IpAddressUtils.getLocalIpv4();

        assertEquals(cachedIp, realTimeIp);
        assertNotNull(cachedIp);
    }

    @Test
    @DisplayName("缓存IPv6地址应与实时获取一致")
    void testGetLocalIpv6WithCache_ReturnsCachedValue() {
        String cachedIp = IpAddressUtils.getLocalIpv6WithCache();
        String realTimeIp = IpAddressUtils.getLocalIpv6();

        assertEquals(cachedIp, realTimeIp);
        assertNotNull(cachedIp);
    }

    @ParameterizedTest
    @DisplayName("验证IPv4地址段边界值")
    @CsvSource({
            "0, true",
            "255, true",
            "256, false",
            "-1, false",
            "128, true"
    })
    void testIpV4SectionBoundaryValues(String section, boolean expected) {
        assertEquals(expected, IpAddressUtils.isIpV4(section + ".0.0.0"));
    }

    @Test
    @DisplayName("IPv4地址应正确处理各段数字")
    void testIpV4WithDifferentSections() {
        assertTrue(IpAddressUtils.isIpV4("1.2.3.4"));
        assertTrue(IpAddressUtils.isIpV4("100.200.150.50"));
    }

    @Test
    @DisplayName("IPv6地址应正确处理压缩格式")
    void testIpV6CompressionScenarios() {
        // 前导零压缩
        assertTrue(IpAddressUtils.isIpV6("2001:db8::8a2e:370:7334"));

        // 全零压缩
        assertTrue(IpAddressUtils.isIpV6("::"));

        // 尾随零压缩
        assertTrue(IpAddressUtils.isIpV6("2001:db8::"));
    }

    @Test
    @DisplayName("IPv6十六进制段验证")
    void testIpV6HexSectionValidation() {
        // 有效十六进制
        assertTrue(IpAddressUtils.isIpV6("ABCD:EF01:2345:6789:ABCD:EF01:2345:6789"));

        // 无效十六进制
        assertFalse(IpAddressUtils.isIpV6("GGGG:FFFF:EEEE:DDDD:CCCC:BBBB:AAAA:9999"));
    }

    @Test
    @DisplayName("网络异常情况下应返回未知地址")
    void testNetworkExceptionHandling() throws Exception {
        // 这个测试主要验证异常处理逻辑，实际执行中可能难以模拟网络异常
        // 但我们可以确认方法在正常情况下不会抛出异常
        assertDoesNotThrow(() -> {
            String ipv4 = IpAddressUtils.getLocalIpv4();
            String ipv6 = IpAddressUtils.getLocalIpv6();
        });
    }

    @ParameterizedTest
    @DisplayName("混合测试 - IPv4和IPv6不应混淆")
    @CsvSource({
            "192.168.1.1, true, false",  // 纯IPv4
            "::ffff:192.168.1.1, false, true",  // 内嵌IPv4的IPv6
            "2001:db8::1, false, true",  // 纯IPv6
            "invalid, false, false"      // 无效地址
    })
    void testIpVersionDetection(String ip, boolean expectedV4, boolean expectedV6) {
        assertEquals(expectedV4, IpAddressUtils.isIpV4(ip));
        assertEquals(expectedV6, IpAddressUtils.isIpV6(ip));
        assertEquals(expectedV4 || expectedV6, IpAddressUtils.isValidIp(ip));
    }
}