package com.wind.security;

/**
 * @author wuxp
 * @date 2023-10-04 18:53
 **/
public final class WebSecurityConstants {

    private WebSecurityConstants() {
        throw new AssertionError();
    }


    /**
     * 保存在请求上线文中，当前请求需要的权限
     */
    public static final String REQUEST_REQUIRED_AUTHORITIES_ATTRIBUTE_NAME = "REQUEST_REQUIRED_AUTHORITIES";

    /**
     * rbac 权限缓存名称
     */
    public static final String RBAC_PERMISSION_CACHE_NAME = "RBAC_PERMISSION_CACHE";

    /**
     * rbac 角色缓存名称
     */
    public static final String RBAC_ROLE_CACHE_NAME = "RBAC_ROLE_CACHE";

    /**
     * rbac 用户角色缓存名称
     */
    public static final String RBAC_USER_ROLE_CACHE_NAME = "RBAC_USER_ROLE_CACHE";

    /**
     * 登录已失效
     */
    public static final String LOGIN_JWT_TOKEN_INVALID = "登录凭证不存在或已过期";
}
