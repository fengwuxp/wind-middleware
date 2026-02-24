package com.wind.web.util;


import com.wind.common.WindConstants;
import com.wind.common.enums.WindClientDeviceType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import ua_parser.Client;
import ua_parser.Parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 客户端设备类型解析器
 *
 * @author wuxp
 * @date 2025-12-11 09:41
 **/
@Slf4j
public final class ClientDeviceTypeParserUtils {

    private static final Parser UA_PARSER = new Parser();

    /**
     * 用户认证方式属性名称
     */
    private static final String USER_AUTHENTICATION_METHOD_ATTRIBUTE_NAME = "USER_AUTHENTICATION_METHOD";


    private static final String CLIENT_HINTS_PLATFORM_HEADER_NAME = "Sec-CH-UA-Platform";
    private static final String CLIENT_HINTS_MOBILE_HEADER_NAME = "Sec-CH-UA-Mobile";
    private static final String CLIENT_HINTS_MODEL_HEADER_NAME = "Sec-CH-UA-Model";
    private static final String CLIENT_HINTS_PLATFORM_VERSION_HEADER_NAME = "Sec-CH-UA-Platform-Version";

    private static final String ANDROID = "android";
    private static final String IOS = "ios";
    private static final String IPAD = "ipad";
    private static final String IPHONE = "iphone";
    private static final String IPOD = "ipod";

    private static final String WINDOWS = "windows";
    private static final String MAC = "mac";
    private static final String MACOS = "macos";
    private static final String LINUX = "linux";

    private static final String TABLET = "tablet";
    private static final String MOBILE = "mobile";

    private static final String GALAXY_TAB = "galaxy tab";
    private static final String MEDIA_PAD = "mediapad";
    private static final String KINDLE_FIRE = "kindle fire";
    private static final String NEXUS_7 = "nexus 7";
    private static final String NEXUS_9 = "nexus 9";

    private ClientDeviceTypeParserUtils() {
        throw new AssertionError();
    }

    /**
     * 解析客户端设备类型
     *
     * @param request 请求
     * @return 客户端设备类型
     */
    @NonNull
    public static WindClientDeviceType resolveDeviceType(@NonNull HttpServletRequest request) {
        // 1. 优先解析 Client Hints
        WindClientDeviceType type = parseFromClientHints(request);
        if (type != null) {
            return type;
        }
        // 2. 使用 User-Agent + uap-java
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        WindClientDeviceType result = parseFromUserAgent(ua);
        return result == null ? WindClientDeviceType.UNKNOWN : result;
    }

    /**
     * 解析用户访问客户端信息
     *
     * @param request 请求
     * @return 用户访问客户端信息
     */
    @NonNull
    public static UserClientInfo resolveUserClientInfo(@NonNull HttpServletRequest request) {
        WindClientDeviceType deviceType = resolveDeviceType(request);

        UserClientInfo result = new UserClientInfo();
        result.setDeviceType(deviceType);

        // ----------- 获取 OS 与版本信息 -----------
        String os = header(request, CLIENT_HINTS_PLATFORM_HEADER_NAME);
        String model = header(request, CLIENT_HINTS_MODEL_HEADER_NAME);      // 设备型号
        String osVersion = header(request, CLIENT_HINTS_PLATFORM_VERSION_HEADER_NAME); // 平台版本

        if (!StringUtils.hasText(os)) {
            // Client Hints 不存在，回退到 UA
            String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
            if (StringUtils.hasText(userAgent)) {
                os = parseOSFromUA(userAgent);
                osVersion = parseOSVersionFromUA(userAgent);
                model = parseDeviceModelFromUA(userAgent);
            }
        }

        result.setOs(StringUtils.hasText(os) ? os : deviceType.getDesc());
        result.setOsVersion(StringUtils.hasText(osVersion) ? osVersion : null);
        result.setClientAppVersion(StringUtils.hasText(model) ? model : null);

        // ----------- 获取登录方式（请求属性或默认） -----------
        Object authenticationMethod = request.getAttribute(USER_AUTHENTICATION_METHOD_ATTRIBUTE_NAME);
        result.setAuthenticationMethod(String.valueOf(authenticationMethod));
        return result;
    }


