package com.wind.core;

import jakarta.validation.constraints.NotBlank;

/**
 * 凭据提供者
 *
 * @author wuxp
 * @date 2025-02-18 15:14
 **/
public interface WindCredentialsProvider {

    /**
     * 加载配置凭据
     *
     * @param credentialsName 凭据名称
     * @return 凭据内容
     */
    String getCredentials(@NotBlank String credentialsName);

}
