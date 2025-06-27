package com.wind.security.core.rbac;

import com.wind.common.util.StringJoinSplitUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * rbac 资源定义
 *
 * @author wuxp
 * @date 2023-09-26 07:41
 **/
public interface RbacResource extends Serializable {

    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * @return 用于展示的名称
     */
    String getName();

    /**
     * rbac 权限资源
     */
    interface Permission extends RbacResource {

        /**
         * @return 权限内容
         */
        Set<String> getAttributes();

        /**
         * 创建一个不可变的权限对象
         *
         * @param id    权限 id
         * @param name  权限名称
         * @param value 权限值，使用逗号分隔
         * @return 权限
         */
        static Permission immutable(String id, String name, String value) {
            Set<String> attributes = StringUtils.hasLength(value) ? StringJoinSplitUtils.split(value) : Collections.emptySet();
            return immutable(id, name, attributes);
        }

        static Permission immutable(String id, String name, Set<String> attributes) {
            return new ImmutablePermission(id, name, attributes);
        }
    }

    /**
     * rbac 角色资源
     */
    interface Role extends RbacResource {

        /**
         * 角色关联的权限
         *
         * @return 权限列表
         */
        @NotNull
        default Set<String> getPermissionsIds() {
            Set<Permission> permissions = getPermissions();
            return permissions == null ? Collections.emptySet() : permissions
                    .stream()
                    .map(Permission::getId)
                    .collect(Collectors.toSet());
        }

        @NotNull
        Set<RbacResource.Permission> getPermissions();

        /**
         * 创建一个不可变的角色对象
         *
         * @param id          权限 ID
         * @param name        权限名称
         * @param permissions 权限值
         * @return 角色
         */
        static Role immutable(String id, String name, Set<RbacResource.Permission> permissions) {
            return new ImmutableRole(id, name, permissions);
        }
    }

    /**
     * 用户角色
     */
    @AllArgsConstructor
    @Getter
    class UserRole {

        /**
         * 角色 id
         */
        private final String roleId;

        /**
         * 过期时间戳，为空表示不过期
         */
        @Nullable
        private final Long expireTime;

        /**
         * 为了给序列化框架使用，提供一个空构造
         */
        public UserRole() {
            this("", null);
        }

        public static UserRole immutable(String roleId, Long expireTime) {
            return new UserRole(roleId, expireTime);
        }

        public static UserRole immutable(String roleId) {
            return immutable(roleId, null);
        }
    }

    /**
     * rbac 用户资源
     */
    interface User extends RbacResource {

    }

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode(of = "id")
    class ImmutablePermission implements RbacResource.Permission {

        @Serial
        private static final long serialVersionUID = 6255678473411919964L;

        /**
         * id
         */
        private final String id;

        /**
         * 权限名称
         */
        private final String name;

        /**
         * 权限内容
         */
        private final Set<String> attributes;

        /**
         * 为了给序列化框架使用，提供一个空构造
         */
        ImmutablePermission() {
            this("", "", Collections.emptySet());
        }

    }

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode(of = "id")
    class ImmutableRole implements RbacResource.Role {

        @Serial
        private static final long serialVersionUID = -6791142921724321619L;

        /**
         * id
         */
        private final String id;

        /**
         * 角色名称
         */
        private final String name;

        /**
         * 权限内容
         */
        private final Set<RbacResource.Permission> permissions;

        /**
         * 为了给序列化框架使用，提供一个空构造
         */
        ImmutableRole() {
            this("", "", Collections.emptySet());
        }
    }

}
