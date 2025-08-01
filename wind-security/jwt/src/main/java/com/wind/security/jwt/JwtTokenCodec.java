package com.wind.security.jwt;

import com.alibaba.fastjson2.JSON;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.wind.common.exception.AssertUtils;
import com.wind.security.authentication.WindAuthenticationToken;
import com.wind.security.authentication.WindAuthenticationUser;
import com.wind.sequence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * jwt token 编码解码
 *
 * @author wuxp
 * @date 2023-09-24 16:59
 **/
public final class JwtTokenCodec {

    private static final String AUTHENTICATION_VARIABLE_NAME = "authentication";

    /**
     * jwt 密钥 id
     */
    private static final String JWT_AUTH_KEY_ID = "Wind-Jwt-Auth-Codec";

    /**
     * jwt issuer
     * jwt的颁发者，其值应为大小写敏感的字符串或Uri。
     */
    private final String issuer;

    /**
     * jwt audience
     * jwt的适用对象，其值应为大小写敏感的字符串或 Uri。一般可以为特定的 App、服务或模块
     */
    private final String audience;

    /**
     * Generate token to set expire time
     */
    private final Duration effectiveTime;

    /**
     * refresh jwt token expire time
     */
    private final Duration refreshEffectiveTime;

    private final JwtEncoder jwtEncoder;

    private final JwtDecoder jwtDecoder;

    private final JwsHeader jwsHeader = JwsHeader.with(SignatureAlgorithm.RS256).build();

    private JwtTokenCodec(JwtTokenCodecBuilder builder) {
        this.audience = builder.audience;
        this.issuer = builder.issuer;
        this.effectiveTime = builder.effectiveTime;
        this.refreshEffectiveTime = builder.refreshEffectiveTime;
        RSAKey rsaKey = generateRsaKey(builder.rsaKeyPair);
        this.jwtEncoder = buildJwtEncoder(rsaKey);
        this.jwtDecoder = buildJwtDecoder(rsaKey);
    }

    public static JwtTokenCodecBuilder builder() {
        return new JwtTokenCodecBuilder();
    }

    /**
     * jwt parse token
     * 注意：如果 token 过期了，将抛出 {@link JwtExpiredException} 异常
     *
     * @param jwtToken jwt token
     * @return jwt token payload
     */
    @NotNull
    public WindAuthenticationToken parse(@NotBlank String jwtToken) {
        AssertUtils.hasText(jwtToken, "argument token must not null");
        Jwt jwt = jwtDecoder.decode(jwtToken);
        Map<String, Object> claims = jwt.getClaims();
        WindAuthenticationUser user = JSON.to(WindAuthenticationUser.class, claims.get(AUTHENTICATION_VARIABLE_NAME));
        Instant expiresAt = Objects.requireNonNull(jwt.getExpiresAt(), "token expire must not null");
        AssertUtils.state(expiresAt.isAfter(Instant.now()), () -> new JwtExpiredException("token is expired"));
        return new WindAuthenticationToken(jwt.getId(), jwtToken, jwt.getSubject(), user, expiresAt.toEpochMilli());
    }

    /**
     * 生成用户 token
     *
     * @param user 用户信息
     * @return 用户 token
     */
    @NotNull
    public WindAuthenticationToken encoding(WindAuthenticationUser user) {
        return encoding(user, effectiveTime);
    }

