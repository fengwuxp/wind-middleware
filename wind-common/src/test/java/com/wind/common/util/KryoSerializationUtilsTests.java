package com.wind.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author wuxp
 * @date 2025-10-20 09:40
 **/
class KryoSerializationUtilsTests {

    private final KryoSerializationUtils codec = KryoSerializationUtils.getInstance();

    @Test
    @DisplayName("序列化与反序列化基本类型")
    void testSimpleObject() {
        String text = "hello kryo";
        byte[] bytes = codec.encode(text);
        Object decoded = codec.decode(bytes);
        Assertions.assertEquals(text, decoded);
    }

    @Test
    @DisplayName("序列化与反序列化复杂对象")
    void testComplexObject() {
        User user = new User("u_001", "Alice", 28, List.of("CN", "US"));
        byte[] bytes = codec.encode(user);
        User result = (User) codec.decode(bytes);

        Assertions.assertEquals(user.getId(), result.getId());
        Assertions.assertEquals(user.getTags(), result.getTags());
        Assertions.assertEquals(user.getName(), result.getName());
    }

    @Test
    @DisplayName("序列化与反序列化 Map 集合")
    void testMapSerialization() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 123);
        data.put("price", 9.99);
        data.put("tags", List.of("A", "B", "C"));

        byte[] bytes = codec.encode(data);
        Map<?, ?> decoded = (Map<?, ?>) codec.decode(bytes);

        Assertions.assertEquals(data.get("id"), decoded.get("id"));
        Assertions.assertEquals(data.get("tags"), decoded.get("tags"));
    }

    @Test
    @DisplayName("Base64 编码与解码")
    void testBase64EncodeDecode() {
        User user = new User("u_002", "Bob", 35, List.of("JP"));
        String encoded = codec.encodeToString(user);
        Object decoded = codec.decode(encoded);

        Assertions.assertInstanceOf(User.class, decoded);
        User u = codec.decodeAs(encoded);
        Assertions.assertEquals("Bob", u.getName());
    }

    @Test
    @DisplayName("循环引用对象验证（防止 StackOverflow）")
    void testCircularReference() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a; // 循环引用

        byte[] bytes = codec.encode(a);
        Node decoded = (Node) codec.decode(bytes);

        Assertions.assertEquals("A", decoded.name);
        Assertions.assertNotNull(decoded.next);
        Assertions.assertEquals("B", decoded.next.name);
    }

    @Test
    @DisplayName("并发序列化反序列化性能与一致性验证")
    void testConcurrentEncodeDecode() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                int index = i;
                tasks.add(() -> {
                    User user = new User("U" + index, "Name" + index, index, List.of("X", "Y"));
                    String encoded = codec.encodeToString(user);
                    User decoded = (User) codec.decode(encoded);
                    return user.equals(decoded);
                });
            }

            List<Future<Boolean>> results = executor.invokeAll(tasks);
            executor.shutdown();
            assertTrue(results.stream().allMatch(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
    }

    // ======== 测试用模型类 ========

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private int age;
        private List<String> tags;

    }

    @Data
    @AllArgsConstructor
    public static class Node implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String name;
        private Node next;

        public Node(String name) {
            this.name = name;
        }
    }

}
