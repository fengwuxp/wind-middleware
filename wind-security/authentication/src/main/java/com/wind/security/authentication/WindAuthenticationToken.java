package com.wind.security.authentication;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Authentication Token
 *
 * @author wuxp
 * @date 2023-09-26 09:35
 **/
@AllArgsConstructor
@Getter
public final class WindAuthenticationToken implements Serializable {

    private static final long serialVersionUID = 1801730423764136024L;

    /**
     * token id 全局唯一的
     */
    private final String id;

    /**
     * token
     */
    private final String tokenValue;

    /**
     * jwt subject，一般是用户 ID
     */
    private final String subject;

    /**
     * 用户信息，如果是 refresh token，该字段为空
     */
    @Nullable
    private final WindAuthenticationUser user;

    /**
     * token 过期时间戳
     */
    private final Long expireTime;

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
