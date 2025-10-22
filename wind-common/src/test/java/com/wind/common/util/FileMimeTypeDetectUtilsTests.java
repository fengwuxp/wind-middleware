package com.wind.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.MimeType;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author wuxp
 * @date 2025-10-22 14:06
 **/
class FileMimeTypeDetectUtilsTests {


    @Test
    void testDetectPom() {
        Path path = Paths.get(System.getProperty("user.dir"), "pom.xml");
        String mimeType = FileMimeTypeDetectUtils.detect(path);
        Assertions.assertEquals("application/xml", mimeType);
    }
}
