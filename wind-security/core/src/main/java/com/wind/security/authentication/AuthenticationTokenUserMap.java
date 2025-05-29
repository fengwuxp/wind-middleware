package com.wind.security.authentication;

import org.springframework.lang.Nullable;

/**
 * 认证 token id 和用户关系的 map
 *
 * @author wuxp
 * @date 2025-05-20 14:45
 **/
public interface AuthenticationTokenUserMap {

    /**
     * put token id 和用户 userId 的关系
     *
     * @param userId  用户 id
     * @param tokenId token id
     */
    void put(String userId, String tokenId);

    /**
     * 通过 用户 id 获取用户 tokenId
     *
     * @param userId 用户 id
     */
    @Nullable
    String getTokenId(String userId);

    /**
     * 移除 token id
     *
     * @param userId 用户 id
     */
    void removeTokenId(String userId);
}
