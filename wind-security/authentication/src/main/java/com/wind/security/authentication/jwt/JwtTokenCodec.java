package com.wind.security.authentication.jwt;

import com.alibaba.fastjson2.JSON;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
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
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * @author wuxp
 * @date 2023-09-24 16:59
 **/
public final class JwtTokenCodec {

    private static final String AUTHENTICATION_VARIABLE_NAME = "authentication";

    private static final String JWT_AUTH_KEY_ID = "jwt-auth-codec-kind";

    private final JwtProperties properties;

    private final JwtEncoder jwtEncoder;

    private final JwtDecoder jwtDecoder;

    private final JwsHeader jwsHeader = JwsHeader.with(SignatureAlgorithm.RS256).build();

    public JwtTokenCodec(JwtProperties properties) {
        this.properties = properties;
        RSAKey rsaKey = generateRsaKey(properties.getKeyPair());
        this.jwtEncoder = buildJwtEncoder(rsaKey);
        this.jwtDecoder = buildJwtDecoder(rsaKey);
    }

    @Nullable
    public <T> JwtTokenPayload<T> parse(String jwtToken, Class<T> classType) {
        if (StringUtils.hasLength(jwtToken)) {
            Jwt jwt = jwtDecoder.decode(jwtToken);
            Map<String, Object> claims = jwt.getClaims();
            T user = JSON.to(classType, claims.get(AUTHENTICATION_VARIABLE_NAME));
            return new JwtTokenPayload<>(jwt.getSubject(), user);
        }
        return null;
    }

    /**
     * 生成用户 token
     *
     * @param id   用户 id
     * @param user 用户信息
     * @return 用户 token
     */
    public String encoding(String id, Object user) {
        Jwt jwt = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        jwsHeader,
                        JwtClaimsSet.builder()
                                .expiresAt(Instant.now().plusSeconds(properties.getEffectiveTime().getSeconds()))
                                .audience(Collections.singletonList(properties.getAudience()))
                                .issuer(properties.getIssuer())
                                .subject(id)
                                .claim(AUTHENTICATION_VARIABLE_NAME, user)
                                .build()
                )
        );
        return jwt.getTokenValue();
    }

    /**
     * 生成 refresh token
     *
     * @param id 用户 id
     * @return refresh token
     */
    public String encodingRefreshToken(String id) {
        Jwt jwt = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        jwsHeader,
                        JwtClaimsSet.builder()
                                .expiresAt(Instant.now().plusSeconds(properties.getRefreshEffectiveTime().getSeconds()))
                                .audience(Collections.singletonList(properties.getAudience()))
                                .subject(id)
                                .build()
                )
        );
        return jwt.getTokenValue();
    }

    /**
     * 解析 验证 refresh token
     *
     * @param refreshToken refresh token
     * @return 用户 id
     */
    @Nullable
    public String parseRefreshToken(String refreshToken) {
        if (StringUtils.hasLength(refreshToken)) {
            return jwtDecoder.decode(refreshToken).getSubject();
        }
        return null;
    }

    private JwtDecoder buildJwtDecoder(RSAKey rsaKey) {
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, new ImmutableJWKSet<>(new JWKSet(rsaKey))));
        return new NimbusJwtDecoder(processor);
    }

    private JwtEncoder buildJwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    private RSAKey generateRsaKey(KeyPair keyPair) {
        // https://github.com/spring-projects/spring-security/blob/main/oauth2/oauth2-jose/src/test/java/org/springframework/security/oauth2/jose/TestKeys.java
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(JWT_AUTH_KEY_ID)
                .build();
    }
}