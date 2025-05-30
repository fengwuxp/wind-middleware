package com.wind.server.i18n;

import com.wind.common.enums.ConfigFileType;
import lombok.Data;
import org.springframework.boot.convert.DurationUnit;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

/**
 * @author wuxp
 * @date 2023-10-30 09:35
 **/
@Data
public class WindMessageSourceProperties {

    /**
     * Message bundles encoding.
     */
    private Charset encoding = StandardCharsets.UTF_8;

    /**
     * Loaded resource bundle files cache duration. When not set, bundles are cached
     * forever. If a duration suffix is not specified, seconds will be used.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration cacheDuration = Duration.ofDays(1);

    /**
     * Whether to fall back to the system Locale if no files for a specific Locale have
     * been found. if this is turned off, the only fallback will be the default file (e.g.
     * "messages.properties" for basename "messages").
     */
    private boolean fallbackToSystemLocale = true;

    /**
     * Whether to always apply the MessageFormat rules, parsing even messages without
     * arguments.
     */
    private boolean alwaysUseMessageFormat = false;

    /**
     * Whether to use the message code as the default message instead of throwing a
     * "NoSuchMessageException". Recommended during development only.
     */
    private boolean useCodeAsDefaultMessage = false;

    /**
     * i18n 国际化 key 配置的前缀
     */
    private String i18nMessageKeyPrefix = "";

    /**
     * 国际化配置名称，一般是应用名
     */
    private String name;

    /**
     * 支持的国际化 Locales
     */
    private Set<Locale> locales;

    /**
     * 配置文件类型
     */
    private ConfigFileType fileType = ConfigFileType.JSON;
}
