package com.wind.mask;

import com.wind.common.exception.AssertUtils;
import com.wind.common.util.WindReflectUtils;
import com.wind.mask.annotation.Sensitive;
import org.springframework.util.ObjectUtils;

import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脱敏规则注册器
 *
 * @author wuxp
 * @date 2024-03-11 13:30
 **/
public class MaskRuleRegistry {

    private final Map<Class<?>, MaskRuleGroup> groups;

    public MaskRuleRegistry() {
        this(Collections.emptyList());
    }

    public MaskRuleRegistry(Collection<MaskRuleGroup> groups) {
        AssertUtils.notNull(groups, "ruleGroups must not null");
        this.groups = new ConcurrentHashMap<>(groups.size());
        groups.forEach(this::registerRule);
    }

    /**
     * 是否需要脱敏
     *
     * @param clazz 类类型
     * @return true 需要
     */
    public boolean requireMask(Class<?> clazz) {
        return !ObjectUtils.isEmpty(getRuleGroup(clazz)) ||
                (clazz.isAnnotationPresent(Sensitive.class) && WindReflectUtils.findFields(clazz, Sensitive.class).length > 0);
    }

    @NotNull
    public MaskRuleGroup getRuleGroup(Class<?> target) {
        AssertUtils.notNull(target, "get sensitive rule class not null");
        return groups.computeIfAbsent(target, key -> this.buildRuleGroup(target));
    }

    public void registerRule(MaskRuleGroup group) {
        this.groups.put(group.getTarget(), group);
    }

    public void registerRules(@NotNull Collection<MaskRuleGroup> groups) {
        groups.forEach(this::registerRule);
    }

    public void clearRules(@NotNull Class<?> clazz) {
        groups.remove(clazz);
    }

    public void clearRules() {
        groups.clear();
    }

    private MaskRuleGroup buildRuleGroup(Class<?> clazz) {
        return MaskRuleGroup.builder().form(clazz).last();
    }

}
