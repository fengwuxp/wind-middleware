package com.wind.common.query.cursor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * @author wuxp
 * @date 2025-10-17 17:22
 **/
class CursorQueryUtilsTests {


    @Test
    void testQueryParamValueAsText() {
        Assertions.assertEquals("1,2,3", CursorQueryUtils.queryParamValueAsText(new String[]{"1", "2", "3"}));
        Assertions.assertEquals("1,2,3", CursorQueryUtils.queryParamValueAsText(new int[]{1, 2, 3}));
        Assertions.assertEquals("1.0,2.0,3.0", CursorQueryUtils.queryParamValueAsText(new double[]{1.0d, 2, 3}));
        Assertions.assertEquals("A,B", CursorQueryUtils.queryParamValueAsText(new Example[]{Example.A, Example.B}));
        Assertions.assertEquals("1,2,3", CursorQueryUtils.queryParamValueAsText(Arrays.asList("1", 2, "3")));
    }

    enum Example {
        A,
        B
    }
}
