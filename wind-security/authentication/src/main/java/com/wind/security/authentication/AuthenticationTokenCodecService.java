package com.wind.security.authentication;

import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * 鉴权 token 生成\解析验证服务
 *
 * @author wuxp
 * @date 2025-05-20 14:15
 **/
public interface AuthenticationTokenCodecService {


    /**
     * 通过用户创建一个 Authentication token
     *
     * @param user 用户
     * @return Authentication token 对象
     */
    default WindAuthenticationToken generateToken(WindAuthenticationUser user) {
        return generateToken(user, null);
    }

    /**
     * 通过用户创建一个 Authentication token
     *
     * @param user 用户
     * @param ttl  token 的 存活时间，未指定则使用默认时长
     * @return Authentication token 对象
     */
    WindAuthenticationToken generateToken(WindAuthenticationUser user, @Nullable Duration ttl);

    /**
     * 通过用户创建一个 Authentication token
     *
     * @param userId 用户 id
     * @return Authentication token 对象
     */
    default WindAuthenticationToken generateRefreshToken(@NotNull Long userId) {
        return generateRefreshToken(String.valueOf(userId));
    }

    /**
     * 通过用户创建一个 Authentication token
     *
     * @param userId 用户 id
     * @return Authentication token 对象
     */
    default WindAuthenticationToken generateRefreshToken(@NotBlank String userId) {
        return generateRefreshToken(userId, null);
    }

    /**
     * 通过用户创建一个 Authentication token
     *
     * @param userId 用户 id
     * @param ttl    token 的 存活时间，未指定则使用默认时长
     * @return Authentication token 对象
     */
    WindAuthenticationToken generateRefreshToken(@NotBlank String userId, @Nullable Duration ttl);

    /**
     * 解析并检查 Authentication token
     *
     * @param accessToken Authentication token
     * @return Authentication token 对象
     */
    WindAuthenticationToken parseAndValidateToken(@NotBlank String accessToken);

    /**
     * 解析并检查 Authentication refresh token
     *
     * @param refreshToken Authentication token
     * @return Authentication token 对象
     */
    WindAuthenticationToken parseAndValidateRefreshToken(@NotBlank String refreshToken);

    /**
     * 撤销 access token 和 refresh token
     *
     * @param userId 用户 id
     */
    void revokeAllToken(@NotBlank String userId);

}
