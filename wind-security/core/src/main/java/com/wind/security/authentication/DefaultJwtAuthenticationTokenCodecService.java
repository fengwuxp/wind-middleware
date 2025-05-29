package com.wind.security.authentication;

import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.security.authentication.jwt.JwtTokenCodec;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;

/**
 * @author wuxp
 * @date 2025-05-20 14:38
 **/
@Slf4j
public class DefaultJwtAuthenticationTokenCodecService implements AuthenticationTokenCodecService {

    public static final String REFRESH_TOKEN_USER_PREFIX="__REFRESH__@";

    private final JwtTokenCodec jwtTokenCodec;

    private final AuthenticationTokenUserMap userTokenMap;

    private final AuthenticationTokenUserMap refreshTokenMap;

    public DefaultJwtAuthenticationTokenCodecService(JwtTokenCodec jwtTokenCodec, AuthenticationTokenUserMap delegate) {
        this.jwtTokenCodec = jwtTokenCodec;
        this.userTokenMap = new AuthenticationTokenUserMapWrapper(delegate, WindConstants.EMPTY);
        this.refreshTokenMap = new AuthenticationTokenUserMapWrapper(delegate, REFRESH_TOKEN_USER_PREFIX);
    }

    @Override
    public WindAuthenticationToken generateToken(WindAuthenticationUser user, Duration ttl) {
        WindAuthenticationToken result = jwtTokenCodec.encoding(user, ttl);
        userTokenMap.put(String.valueOf(user.getId()), result.getId());
        return result;
    }

    @Override
    public WindAuthenticationToken generateRefreshToken(String userId, Duration ttl) {
        WindAuthenticationToken result = jwtTokenCodec.encodingRefreshToken(userId, ttl);
        refreshTokenMap.put(userId, result.getId());
        return result;
    }

    @Override
    public WindAuthenticationToken parseAndValidateToken(String accessToken) {
        WindAuthenticationToken result = jwtTokenCodec.parse(accessToken);
        String tokenId = userTokenMap.getTokenId(result.getSubject());
        AssertUtils.hasText(tokenId, "invalid access token user");
        AssertUtils.isTrue(Objects.equals(tokenId, result.getId()), "invalid access token");
        return result;
    }

    @Override
    public WindAuthenticationToken parseAndValidateRefreshToken(String refreshToken) {
        WindAuthenticationToken result = jwtTokenCodec.parseRefreshToken(refreshToken);
        String tokenId = refreshTokenMap.getTokenId(result.getSubject());
        AssertUtils.hasText(tokenId, "invalid refresh token user");
        AssertUtils.isTrue(Objects.equals(tokenId, result.getId()), "invalid refresh token");
        return result;
    }

    @Override
    public void revokeAllToken(String userId) {
        try {
            userTokenMap.removeTokenId(userId);
        } catch (Exception ignore) {
            // ignore
        }
        refreshTokenMap.removeTokenId(userId);
    }


    @AllArgsConstructor
    private static class AuthenticationTokenUserMapWrapper implements AuthenticationTokenUserMap {

        private final AuthenticationTokenUserMap delegate;

        private final String prefix;

        @Override
        public void put(String userId, String tokenId) {
            delegate.put(prefix + userId, tokenId);
        }

        @Override
        public String getTokenId(String userId) {
            return delegate.getTokenId(prefix + userId);
        }

        @Override
        public void removeTokenId(String userId) {
            delegate.removeTokenId(prefix + userId);
        }
    }
}
