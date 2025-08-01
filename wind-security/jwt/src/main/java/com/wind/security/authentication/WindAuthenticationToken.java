package com.wind.security.authentication;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Authentication Token
 *
 * @param id         token id 全局唯一的
 * @param tokenValue token
 * @param subject    jwt subject，一般是用户 ID
 * @param user       用户信息，如果是 refresh token，该字段为空
 * @param expireTime token 过期时间戳
 * @author wuxp
 * @date 2023-09-26 09:35
 */
public record WindAuthenticationToken(String id, String tokenValue, String subject, @Nullable WindAuthenticationUser user,
                                      Long expireTime) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1801730423764136024L;

    /**
     * 获取长整型的 subject
     *
     * @return subject
     */
    public Long getSubjectAsLong() {
        return Long.parseLong(subject);
    }

    @NotNull
    public WindAuthenticationUser requireUser() {
        AssertUtils.state(user != null, () -> BaseException.unAuthorized("user un authorized"));
        return user;
    }

    /**
     * @return 是否过期
     */
    public boolean isExpired() {
        return expireTime < System.currentTimeMillis();
    }
}
