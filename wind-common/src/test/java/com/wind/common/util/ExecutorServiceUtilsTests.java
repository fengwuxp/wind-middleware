package com.wind.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author wuxp
 * @date 2025-06-30 09:16
 **/
class ExecutorServiceUtilsTests {


    @Test
    void testVirtual() throws Exception {
        try (ExecutorService executor = ExecutorServiceUtils.virtual("example-")) {
            Future<?> future = executor.submit((Callable<Object>) () -> 1);
            Assertions.assertEquals(1, future.get());
        }
    }
}
