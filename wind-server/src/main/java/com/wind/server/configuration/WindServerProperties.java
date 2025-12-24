package com.wind.server.configuration;

import com.wind.api.core.signature.ApiSecretAccount;
import com.wind.api.core.signature.ApiSignAlgorithm;
import com.wind.common.WindConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Set;

import static com.wind.common.WindConstants.WIND_SERVER_PROPERTIES_PREFIX;

/**
 * @author wuxp
 * @date 2023-09-27 11:56
 **/
@Data
@ConfigurationProperties(prefix = WIND_SERVER_PROPERTIES_PREFIX)
public class WindServerProperties {

    /**
     * 开启 wind server supports
     */
    private boolean enabled = true;

    /**
     * 控制器 Aop 拦截增强配置
     */
    private ControllerMethodAspectProperties controllerMethodAspect = new ControllerMethodAspectProperties();

    /**
     * api 签名配置
     */
    private ApiSignatureProperties apiSignature;


    @Data
    public static class ControllerMethodAspectProperties {

        /**
         * spring aop aspect pointcut 表达式
         * <a href="https://zhuanlan.zhihu.com/p/63001123">spring aop中pointcut表达式完整版</a>
         * <a href="https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/pointcuts.html">Expression Supported Pointcut</a>
         */
        private String expression;

    }

    /**
     * api 签名配置
     */
    @Data
    public static class ApiSignatureProperties implements ApiSecretAccount {

        /**
         * 签名请求头前缀
         */
        private String headerPrefix = WindConstants.WIND;

        /**
         * 访问标识
         *
         */
        private String accessId;

        /**
         * 签名秘钥
         */
        private String secretKey;

        /**
         * 签名秘钥版本
         *
         */
        private String secretVersion;

        /**
         * 签名算法实现
         *
         */
        private ApiSignAlgorithm signer;

        /**
         * 忽略签名的请求匹配器
         */
        private Set<String> ignorePatterns = Collections.emptySet();

    }
}
