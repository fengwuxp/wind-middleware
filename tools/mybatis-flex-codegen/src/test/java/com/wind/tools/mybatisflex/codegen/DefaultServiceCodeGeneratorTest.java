package com.wind.tools.mybatisflex.codegen;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

class DefaultServiceCodeGeneratorTest {

    @Test
    void testGen() {
        String[] outPaths = {
                // main 方法下为项目根路径
                System.getProperty("user.dir"),
                "target",
                "codegen-result"
        };
        String outBasePath = getPath(outPaths);
        CodegenConfiguration configuration = CodegenConfiguration
                .builder()
                .basePackage("com.wind.example")
                .outDir(outBasePath)
                .requestBaseMapping("ExampleConstants.API_V1 + \"/examples\"")
                .build();
        new DefaultServiceCodeGenerator(configuration).gen(ExampleUser.class, "用户");
        Assertions.assertTrue(new File(outBasePath).exists());
    }

    private static String getPath(String[] paths) {
        return Paths.get(String.join(File.separator, paths)).toString();
    }

    /**
     * 示例用户
     * 这是一条测试注释1
     * 这是一条测试注释2
     * 这是一条测试注释3
     *
     * @author wuxp
     * @date 2023-10-06 16:37
     **/
    @Data
    public static class ExampleUser {

        /**
         * ID
         * 测试注释 1
         */
        private Long id;

        /**
         * 用户名
         */
        @NotEmpty
        @Size(min = 2, max = 32)
        private String name;

        /**
         * 年龄
         */
        private Integer age;

        @NotNull
        private Boolean enabled;

        @NotNull
        private Boolean deleted;

        @NotNull
        private Integer version;

        private List<String> tags;

        private LocalDateTime gmtCreate;

        private LocalDateTime gmtModified;
    }
}