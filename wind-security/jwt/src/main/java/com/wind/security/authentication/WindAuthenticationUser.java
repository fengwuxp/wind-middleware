package com.wind.security.authentication;

import com.wind.common.exception.AssertUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication User
 *
 * @author wuxp
 * @date 2023-10-26 12:49
 **/
@AllArgsConstructor
@Getter
public class WindAuthenticationUser implements Serializable {

    @Serial
    private static final long serialVersionUID = -7270843983936135796L;

    /**
     * 用户 id
     */
    private final String id;

    /**
     * 用户名
     */
    private final String userName;

    /**
     * 用户属性
     */
    private final Map<String, Object> attributes;

    public WindAuthenticationUser(Long id, String userName) {
        this(String.valueOf(id), userName);
    }

    public WindAuthenticationUser(String id, String userName) {
        this(id, userName, new HashMap<>());
    }

    /**
     * 获取长整型的 id
     *
     * @return id
     */
    public Long getIdAsLong() {
        return Long.parseLong(id);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 获取属性并判断空
     *
     * @param key 属性名称
     * @return 属性值
     */
    public <T> T requireAttribute(String key) {
        T result = getAttribute(key);
        AssertUtils.notNull(result, () -> String.format("attribute name = %s must not null", key));
        return result;
    }
}