    /**
     * 从 UA 获取操作系统名称
     *
     * @param ua UA
     * @return 操作系统名称
     */
    private static String parseOSFromUA(String ua) {
        ua = ua.toLowerCase();
        if (ua.contains(ANDROID)) {
            return WindClientDeviceType.ANDROID_PHONE.getOs();
        }
        if (ua.contains(IPHONE) || ua.contains(IPAD) || ua.contains(IPOD)) {
            return WindClientDeviceType.IPHONE.getOs();
        }
        if (ua.contains(WINDOWS)) {
            return WindClientDeviceType.WINDOWS.getOs();
        }
        if (ua.contains(MAC)) {
            return WindClientDeviceType.MAC.getOs();
        }
        if (ua.contains(LINUX)) {
            return WindClientDeviceType.LINUX.getOs();
        }
        return null;
    }

    /**
     * 从 UA 获取操作系统版本
     *
     * @param ua UA
     * @return 操作系统版本
     */
    private static String parseOSVersionFromUA(String ua) {
        // 示例 Android/10, iPhone OS 17_2 等
        Pattern pattern = Pattern.compile("(android)\\s([\\d\\\\.]+)|(iphone os|cpu os)\\s([\\d_]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ua);
        if (matcher.find()) {
            String version = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
            return version != null ? version.replace('_', '.') : null;
        }
        return null;
    }

    // 从 UA 获取设备型号
    private static String parseDeviceModelFromUA(String ua) {
        // 例如 "iPhone14,2" 或 "SM-T870" 等
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(ua);
        if (matcher.find()) {
            String inside = matcher.group(1);
            // 简单提取品牌型号信息
            String[] parts = inside.split(";");
            if (parts.length > 0) {
                return parts[parts.length - 1].trim();
            }
        }
        return null;
    }


    // ==========================
    // 1. Client Hints 解析逻辑
    // ==========================

    private static WindClientDeviceType parseFromClientHints(HttpServletRequest req) {
        String platform = header(req, CLIENT_HINTS_PLATFORM_HEADER_NAME);
        String mobileFlag = header(req, CLIENT_HINTS_MOBILE_HEADER_NAME);   // "?1" 或 "?0"
        String model = header(req, CLIENT_HINTS_MODEL_HEADER_NAME);

        if (platform == null) {
            return null; // 没有 CH, 回退 UA
        }

        platform = platform.replace("\"", "").trim();
        boolean isMobile = "?1".equalsIgnoreCase(mobileFlag);
        return switch (platform.toLowerCase()) {
            case ANDROID -> {
                // Android 平板: 非 mobile 或 model 包含 pad/tab
                if (!isMobile || isTabletModel(model)) {
                    yield WindClientDeviceType.ANDROID_PAD;
                }
                yield WindClientDeviceType.ANDROID_PHONE;
            }
            case IOS -> {
                // iOS 无法直接区分 iPad，需要 UA fallback
                String ua = req.getHeader("User-Agent");
                if (ua != null && ua.toLowerCase().contains("ipad")) {
                    yield WindClientDeviceType.IPAD;
                }
                yield WindClientDeviceType.IPHONE;
            }
            case MACOS -> WindClientDeviceType.MAC;
            case WINDOWS -> WindClientDeviceType.WINDOWS;
            case LINUX -> WindClientDeviceType.LINUX;
            default -> null;
        };
    }

    private static boolean isTabletModel(String model) {
        if (model == null) {
            return false;
        }
        String m = model.toLowerCase();
        return m.contains(IPAD) || m.contains(TABLET) || m.contains(GALAXY_TAB) || m.contains(NEXUS_7) || m.contains(NEXUS_9);
    }


    private static String header(HttpServletRequest req, String name) {
        String val = req.getHeader(name);
        if (val == null) {
            return null;
        }
        return val.replace("\"", "").trim();
    }

    // ==========================
    // 2. UA + uap-java 解析逻辑
    // ==========================

    @Nullable
    private static WindClientDeviceType parseFromUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        String uaLower = userAgent.toLowerCase();
        try {
            Client client = UA_PARSER.parse(userAgent);
            WindClientDeviceType type = parseByDeviceFamily(client.device.family, uaLower);
            if (type != null) {
                return type;
            }
            type = parseByOSFamily(client.os.family, client.device.family, uaLower);
            if (type != null) {
                return type;
            }
        } catch (Exception exception) {
            log.debug("解析设备类型失败, userAgent = {}", userAgent, exception);
        }

        return fallbackParseUA(uaLower);
    }


