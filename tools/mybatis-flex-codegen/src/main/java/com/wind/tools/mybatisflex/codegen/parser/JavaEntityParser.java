package com.wind.tools.mybatisflex.codegen.parser;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.javadoc.Javadoc;
import com.wind.common.WindConstants;
import com.wind.common.util.WindReflectUtils;
import com.wind.tools.codegen.SourceCodeProvider;
import com.wind.tools.mybatisflex.codegen.model.GenCodeInfo;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author wuxp
 * @date 2023-10-06 16:47
 **/
public class JavaEntityParser {

    private final SourceCodeProvider sourceCodeProvider = new SourceCodeProvider();

    public GenCodeInfo parse(Class<?> classType, String desc) {
        GenCodeInfo result = new GenCodeInfo();
        Optional<TypeDeclaration<?>> optional = sourceCodeProvider.getTypeDeclaration(classType);
        List<GenCodeInfo.FieldInfo> fields = Arrays.stream(getClassFields(classType))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(this::buildField)
                .toList();
        result.setName(classType.getSimpleName())
                .setComment(StringUtils.hasLength(desc) || optional.isEmpty() ? desc : getCommentByTag(optional.get()))
                .setFields(fields);
        if (result.getComment() != null) {
            result.setComment(result.getComment().replace("表", ""));
        }
        return result;
    }

    private GenCodeInfo.FieldInfo buildField(Field field) {
        GenCodeInfo.FieldInfo result = new GenCodeInfo.FieldInfo();
        Optional<FieldDeclaration> optional = sourceCodeProvider.getFieldDeclaration(field);
        Class<?> fieldType = field.getType();
        result.setName(field.getName())
                .setType(optional.isPresent() ? optional.get().getElementType().asString() : fieldType.getSimpleName())
                .setClassType(fieldType)
                .setComment(optional.isPresent() ? getCommentByTag(optional.get()) : WindConstants.EMPTY);
        Set<String> dependencies = new HashSet<>();
        List<String> annotations = new ArrayList<>();
        Arrays.stream(field.getAnnotations())
                // 仅支持验证注解
                .filter(annotation -> annotation.annotationType().getName().startsWith("jakarta.validation.constraints"))
                .forEach(annotation -> {
                    if (isNoneJavaLang(annotation.annotationType())) {
                        dependencies.add(annotation.annotationType().getName());
                    }
                    annotations.add(annotation.annotationType().getSimpleName());
                });
        if (optional.isPresent()) {
            List<String> names = new ArrayList<>(annotations);
            annotations.clear();
            optional.get()
                    .getAnnotations()
                    .stream()
                    .filter(annotationExpr -> names.contains(annotationExpr.getName().asString()))
                    .forEach(annotationExpr -> {
                        String text = annotationExpr.toString();
                        // 移除 @ 符号
                        annotations.add(text.substring(1));
                    });
        }

        if (isNoneJavaLang(fieldType)) {
            dependencies.add(fieldType.getName());
        }
        return result.setDependencies(dependencies).setAnnotations(annotations);
    }

    private Field[] getClassFields(Class<?> clazz) {
        Field[] declaredFields = WindReflectUtils.getFields(clazz);
        Arrays.asList(declaredFields).forEach(ReflectionUtils::makeAccessible);
        return declaredFields;
    }

    /**
     * 通过注解标签 名称获取注释
     *
     * @param node 有携带注释的代码节点
     * @return 注释内容
     */
    private String getCommentByTag(NodeWithJavadoc<?> node) {
        Optional<Comment> optional = node.getComment();
        if (!optional.isPresent()) {
            return "";
        }
        Comment comment = optional.get();
        if (comment.isJavadocComment()) {
            Javadoc javadoc = comment.asJavadocComment().parse();
            return javadoc.getDescription().toText().replaceAll(System.lineSeparator(), "\\\\n");
        }
        return WindConstants.EMPTY;
    }

    /**
     * @param clazz 类类型
     * @return 是否 java.lang 下面的类
     */
    private boolean isNoneJavaLang(Class<?> clazz) {
        return !isJavaLang(clazz.getName());
    }

    private boolean isJavaLang(String className) {
        return className.startsWith("java.lang");
    }
}
