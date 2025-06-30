package com.wind.security.authentication;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Authentication User
 *
 * @author wuxp
 * @date 2023-10-26 12:49
 **/
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WindAuthenticationUser implements Serializable {

    @Serial
    private static final long serialVersionUID = -7270843983936135796L;

    /**
     * 用户 id
     * TODO 待优化 改为 String，临时兼容旧逻辑
     */
    private Object id;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户属性
     */
    private Map<String, Object> attributes = Collections.emptyMap();

    public WindAuthenticationUser(Long id, String userName) {
        this(String.valueOf(id), userName);
    }

    public WindAuthenticationUser(String id, String userName) {
        this(id, userName, Collections.emptyMap());
    }

    /**
     * 获取长整型的 id
     *
     * @return id
     */
    public Long getIdAsLong() {
        if (id instanceof Number number) {
            return number.longValue();
        }
        if (id instanceof String str) {
            return Long.parseLong(str);
        }
        throw BaseException.common("invalid user id");
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
