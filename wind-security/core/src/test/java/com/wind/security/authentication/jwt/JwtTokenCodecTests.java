package com.wind.security.authentication.jwt;

import com.wind.security.authentication.JwtTokenCodecHelper;
import com.wind.security.authentication.WindAuthenticationToken;
import com.wind.security.authentication.WindAuthenticationUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.Base64Utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

class JwtTokenCodecTests {

    private final JwtTokenCodec jwtTokenCodec = JwtTokenCodecHelper.createCodec(Duration.ofMinutes(1));

    @Test
    void testCodecUserToken() {
        WindAuthenticationUser user = new WindAuthenticationUser(1L, "");
        WindAuthenticationToken token = jwtTokenCodec.encoding(user);
        WindAuthenticationUser result = jwtTokenCodec.parse(token.getTokenValue()).getUser();
        Assertions.assertEquals(user, result);
    }

    @Test
    void testCodecUserTokenWithTtlExpired() {
        WindAuthenticationUser user = new WindAuthenticationUser(1L, "");
        WindAuthenticationToken token = jwtTokenCodec.encoding(user, Duration.ofNanos(1));
        JwtExpiredException exception = Assertions.assertThrows(JwtExpiredException.class, () -> jwtTokenCodec.parse(token.getTokenValue()));
        Assertions.assertEquals("token is expired", exception.getMessage());
    }

    @Test
    void testJwtTokenEqJti() {
        WindAuthenticationUser user = new WindAuthenticationUser(1L, "");
        WindAuthenticationToken token = jwtTokenCodec.encoding(user);
        Assertions.assertNotNull(token.getId());
        WindAuthenticationToken parsed = jwtTokenCodec.parse(token.getTokenValue());
        Assertions.assertEquals(parsed.getId(), token.getId());
    }

    @Test
    void testEncodingJwtTokenNotEqJti() throws Exception {
        WindAuthenticationUser user = new WindAuthenticationUser(1L, "");
        WindAuthenticationToken token1 = jwtTokenCodec.encoding(user);
        WindAuthenticationToken token2 = jwtTokenCodec.encoding(user);
        Assertions.assertNotEquals(token1.getId(), token2.getId());
    }

    @Test
    void testCodecUserTokenExpired() throws Exception {
        JwtTokenCodec codec = JwtTokenCodecHelper.createCodec(Duration.ofMillis(100));
        WindAuthenticationUser user = new WindAuthenticationUser(1L, "");
        WindAuthenticationToken token = codec.encoding(user);
        Thread.sleep(101);
        JwtExpiredException exception = Assertions.assertThrows(JwtExpiredException.class, () -> codec.parse(token.getTokenValue()));
        Assertions.assertEquals("token is expired", exception.getMessage());
    }

    @Test
    void testCodecRefreshToken() {
        WindAuthenticationToken token = jwtTokenCodec.encodingRefreshToken("1");
        Assertions.assertEquals(1L, token.getSubjectAsLong());
        Assertions.assertEquals(1L, jwtTokenCodec.parseRefreshToken(token.getTokenValue()).getSubjectAsLong());
    }

    @Test
    void testRefreshTokenEqJti() {
        WindAuthenticationToken token = jwtTokenCodec.encodingRefreshToken("1");
        WindAuthenticationToken parsed = jwtTokenCodec.parseRefreshToken(token.getTokenValue());
        Assertions.assertEquals(parsed.getId(), token.getId());
    }

    @Test
    void testRefreshTokenExpired() throws Exception {
        JwtTokenCodec codec = JwtTokenCodecHelper.createCodec(JwtTokenCodecHelper.jwtProperties(Duration.ofMillis(100)));
        WindAuthenticationToken token = codec.encodingRefreshToken("1");
        Thread.sleep(101);
        String tokenValue = token.getTokenValue();
        JwtExpiredException exception = Assertions.assertThrows(JwtExpiredException.class, () -> codec.parseRefreshToken(tokenValue));
        Assertions.assertEquals("refresh token is expired", exception.getMessage());
    }


}