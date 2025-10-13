package com.wind.middleware.idempotent;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

/**
 * 幂等 key 存储
 *
 * @author wuxp
 * @date 2025-10-13 10:33
 **/
public interface WindIdempotentKeyStorage {

    /**
     * 保存幂等 key 及其执行结果
     *
     * @param idempotentKey 幂等 key
     * @param value         幂等执行结果
     */
    void save(@NotBlank String idempotentKey, Object value);

    /**
     * 检查幂等 key 是否存在并获取幂等执行结果, 如果未执行过则返回 null
     *
     * @param idempotentKey 幂等 key
     * @return 幂等执行结果
     */
    @Nullable
    WindIdempotentValueWrapper checkExistsAndGetValue(@NotBlank String idempotentKey);

    /**
     * 判断幂等 key 是否存在
     *
     * @param idempotentKey 幂等 key
     * @return if true 执行过
     */
    boolean exists(@NotBlank String idempotentKey);


}
