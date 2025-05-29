package com.wind.security.authentication;

import com.wind.common.exception.BaseException;
import com.wind.security.authentication.jwt.JwtTokenCodec;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wuxp
 * @date 2025-05-20 15:15
 **/
class DefaultJwtAuthenticationTokenCodecServiceTests {

    private final AuthenticationTokenUserMap tokenUserMap = getTokenUserMap();

    private final AuthenticationTokenCodecService tokenCodecService = createCodeService(tokenUserMap);

    @Test
    void testGenerateToken() {
        WindAuthenticationToken token = tokenCodecService.generateToken(new WindAuthenticationUser(1L, RandomStringUtils.randomAlphabetic(12)));
        WindAuthenticationToken parsed = tokenCodecService.parseAndValidateToken(token.getTokenValue());
        Assertions.assertEquals(token.getId(), parsed.getId());
        Assertions.assertEquals(token.getSubject(), parsed.getSubject());
    }

    @Test
    void testParseAndValidateTokenWithException() {
        WindAuthenticationToken token = tokenCodecService.generateToken(new WindAuthenticationUser(1L, RandomStringUtils.randomAlphabetic(12)));
        tokenUserMap.removeTokenId(token.getSubject());
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> tokenCodecService.parseAndValidateToken(token.getTokenValue()));
        Assertions.assertEquals("invalid access token user", exception.getMessage());
    }

    @Test
    void testGenerateRefreshToken() {
        WindAuthenticationToken token = tokenCodecService.generateRefreshToken("1");
        WindAuthenticationToken parsed = tokenCodecService.parseAndValidateRefreshToken(token.getTokenValue());
        Assertions.assertEquals(token.getId(), parsed.getId());
        Assertions.assertEquals(token.getSubject(), parsed.getSubject());
    }

    @Test
    void testParseAndValidateRefreshTokenWithException() {
        WindAuthenticationToken token = tokenCodecService.generateRefreshToken("1");
        tokenCodecService.revokeAllToken(token.getSubject());
        BaseException exception = Assertions.assertThrows(BaseException.class,
                () -> tokenCodecService.parseAndValidateRefreshToken(token.getTokenValue()));
        Assertions.assertEquals("invalid refresh token user", exception.getMessage());
    }

    private AuthenticationTokenCodecService createCodeService(AuthenticationTokenUserMap tokenStore) {
        JwtTokenCodec tokenCodec = JwtTokenCodecHelper.createCodec(Duration.ofHours(1));
        return new DefaultJwtAuthenticationTokenCodecService(tokenCodec, tokenStore);
    }

    private AuthenticationTokenUserMap getTokenUserMap() {
        return new AuthenticationTokenUserMap() {

            private final Map<String, String> caches = new HashMap<>();

            @Override
            public void put(String userId, String tokenId) {
                caches.put(userId, tokenId);
            }

            @Override
            public String getTokenId(String userId) {
                return caches.get(userId);
            }

            @Override
            public void removeTokenId(String userId) {
                caches.remove(userId);
            }
        };
    }
}
