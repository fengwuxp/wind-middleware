package com.wind.security.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wuxp
 * @date 2023-09-26 13:00
 **/
@Data
@ConfigurationProperties(prefix = WindSecurityProperties.PREFIX)
public class WindSecurityProperties {

    /**
     * 配置 prefix
     */
    public static final String PREFIX = "wind.security";

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 默认的角色值前缀
     *
     * @see org.springframework.security.authorization.AuthorityAuthorizationManager#ROLE_PREFIX
     */
    private String rolePrefix = "ROLE_";

}
