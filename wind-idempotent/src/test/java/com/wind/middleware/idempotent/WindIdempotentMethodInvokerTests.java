package com.wind.middleware.idempotent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author wuxp
 * @date 2025-10-13 14:06
 **/
class WindIdempotentMethodInvokerTests {

    @BeforeEach
    void setup(){
        WindIdempotentExecuteUtilsTests.init();
    }

    @AfterEach
    void after(){
        WindIdempotentExecuteUtilsTests.clear();
    }

    // ========== 1 参数 ==========
    @Test
    void test1ParamReturn() {
        String result = WindIdempotentMethodInvoker.key("test1")
                .wrapper(ExampleService::getName)
                .invoke("zhans");
        Assertions.assertEquals("hello world", result);
    }

    @Test
    void test1ParamVoid() {
        WindIdempotentMethodInvoker.key(() -> "test2")
                .wrapper(ExampleService::sayHello)
                .invoke("hello void 1");
    }

    // ========== 2 参数 ==========
    @Test
    void test2ParamReturn() {
        String result = WindIdempotentMethodInvoker.key("test3")
                .wrapper(ExampleService::m2)
                .invoke("zhans", 2);
        Assertions.assertEquals("hello world, 2", result);
    }

    @Test
    void test2ParamVoid() {
        WindIdempotentMethodInvoker.key("test4")
                .wrapper(ExampleService::sayHello2)
                .invoke("hello void 2", 123);
    }

    // ========== 3 参数 ==========
    @Test
    void test3ParamReturn() {
        String result = WindIdempotentMethodInvoker.key("test5")
                .wrapper(ExampleService::m3)
                .invoke("zhans", 2, 3);
        Assertions.assertEquals("3 params", result);
    }

    @Test
    void test3ParamVoid() {
        WindIdempotentMethodInvoker.key("test6")
                .wrapper(ExampleService::sayHello3)
                .invoke("hello void 3", 1, 2);
    }

    // ========== 4 参数 ==========
    @Test
    void test4ParamReturn() {
        String result = WindIdempotentMethodInvoker.key("test7")
                .wrapper(ExampleService::m4)
                .invoke("a", "b", "c", "d");
        Assertions.assertEquals("4 params", result);
    }

    @Test
    void test4ParamVoid() {
        WindIdempotentMethodInvoker.key("test8")
                .wrapper(ExampleService::sayHello4)
                .invoke("x", "y", "z", "w");
    }

    // ========== 5 参数 ==========
    @Test
    void test5ParamReturn() {
        String result = WindIdempotentMethodInvoker.key("test9")
                .wrapper(ExampleService::m5)
                .invoke(1, 2, 3, 4, 5);
        Assertions.assertEquals("5 params", result);
    }

    @Test
    void test5ParamVoid() {
        WindIdempotentMethodInvoker.key("test10")
                .wrapper(ExampleService::sayHello5)
                .invoke(1, 2, 3, 4, 5);
    }

    // ========== 6 参数 ==========
    @Test
    void test6ParamReturn() {
        String result = WindIdempotentMethodInvoker.key("test11")
                .wrapper(ExampleService::m6)
                .invoke(1, 2, 3, 4, 5, 6);
        Assertions.assertEquals("6 params", result);
    }

    @Test
    void test6ParamVoid() {
        WindIdempotentMethodInvoker.key("test12")
                .wrapper(ExampleService::sayHello6)
                .invoke(1, 2, 3, 4, 5, 6);
    }

    // ================= Example Service =================
    static class ExampleService {

         static String getName(String name) {
            return "hello world";
        }

         static void sayHello(String name) {
            System.out.println("sayHello: " + name);
        }

         static String m2(String name, int age) {
            return "hello world, " + age;
        }

         static void sayHello2(String name, int age) {
            System.out.println("sayHello2: " + name + ", " + age);
        }

         static String m3(String a, int b, int c) {
            return "3 params";
        }

         static void sayHello3(String a, int b, int c) {
            System.out.println("sayHello3: " + a + ", " + b + ", " + c);
        }

         static String m4(String a, String b, String c, String d) {
            return "4 params";
        }

         static void sayHello4(String a, String b, String c, String d) {
            System.out.println("sayHello4: " + a + ", " + b + ", " + c + ", " + d);
        }

         static String m5(int a, int b, int c, int d, int e) {
            return "5 params";
        }

         static void sayHello5(int a, int b, int c, int d, int e) {
            System.out.println("sayHello5: " + a + ", " + b + ", " + c + ", " + d + ", " + e);
        }

         static String m6(int a, int b, int c, int d, int e, int f) {
            return "6 params";
        }

         static void sayHello6(int a, int b, int c, int d, int e, int f) {
            System.out.println("sayHello6: " + a + ", " + b + ", " + c + ", " + d + ", " + e + ", " + f);
        }
    }
}
