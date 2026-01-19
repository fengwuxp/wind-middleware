package com.wind.core.util;


import com.wind.core.resources.TreeResources;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author wuxp
 * @date 2024-12-12 23:46
 **/
class WindResourcesUtilsTests {

    @Test
    void testConvertForTree() {
        ExampleResources root1 = new ExampleResources(1L, "root1", null, null, 0, null, null);
        ExampleResources root2 = new ExampleResources(2L, "root2", null, null, 0, null, null);
        List<ExampleResources> result = WindResourcesUtils.convertForTree(Arrays.asList(root1, root2, new ExampleResources(22L, "c2", 2L, null, 1, null,
                        null)),
                ExampleResources::setChildren);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(1, result.get(1).getChildren().size());
        Assertions.assertEquals("root2", result.get(1).getChildren().getFirst().getParentName());
    }


    @Data
    @AllArgsConstructor
    static class ExampleResources implements TreeResources<Long> {

        private Long id;

        private String name;

        private Long parentId;

        private String parentName;

        private Integer level;

        private String content;

        private List<ExampleResources> children;


    }
}
