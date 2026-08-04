package com.wind.common.config;

import com.wind.common.WindConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author wuxp
 * @date 2025-09-24 13:35
 **/
class WindSystemConfigRepositoryTests {

    private final WindSystemConfigRepository repository = new WindSystemConfigRepository(testStorage());

    @Test
    void testRequireConfig() {
        String result = repository.requireConfig("test", String.class, WindConstants.EMPTY);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(WindConstants.EMPTY, result);
    }

    @Test
    void testGetJsonConfig() {
        Map<String, Object> result = repository.getJsonConfig("EXAMPLE_JSON");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("1", result.get("a"));
    }

    private static SystemConfigStorage testStorage() {
        return new SystemConfigStorage() {
            @Override
            public void saveConfig(String name, String group, String value) {
            }

            @Override
            public String getConfig(String name) {
                return "EXAMPLE_JSON".equals(name) ? "{\"a\":\"1\"}" : null;
            }
        };
    }
}
