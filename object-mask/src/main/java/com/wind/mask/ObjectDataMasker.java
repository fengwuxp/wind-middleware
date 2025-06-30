package com.wind.mask;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.util.WindDeepCopyUtils;
import com.wind.common.util.WindReflectUtils;
import com.wind.mask.annotation.Sensitive;
import lombok.AllArgsConstructor;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 数据对象脱敏器，支持通过注解 {@link Sensitive} 或 手动注册 {@link MaskRuleRegistry#registerRule(MaskRuleGroup)}} 脱敏规则的方式
 * 如果希望脱敏时不影响原有对象，请使用 {@link #ofDeepCopy(MaskRuleRegistry)} 创建实例
 *
 * @author wuxp
 * @date 2024-08-08 09:20
 **/
@AllArgsConstructor
public class ObjectDataMasker implements WindMasker<Object, Object> {

    private final MaskRuleRegistry registry;

    private Function<Object, Object> objectCopyer;

    public ObjectDataMasker(MaskRuleRegistry registry) {
        this(registry, o -> o);
    }

    public static ObjectDataMasker ofDeepCopy(MaskRuleRegistry registry) {
        return new ObjectDataMasker(registry, WindDeepCopyUtils::copy);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T maskAs(Object target) {
        return (T) mask(target);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Object mask(Object target) {
        if (target == null) {
            return null;
        }
        Class<?> clazz = target.getClass();
        if (clazz.isArray()) {
            //  数组
            return ClassUtils.isPrimitiveArray(clazz) ? target : maskArray((Object[]) target);
        } else if (ClassUtils.isAssignable(Collection.class, clazz)) {
            // 集合
            return maskCollection((Collection<Object>) target);
        } else if (target instanceof Map) {
            if (registry.requireMask(Map.class)) {
                // Map
                return maskMap((Map<Object, Object>) objectCopyer.apply(target));
            }
        } else if (registry.requireMask(clazz)) {
            return maskObject(objectCopyer.apply(target));
        }
        return target;
    }

    private Object maskObject(Object object) {
        Class<?> clazz = object.getClass();
        if (registry.requireMask(clazz)) {
            for (MaskRule rule : registry.getRuleGroup(clazz).getRules()) {
                try {
                    maskObjectField(rule, object);
                } catch (Exception exception) {
                    throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "object mask error", exception);
                }
            }
        }
        return object;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void maskObjectField(MaskRule rule, Object val) throws Exception {
        Field field = WindReflectUtils.findField(val.getClass(), rule.getName());
        Object o = field.get(val);
        if (o == null) {
            return;
        }
        WindMasker masker = rule.getMasker();
        if (masker instanceof ObjectMasker objectMasker) {
            field.set(val, objectMasker.mask(o, rule.getKeys()));
        } else {
            field.set(val, masker.mask(o));
        }
    }

    private Object[] maskArray(Object[] array) {
        Class<?> componentType = array.getClass().getComponentType();
        if (registry.requireMask(componentType)) {
            Object[] result = (Object[]) objectCopyer.apply(array);
            for (Object v : result) {
                this.maskObject(v);
            }
            return result;
        }
        return array;
    }

    @SuppressWarnings({"unchecked"})
    private Collection<Object> maskCollection(Collection<Object> objects) {
        if (objects.isEmpty()) {
            return objects;
        }
        Object first = objects.iterator().next();
        if (first == null) {
            // TODO 待优化
            return objects;
        }
        Class<?> elementType = first.getClass();
        if (registry.requireMask(elementType)) {
            Collection<Object> result = (Collection<Object>) objectCopyer.apply(objects);
            result.forEach(this::maskObject);
            return result;
        }
        return objects;
    }

    @SuppressWarnings({"unchecked"})
    private Map<Object, Object> maskMap(Map<Object, Object> map) {
        if (map instanceof HashMap || map instanceof ConcurrentMap) {
            // TODO 优化改用 json path ？
            MaskRuleGroup group = registry.getRuleGroup(Map.class);
            map.replaceAll((key, value) -> {
                if (key instanceof String && value instanceof String) {
                    MaskRule rule = group.matchesWithKey((String) key);
                    return rule == null ? value : rule.getMasker().mask(value);
                }
                return maskAs(value);
            });
            return map;
        }
        return map;
    }
}
