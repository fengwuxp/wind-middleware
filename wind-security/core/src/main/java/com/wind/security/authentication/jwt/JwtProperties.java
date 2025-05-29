package com.wind.security.authentication.jwt;

import com.wind.security.AbstractRsaProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.http.HttpHeaders;

import java.time.Duration;


/**
 * @author wuxp
 * <a href="https://llbetter.com/JWT-default-claims/">JWT(Json Web Token)中默认的声明含义</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class JwtProperties extends AbstractRsaProperties {

    /**
     * jwt issuer
     * jwt的颁发者，其值应为大小写敏感的字符串或Uri。
     */
    private String issuer;

    /**
     * jwt audience
     * jwt的适用对象，其值应为大小写敏感的字符串或 Uri。一般可以为特定的 App、服务或模块
     */
    private String audience;

    /**
     * Generate token to set expire time
     */
    private Duration effectiveTime = Duration.ofHours(2);

    /**
     * refresh jwt token expire time
     */
    private Duration refreshEffectiveTime = Duration.ofDays(3);

    /**
     * token 请求头名称
     */
    private String headerName = HttpHeaders.AUTHORIZATION;

    /**
     * rsa 公钥
     */
    private String rsaPublicKey;

    /**
     * rsa 私钥
     */
    private String rsaPrivateKey;

}