    private static WindClientDeviceType parseByDeviceFamily(String deviceFamily, String ua) {
        if (deviceFamily == null) return null;

        String d = deviceFamily.toLowerCase();

        if (d.contains(IPAD)) return WindClientDeviceType.IPAD;
        if (d.contains(IPHONE) || d.contains(IPOD)) {
            return WindClientDeviceType.IPHONE;
        }

        if (d.contains(ANDROID)) {
            return isAndroidTablet(d, ua)
                    ? WindClientDeviceType.ANDROID_PAD
                    : WindClientDeviceType.ANDROID_PHONE;
        }

        if (d.contains(TABLET)) return WindClientDeviceType.ANDROID_PAD;

        return null;
    }

    private static WindClientDeviceType parseByOSFamily(String os, String device, String ua) {
        if (os == null) {
            return null;
        }

        String o = os.toLowerCase();
        if (o.contains(IOS)) {
            if (device != null && device.toLowerCase().contains(IPAD)) {
                return WindClientDeviceType.IPAD;
            }
            return WindClientDeviceType.IPHONE;
        }

        if (o.contains(ANDROID)) {
            return isAndroidTablet(device, ua) ? WindClientDeviceType.ANDROID_PAD : WindClientDeviceType.ANDROID_PHONE;
        }

        if (o.contains(WINDOWS)) return WindClientDeviceType.WINDOWS;
        if (o.contains(MAC)) return WindClientDeviceType.MAC;
        if (o.contains(LINUX)) return WindClientDeviceType.LINUX;

        return null;
    }


    private static boolean isAndroidTablet(String device, String ua) {
        ua = ua == null ? WindConstants.EMPTY : ua;
        String d = device == null ? WindConstants.EMPTY : device.toLowerCase();
        if (d.contains(TABLET) || ua.contains(TABLET)) {
            return true;
        }
        String[] keywords = {IPAD, TABLET, MEDIA_PAD, GALAXY_TAB, NEXUS_7, NEXUS_9, KINDLE_FIRE};
        for (String k : keywords) {
            if (d.contains(k) || ua.contains(k)) return true;
        }
        return ua.contains(ANDROID) && !ua.contains(MOBILE);
    }

    @Nullable
    private static WindClientDeviceType fallbackParseUA(String ua) {
        if (ua.contains(IPAD)) {
            return WindClientDeviceType.IPAD;
        }
        if (ua.contains(IPHONE) || ua.contains(IPOD)) {
            return WindClientDeviceType.IPHONE;
        }
        if (ua.contains(ANDROID)) {
            return ua.contains(MOBILE) ? WindClientDeviceType.ANDROID_PHONE : WindClientDeviceType.ANDROID_PAD;
        }
        if (ua.contains(WINDOWS)) {
            return WindClientDeviceType.WINDOWS;
        }
        if (ua.contains(MAC)) {
            return WindClientDeviceType.MAC;
        }
        if (ua.contains(LINUX)) {
            return WindClientDeviceType.LINUX;
        }
        return null;
    }

    @Data
    public static class UserClientInfo {

        private WindClientDeviceType deviceType;

        private String deviceId;

        private String os;

        private String osVersion;

        private String clientAppVersion;

        private String authenticationMethod;
    }
}
