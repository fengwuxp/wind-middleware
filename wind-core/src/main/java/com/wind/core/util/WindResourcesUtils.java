package com.wind.core.util;

import com.wind.common.exception.AssertUtils;
import com.wind.common.util.WindReflectUtils;
import com.wind.core.resources.TreeResources;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 资源工具类
 *
 * @author wuxp
 * @date 2024-12-12 23:27
 **/
@Slf4j
public final class WindResourcesUtils {

    private WindResourcesUtils() {
        throw new AssertionError();
    }

    /**
     * 将资源列表转换为树列表，注意改方法会改变传入参数集合对象中的资源
     *
     * @param resources      资源列表
     * @param childrenSetter 给资源对象设置子节点列表
     * @return 树资源列表
     */
    @NotNull
    public static <T extends TreeResources<Long>> List<T> convertForTree(Collection<T> resources, BiConsumer<T, List<T>> childrenSetter) {
        AssertUtils.notNull(childrenSetter, "argument childrenSetter must not null");
        if (CollectionUtils.isEmpty(resources)) {
            return Collections.emptyList();
        }
        // 按层级分组
        Map<Integer, List<T>> levels = resources.stream()
                .collect(Collectors.groupingBy(T::getLevel));
        // 层级顺序排序
        List<Integer> levelOrders = new ArrayList<>(levels.keySet());
        Collections.sort(levelOrders);
        // 子孙节点表
        Map<Long, List<T>> childrenMap = resources.stream()
                .filter(item -> !item.isRoot())
                .collect(Collectors.groupingBy(T::getParentId));
        // 由层级低到高设置子孙节点
        for (Integer level : levelOrders) {
            List<T> nodes = levels.get(level);
            for (T node : nodes) {
                List<T> children = childrenMap
                        .getOrDefault(node.getId(), Collections.emptyList())
                        .stream()
                        .sorted(Comparator.comparing(T::getOrderIndex))
                        .toList();
                children.forEach(item -> {
                    if (item.getParentName() == null) {
                        try {
                            WindReflectUtils.setFieldValue("parentName", item, node.getName());
                        } catch (Exception exception) {
                            log.debug("Set class type = {} parentName exception, message  = {}", item.getClass().getName(), exception.getMessage(), exception);
                        }
                    }
                });
                childrenSetter.accept(node, children);
            }
        }
        // 返回根节点列表
        int minLevel = resources.stream()
                .filter(T::isRoot)
                .mapToInt(T::getLevel)
                .findFirst()
                .orElse(0);
        return levels.getOrDefault(minLevel, Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(T::getOrderIndex))
                .toList();
    }
}
