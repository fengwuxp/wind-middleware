package com.wind.server.endpoint;

import com.wind.common.exception.AssertUtils;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Properties;

/**
 * 获取构建的 git info
 *
 * @author wuxp
 * @date 2023-12-13 14:04
 **/
@Component
@Endpoint(id = "gitinfos")
public class JarGitInfosEndpoint {

    private static final String GIT_INFOS_RESOURCE_LOCATION_PATTERN = "META-INF/%s/git.properties";

    @ReadOperation
    public Properties infos(@Selector(match = Selector.Match.ALL_REMAINING) String artifactId) throws IOException {
        AssertUtils.hasText(artifactId, "path variable artifactId must not empty");
        return PropertiesLoaderUtils.loadAllProperties(String.format(GIT_INFOS_RESOURCE_LOCATION_PATTERN, artifactId), null);
    }
}
