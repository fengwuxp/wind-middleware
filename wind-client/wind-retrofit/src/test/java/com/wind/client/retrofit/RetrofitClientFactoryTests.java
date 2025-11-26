package com.wind.client.retrofit;

import com.wind.api.core.signature.ApiSecretAccount;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author wuxp
 * @date 2024-05-07 09:53
 **/
class RetrofitClientFactoryTests {

    @Test
    void testBuild() {
        RetrofitClientFactory factory = RetrofitClientFactory.builder()
                .baseUrl("https://wind.example.com")
                .authenticationHeaderPrefix("Wind")
                .account(ApiSecretAccount.sha256WithRsa("example", "test"))
                .restful();
        Assertions.assertNotNull(factory);
    }

    @Test
    void testJacksonLocalDateTime() {
        JsonMapper jsonMapper = RetrofitClientFactory.RetrofitClientFactoryBuilder.buildJsonMapper();
        String text = jsonMapper.writer().writeValueAsString(new LocalDateTimeObject());
        Assertions.assertNotNull(text);
    }

    @Data
    public static class LocalDateTimeObject {

        private LocalDateTime now = LocalDateTime.now();

        private LocalDate today = LocalDate.now();

        private LocalTime nowTime = LocalTime.now();
    }
}
