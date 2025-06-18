package com.wind.mask;

import com.wind.common.util.WindReflectUtils;
import com.wind.mask.annotation.Sensitive;
import com.wind.mask.masker.MaskerFactory;
import lombok.Data;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 脱敏规则组
 *
 * @author wuxp
 * @date 2024-03-11 13:30
 * @see MaskRule
 **/
@Data
public final class MaskRuleGroup {


    /**
     * 脱敏的目标类类型
     */
    @NotNull
    private final Class<?> target;

    /**
     * 字段的脱敏规则
     */
    private final Set<MaskRule> rules;

    public MaskRuleGroup(Class<?> target, Collection<MaskRule> rules) {
        this.target = target;
        this.rules = new LinkedHashSet<>(rules);
    }

    @Nullable
    public MaskRule matchesWithName(String fieldName) {
        if (fieldName == null && rules.isEmpty()) {
            return null;
        }
        return rules.stream()
                .filter(rule -> rule.eq(fieldName))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public MaskRule matchesWithKey(String key) {
        if (key == null && rules.isEmpty()) {
            return null;
        }
        return rules.stream()
                .filter(rule -> rule.matches(key))
                .findFirst()
                .orElse(null);
    }


    /**
     * 将 {@link Map} 类型字段的 {@link MaskRule} 转为 {@link  MaskRuleGroup}
     *
     * @param rule Map 类型字段的规则
     */
    public static MaskRuleGroup convertMapRules(MaskRule rule) {
        List<MaskRule> rules = rule.getKeys().stream()
                .map(key -> MaskRule.mark(key, rule.getMasker()))
                .collect(Collectors.toList());
        return new MaskRuleGroup(Map.class, rules);
    }


    public static GroupBuilder builder() {
        return new GroupBuilder(Collections.emptyList());
    }

    /**
     * {@link MaskRuleGroup} 建造器
     */
    public static final class GroupBuilder {

        private final List<MaskRuleGroup> groups;

        public GroupBuilder(List<MaskRuleGroup> groups) {
            this.groups = new ArrayList<>(groups);
        }

        public RuleBuilder form(Class<?> target) {
            return new RuleBuilder(this, target);
        }
    }

    /**
     * {@link MaskRule} 建造器
     *
     * @see #ofMaskerType(String, Class, String...)
     */
    public static final class RuleBuilder {

        private final GroupBuilder builder;

        private final Class<?> clazz;

        private final List<MaskRule> fieldRules;

        public RuleBuilder(GroupBuilder builder, Class<?> clazz) {
            this.builder = builder;
            this.clazz = clazz;
            this.fieldRules = new ArrayList<>();
        }

        public RuleBuilder ofMaskerType(String name, Class<? extends WindMasker<?, ?>> maskerType, String... keys) {
            addRules(new String[]{name}, fieldName -> MaskRule.mark(fieldName, MaskerFactory.getMasker(maskerType), keys));
            return this;
        }

        /**
         * @param name   脱敏字段名称
         * @param masker 脱敏器类类型
         * @param keys   keys
         * @return RuleBuilder
         */
        public RuleBuilder of(String name, WindMasker<?, ?> masker, String... keys) {
            addRules(new String[]{name}, fieldName -> MaskRule.mark(fieldName, masker, keys));
            return this;
        }

        public RuleBuilder ofMaskerType(Class<? extends WindMasker<?, ?>> maskerType, String... names) {
            addRules(names, fieldName -> MaskRule.mark(fieldName, MaskerFactory.getMasker(maskerType)));
            return this;
        }

        /**
         * 使用自定义的 {@link WindMasker} 实现
         *
         * @param masker 脱敏器实例
         * @param names  需要脱敏的字段名称
         * @return RuleBuilder
         */
        public RuleBuilder of(WindMasker<?, ?> masker, String... names) {
            addRules(names, fieldName -> MaskRule.mark(fieldName, masker));
            return this;
        }

        /**
         * 标记 {@link java.util.Map} 类型的字段
         *
         * @param masker     脱敏器实例
         * @param fieldNames 需要脱敏的字段名称
         * @param keys       需要脱敏的 Map keys
         * @return RuleBuilder
         */
        public RuleBuilder markMapFieldKeys(ObjectMasker<?, ?> masker, Collection<String> fieldNames, String... keys) {
            addRules(fieldNames.toArray(new String[0]), fieldName -> MaskRule.mark(fieldName, masker, keys));
            return this;
        }

        public RuleBuilder markMapFieldKeys(ObjectMasker<?, ?> masker, String fieldName, String... keys) {
            markMapFieldKeys(masker, Collections.singletonList(fieldName), keys);
            return this;
        }

        public RuleBuilder next(Class<?> target) {
            builder.groups.add(createRuleGroup());
            return new GroupBuilder(builder.groups).form(target);
        }

        public List<MaskRuleGroup> build() {
            builder.groups.add(createRuleGroup());
            return Collections.unmodifiableList(builder.groups);
        }

        public MaskRuleGroup last() {
            builder.groups.add(createRuleGroup());
            return build().get(builder.groups.size() - 1);
        }

        private void addRules(String[] names, Function<String, MaskRule> rueFactory) {
            fieldRules.addAll(Arrays.stream(names).map(rueFactory).collect(Collectors.toList()));
        }

        private MaskRuleGroup createRuleGroup() {
            fieldRules.addAll(parsesRules());
            return new MaskRuleGroup(clazz, fieldRules);
        }

        private List<MaskRule> parsesRules() {
            List<MaskRule> result = new ArrayList<>();
            Sensitive clazzAnnotation = clazz.getAnnotation(Sensitive.class);
            if (clazzAnnotation != null) {
                for (String name : clazzAnnotation.names()) {
                    result.add(MaskRule.mark(name, MaskerFactory.getMasker(clazzAnnotation.masker()), name));
                }
            }
            for (Field field : WindReflectUtils.findFields(clazz, Sensitive.class)) {
                Sensitive annotation = field.getAnnotation(Sensitive.class);
                result.add(MaskRule.mark(field.getName(), MaskerFactory.getMasker(annotation.masker()), annotation.names()));
            }
            return result;
        }

    }
}