    /**
     * 生成用户 token
     *
     * @param user 用户信息
     * @param ttl  token 有效期，为空则使用默认有效期
     * @return 用户 token
     */
    public WindAuthenticationToken encoding(WindAuthenticationUser user, @Nullable Duration ttl) {
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,
                        newJwtBuilder(String.valueOf(user.getId()), ttl == null ? effectiveTime : ttl)
                                .claim(AUTHENTICATION_VARIABLE_NAME, user)
                                .id(genJti(user))
                                .build()
                )
        );
        return new WindAuthenticationToken(jwt.getId(), jwt.getTokenValue(), jwt.getSubject(), user,
                Objects.requireNonNull(jwt.getExpiresAt()).toEpochMilli());
    }

    /**
     * 生成 refresh token
     *
     * @param userId 用户 id
     * @param ttl    refresh token 有效期，为空则使用默认有效期
     * @return refresh token
     */
    @NotNull
    public WindAuthenticationToken encodingRefreshToken(String userId, @Nullable Duration ttl) {
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,
                        newJwtBuilder(userId, ttl == null ? refreshEffectiveTime : ttl).id(genJti(userId)).build()
                )
        );
        return new WindAuthenticationToken(jwt.getId(), jwt.getTokenValue(), jwt.getSubject(), null,
                Objects.requireNonNull(jwt.getExpiresAt()).toEpochMilli());
    }

    /**
     * 生成 refresh token
     *
     * @param userId 用户 id
     * @return refresh token
     */
    @NotNull
    public WindAuthenticationToken encodingRefreshToken(String userId) {
        return encodingRefreshToken(userId, refreshEffectiveTime);
    }

    /**
     * 解析 验证 refresh token
     * 注意：如果 token 过期了，将抛出 {@link JwtExpiredException} 异常
     *
     * @param refreshToken refresh token
     * @return 用户 id
     */
    @NotNull
    public WindAuthenticationToken parseRefreshToken(@NotBlank String refreshToken) {
        AssertUtils.hasText(refreshToken, "argument token must not null");
        Jwt jwt = jwtDecoder.decode(refreshToken);
        Instant expiresAt = Objects.requireNonNull(jwt.getExpiresAt(), "refresh token expire must not null");
        AssertUtils.notNull(expiresAt, "jwt token expireAt must not null");
        AssertUtils.state(expiresAt.isAfter(Instant.now()), () -> new JwtExpiredException("refresh token is expired"));
        return new WindAuthenticationToken(jwt.getId(), refreshToken, jwt.getSubject(), null, expiresAt.toEpochMilli());
    }

    private JwtClaimsSet.Builder newJwtBuilder(String userId, Duration effectiveTime) {
        return JwtClaimsSet.builder().expiresAt(Instant.now().plusSeconds(effectiveTime.getSeconds())).audience(Collections.singletonList(audience)).issuer(issuer).subject(userId);
    }

    private JwtDecoder buildJwtDecoder(RSAKey rsaKey) {
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, new ImmutableJWKSet<>(new JWKSet(rsaKey))));
        return new NimbusJwtDecoder(processor);
    }

    private JwtEncoder buildJwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    /**
     * JWT 的唯一标识符（JWT ID）
     * 参见：<a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.7">JWT Registered Claim</a>
     *
     * @param userInfo 用户信息
     * @return jti
     */
    private String genJti(Object userInfo) {
        String text = String.format("%s@%d#%s", SequenceGenerator.randomAlphanumeric(getClass().getName(), 1024),
                System.currentTimeMillis(), userInfo);
        return DigestUtils.sha256Hex(text);
    }

    private RSAKey generateRsaKey(KeyPair keyPair) {
        // https://github.com/spring-projects/spring-security/blob/main/oauth2/oauth2-jose/src/test/java/org/springframework/security/oauth2/jose/TestKeys.java
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).privateKey(keyPair.getPrivate()).keyID(JWT_AUTH_KEY_ID).build();
    }


    public static class JwtTokenCodecBuilder {

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
         * rsa 秘钥对
         */
        private KeyPair rsaKeyPair;


        private JwtTokenCodecBuilder() {
        }

        public JwtTokenCodecBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public JwtTokenCodecBuilder audience(String audience) {
            this.audience = audience;
            return this;
        }

        public JwtTokenCodecBuilder effectiveTime(Duration effectiveTime) {
            this.effectiveTime = effectiveTime;
            return this;
        }

        public JwtTokenCodecBuilder refreshEffectiveTime(Duration refreshEffectiveTime) {
            this.refreshEffectiveTime = refreshEffectiveTime;
            return this;
        }

        public JwtTokenCodecBuilder rsaKeyPair(KeyPair rsaKeyPair) {
            this.rsaKeyPair = rsaKeyPair;
            return this;
        }

        public JwtTokenCodec build() {
            return new JwtTokenCodec(this);
        }
    }
}
