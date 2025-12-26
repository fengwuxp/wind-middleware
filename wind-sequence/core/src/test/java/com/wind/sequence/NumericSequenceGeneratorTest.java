package com.wind.sequence;

import com.wind.common.exception.BaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

class NumericSequenceGeneratorTest {

    @Test
    void testNext1() {

        NumericSequenceGenerator generator = new NumericSequenceGenerator(createCounter(0), 4);
        Assertions.assertEquals(String.format("%04d", 1L), generator.next());
    }

    @Test
    void testNext2() {
        NumericSequenceGenerator generator = new NumericSequenceGenerator(createCounter(1000), 4);
        Assertions.assertEquals("1001", generator.next());
        Assertions.assertEquals("1002", generator.next());
        Assertions.assertEquals("1003", generator.next());
    }

    @Test
    void testNextError() {
        NumericSequenceGenerator generator = new NumericSequenceGenerator(createCounter(10000), 4);
        BaseException exception = Assertions.assertThrows(BaseException.class, generator::next);
        Assertions.assertEquals("sequence exceeds maximum length", exception.getMessage());
    }

    private final Supplier<Long> createCounter(int value) {
        return new Supplier<>() {

            private final AtomicLong counter = new AtomicLong(value);

            @Override
            public Long get() {
                return counter.incrementAndGet();
            }
        };
    }
}