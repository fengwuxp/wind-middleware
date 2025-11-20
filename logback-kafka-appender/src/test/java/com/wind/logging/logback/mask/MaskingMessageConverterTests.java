package com.wind.logging.logback.mask;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.wind.common.exception.BaseException;
import com.wind.mask.MaskRuleGroup;
import com.wind.mask.masker.StringRangMasker;
import com.wind.trace.WindTracer;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author wuxp
 * @date 2024-08-07 15:51
 **/
@Slf4j
class MaskingMessageConverterTests {

    private static final Logger LOG = LoggerFactory.getLogger(MaskingMessageConverterTests.class);

    static {
        WindTracer.TRACER.trace();
    }

    @AfterEach
    void after() {
        WindTracer.TRACER.clear();
    }

    @Test
    void testLogin() {
        List<MaskRuleGroup> groups = MaskRuleGroup.builder().form(LogbackMaskUser.class)
                .of(LogbackMaskUser.Fields.mobilePhone, StringRangMasker.phone())
                .of(LogbackMaskUser.Fields.password, StringRangMasker.secret())
                .build();
        LogbackMaskUser user = new LogbackMaskUser();
        String mobilePhone = "18900234567";
        user.setMobilePhone(mobilePhone);
        user.setPassword("123213");
        LOG.info("test = {}", user);
        BaseException badRequest = BaseException.badRequest("Bad Request");
        LOG.error("test = {}, message  = {}", user, badRequest.getMessage(), badRequest);
        Assertions.assertEquals(mobilePhone, user.getMobilePhone());
    }

    @Test
    void testException() {
        try {
            JSONArray objects = JSON.parseArray("[" + RandomStringUtils.secure().nextAlphabetic(2000) + "]");
        } catch (Exception exception) {
            LOG.error("test info  = {},parse json error, message  = {}", RandomStringUtils.secure().nextAlphabetic(200), exception.getMessage(), exception);
        }
    }

    @Data
    @FieldNameConstants
    static class LogbackMaskUser {

        private String mobilePhone;

        private String password;
    }
}
