package com.wind.websocket.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * websocket 会话消息查询服务
 *
 * @param <T> 消息类型
 * @author wuxp
 * @date 2025-05-30 16:53
 **/
public interface WindSocketSessionMessageQueryService<T> {

    /**
     * 获取会话消息，按照发送顺序倒序
     *
     * @param sessionId 会话 id
     * @param queryPage 查询yem
     * @param querySize 查询数量
     * @return 消息列表，如果查询结果条数小于查询数量，则表示查询结束
     */
    @NotNull
    List<T> querySessionMessages(@NotBlank String sessionId, int queryPage, int querySize);

    /**
     * 获取会话消息，按照发送顺序倒序
     *
     * @param sessionId 会话 id
     * @param queryPage 默认查询第一页
     * @return 消息列表
     */
    default List<T> querySessionMessages(@NotBlank String sessionId, int queryPage) {
        return querySessionMessages(sessionId, queryPage, 100);
    }

}
