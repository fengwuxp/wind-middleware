package com.wind.security.configuration;

import com.wind.common.locks.JdkLockFactory;
import com.wind.common.locks.LockFactory;
import com.wind.security.authentication.AuthenticationTokenCodecService;
import com.wind.security.authentication.AuthenticationTokenUserMap;
import com.wind.security.authentication.WindAuthenticationProperties;
import com.wind.security.authentication.jwt.DefaultJwtAuthenticationTokenCodecService;
import com.wind.security.authentication.jwt.JwtProperties;
import com.wind.security.jwt.JwtTokenCodec;
import com.wind.security.authority.SimpleSecurityAccessOperations;
import com.wind.security.authority.WebRequestAuthorityLoader;
import com.wind.security.authority.WebRequestAuthorizationManager;
import com.wind.security.core.SecurityAccessOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.wind.common.WindConstants.ENABLED_NAME;
import static com.wind.common.WindConstants.TRUE;

/**
 * @author wuxp
 * @date 2023-09-26 13:00
 **/
@Configuration
@EnableConfigurationProperties(value = {com.wind.security.configuration.WindSecurityProperties.class})
@ConditionalOnProperty(prefix = com.wind.security.configuration.WindSecurityProperties.PREFIX, name = ENABLED_NAME, havingValue = TRUE,
        matchIfMissing = true)
public class WindSecurityAutoConfiguration {

    /**
     * jwt  配置 prefix
     */
    private static final String JWT_PREFIX = com.wind.security.configuration.WindSecurityProperties.PREFIX + ".jwt";

    /**
     * Authentication 配置 prefix
     */
    public static final String AUTHENTICATION_PREFIX = com.wind.security.configuration.WindSecurityProperties.PREFIX + ".authentication.crypto";

    @Bean
    @ConditionalOnProperty(prefix = JWT_PREFIX, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
    @ConfigurationProperties(prefix = JWT_PREFIX)
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = AUTHENTICATION_PREFIX, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
    @ConfigurationProperties(prefix = AUTHENTICATION_PREFIX)
    public WindAuthenticationProperties windAuthenticationProperties() {
        return new WindAuthenticationProperties();
    }

    @Bean
    @ConditionalOnBean(JwtProperties.class)
    public JwtTokenCodec jwtTokenCodec(JwtProperties properties) {
        return JwtTokenCodec.builder()
                .issuer(properties.getIssuer())
                .audience(properties.getAudience())
                .effectiveTime(properties.getEffectiveTime())
                .refreshEffectiveTime(properties.getRefreshEffectiveTime())
                .rsaKeyPair(properties.getKeyPair())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(LockFactory.class)
    public LockFactory jdkLockFactory() {
        return new JdkLockFactory();
    }

    @Bean
    @ConditionalOnMissingBean({SecurityAccessOperations.class})
    public SecurityAccessOperations securityAccessOperations() {
        return new SimpleSecurityAccessOperations("ROLE_");
    }

    @Bean
    @ConditionalOnBean({WebRequestAuthorityLoader.class})
    public WebRequestAuthorizationManager webRequestAuthorizationManager(WebRequestAuthorityLoader webRequestAuthorityLoader,
                                                                         SecurityAccessOperations securityAccessOperations) {
        return new WebRequestAuthorizationManager(webRequestAuthorityLoader, securityAccessOperations);
    }

    @Bean
    @ConditionalOnBean({JwtTokenCodec.class, AuthenticationTokenUserMap.class})
    public AuthenticationTokenCodecService defaultAuthenticationTokenCodecService(JwtTokenCodec codec, AuthenticationTokenUserMap tokenUserMap) {
        return new DefaultJwtAuthenticationTokenCodecService(codec, tokenUserMap);
    }
}



