package com.wind.websocket.core;

import com.wind.core.ObjectReadonlyMetadata;
import com.wind.websocket.WindWebSocketMetadataNames;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.LocalDateTime;

/**
 * websocket connection 描述符
 *
 * @author wuxp
 * @date 2025-05-31 09:06
 **/
public interface SocketClientConnectionDescriptor extends ObjectReadonlyMetadata {

    /**
     * @return 获取连接 id
     */
    @NotBlank
    String getId();

    /**
     * @return 获取连接关联的会话 id
     */
    String getSessionId();

    /**
     * @return 客户端唯一标识，可以在创建连接时通过参数传递
     */
    @Null
    default String getClientId() {
        return getMetadataValue(WindWebSocketMetadataNames.CLIENT_ID_NAME);
    }

    /**
     * @return 客户端设备类型
     */
    default String getClientDeviceType() {
        return getMetadataValue(WindWebSocketMetadataNames.CLIENT_DEVICE_TYPE_NAME);
    }

    /**
     * @return 连接关联的用户 id
     */
    @Null
    default String getUserId() {
        return getMetadataValue(WindWebSocketMetadataNames.USER_ID_NAME);
    }

    /**
     * @return 获取用户代理信息
     */
    default String getUserAgent() {
        return getMetadataValue(WindWebSocketMetadataNames.USER_AGENT_NAME);
    }

    /**
     * @return 获取客户端ip
     */
    @NotBlank
    default String getClientIp() {
        return requireMetadataValue(WindWebSocketMetadataNames.CLIENT_IP_NAME);
    }

    /**
     * @return 是否已认证
     */
    default boolean isAuthenticated() {
        return getUserId() != null;
    }

    /**
     * @return 连接建立时间
     */
    @NotNull
    default LocalDateTime getGmtConnected() {
        return requireMetadataValue(WindWebSocketMetadataNames.GMT_CONNECTED_NAME);
    }
}
