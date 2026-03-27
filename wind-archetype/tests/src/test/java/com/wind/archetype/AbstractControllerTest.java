package com.wind.archetype;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author wind
 **/
@ContextConfiguration(classes = {AbstractControllerTest.TestConfig.class})
@SpringJUnitConfig()
@WebAppConfiguration
@TestPropertySource(properties = "spring.cloud.nacos.config.enabled=false")
public abstract class AbstractControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    protected MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Configuration
    @SpringBootApplication(scanBasePackages = {"com.wind.example.controller.**"})
    static class TestConfig {

        public static void main(String[] args) {
            SpringApplication.run(TestConfig.class, args);
        }
    }

}
