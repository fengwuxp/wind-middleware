package com.wind.tools.mybatisflex.codegen;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.wind.common.WindConstants;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.tools.mybatisflex.codegen.model.CodegenOutPutType;
import com.wind.tools.mybatisflex.codegen.model.GenCodeInfo;
import com.wind.tools.mybatisflex.codegen.parser.JavaEntityParser;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author wuxp
 * @date 2023-10-06 16:36
 **/
public class DefaultServiceCodeGenerator {

    private static final Set<String> CREATE_REQUEST_IGNORE_FIELDS = ImmutableSet.of("id", "gmtCreate", "gmtModified");

    private static final Set<String> UPDATE_REQUEST_IGNORE_FIELDS = ImmutableSet.of("gmtCreate", "gmtModified");

    static final JavaEntityParser PARSER = new JavaEntityParser();

    private final CodegenConfiguration configuration;


    public DefaultServiceCodeGenerator(CodegenConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * 生成代码
     *
     * @param entityClass 实体类
     */
    public void gen(Class<?> entityClass) {
        gen(entityClass, WindConstants.EMPTY);
    }

    /**
     * 生成代码
     *
     * @param entityClass 实体类
     * @param entityDesc  实体类说明
     */
    public void gen(Class<?> entityClass, String entityDesc) {
        GenCodeInfo genCodeInfo = PARSER.parse(entityClass, entityDesc);
        try {
            genModel(genCodeInfo);
            genMapper(genCodeInfo);
            genConverter(genCodeInfo);
            genService(genCodeInfo);
            genController(genCodeInfo);
        } catch (Exception exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "生成失败", exception);
        }
    }


    private void genModel(GenCodeInfo entity) throws TemplateException, IOException {
        String templateName = "dto.ftl";
        render(templateName, String.format("%s/services/model/dto", configuration.getOutDir()),
                entity.reJavaClassName("%sDTO").setCodegenOutPutType(CodegenOutPutType.DTO));
        render(templateName, String.format("%s/services/model/request", configuration.getOutDir()),
                rebuildEntityByCreateRequest(entity.reJavaClassName("Create%sRequest").setCodegenOutPutType(CodegenOutPutType.REQUEST)));
        render(templateName, String.format("%s/services/model/request", configuration.getOutDir()),
                rebuildEntityByUpdateRequest(entity.reJavaClassName("Update%sRequest").setCodegenOutPutType(CodegenOutPutType.REQUEST)));
        render(templateName, String.format("%s/services/model/query", configuration.getOutDir()), rebuildEntityByQuery(entity.reJavaClassName(
                "%sQuery").setCodegenOutPutType(CodegenOutPutType.QUERY)));
    }

    private void genMapper(GenCodeInfo entity) throws TemplateException, IOException {
        render("mapper.ftl", String.format("%s/dal/mapper", configuration.getOutDir()),
                entity.reJavaClassName("%sMapper").setCodegenOutPutType(CodegenOutPutType.MAPPER));
    }

    private void genConverter(GenCodeInfo entity) throws TemplateException, IOException {
        render("mapstruct.ftl", String.format("%s/services/mapstruct", configuration.getOutDir()),
                entity.reJavaClassName("%sConverter").setCodegenOutPutType(CodegenOutPutType.CONVERTER));
    }

    private void genService(GenCodeInfo entity) throws TemplateException, IOException {
        render("service.ftl", String.format("%s/services/", configuration.getOutDir()),
                entity.reJavaClassName("%sService").setCodegenOutPutType(CodegenOutPutType.SERVICE));
        GenCodeInfo serviceImpl = entity.reJavaClassName("%sServiceImpl").setCodegenOutPutType(CodegenOutPutType.SERVICE_IMPL);
        serviceImpl.setExtraProps(ImmutableMap.of("query",
                rebuildEntityByQuery(entity.reJavaClassName("%sQuery").setCodegenOutPutType(CodegenOutPutType.QUERY))));
        render("service_impl.ftl", String.format("%s/services/impl", configuration.getOutDir()), serviceImpl);
    }

    private void genController(GenCodeInfo entity) throws TemplateException, IOException {
        render("controller.ftl", String.format("%s/controller", configuration.getOutDir()),
                entity.reJavaClassName("%sController").setCodegenOutPutType(CodegenOutPutType.CONTROLLER));
    }

    private void render(String templateName, String dir, GenCodeInfo entity) throws IOException, TemplateException {
        CodegenFileUtils.createDirectoryRecursively(normalizePath(dir));
        String fileName = String.format("%s/%s.java", dir, entity.getJavaClassName());
        Path path = Paths.get(normalizePath(fileName));
        Writer writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
        Map<String, Object> params = JSON.parseObject(JSON.toJSONString(entity), new TypeReference<Map<String, Object>>() {
        });
        params.put("outPutType", entity.getCodegenOutPutType());
        params.putAll(configuration.getConfigVariables());
        getTemplate(templateName).process(params, writer);
    }


    private Template getTemplate(String templateName) {
        //创建一个合适的Configuration对象
        Configuration config = new Configuration(Configuration.VERSION_2_3_28);
        DefaultObjectWrapper objectWrapper = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_32).build();
        config.setObjectWrapper(objectWrapper);
        //这个一定要设置，不然在生成的页面中 会乱码
        config.setDefaultEncoding(StandardCharsets.UTF_8.name());
        //支持从jar中加载模板
        config.setClassForTemplateLoading(DefaultServiceCodeGenerator.class, "/");
        try {
            // 获取页面模版。
            return config.getTemplate(MessageFormat.format("/template/{0}", templateName));
        } catch (IOException e) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "加载模板失败，templateName = " + templateName, e);
        }
    }

    /**
     * 创建请求，忽略 {@link #CREATE_REQUEST_IGNORE_FIELDS} 相关字段
     */
    private GenCodeInfo rebuildEntityByCreateRequest(GenCodeInfo entity) {
        return rebuildEntity(entity, CREATE_REQUEST_IGNORE_FIELDS);
    }

    /**
     * 创建请求，忽略 {@link #UPDATE_REQUEST_IGNORE_FIELDS} 相关字段
     */
    private GenCodeInfo rebuildEntityByUpdateRequest(GenCodeInfo entity) {
        GenCodeInfo info = rebuildEntity(entity, UPDATE_REQUEST_IGNORE_FIELDS);
        info.getFields().forEach(DefaultServiceCodeGenerator::removeNotNullionAnnotations);
        Optional<GenCodeInfo.FieldInfo> id = info.getFields().stream().filter(field -> Objects.equals("id", field.getName())).findFirst();
        if (id.isPresent()) {
            List<String> annotations = new ArrayList<>(id.get().getAnnotations());
            annotations.add(NotNull.class.getSimpleName());
            id.get().setAnnotations(annotations);
            id.get().setDependencies(Collections.singleton(NotNull.class.getName()));
        }
        return info;
    }

    private static GenCodeInfo.FieldInfo removeNotNullionAnnotations(GenCodeInfo.FieldInfo field) {
        List<String> annotations = field.getAnnotations().stream()
                // 查询对象移除验证注解
                .filter(name ->
                        !(Objects.equals(NotNull.class.getSimpleName(), name) ||
                                Objects.equals(NotBlank.class.getSimpleName(), name) ||
                                Objects.equals(NotEmpty.class.getSimpleName(), name))
                ).collect(Collectors.toList());
        Set<String> dependencies = field.getDependencies().stream()
                // 查询对象移除验证注解
                .filter(name ->
                        !(Objects.equals(NotNull.class.getName(), name) ||
                                Objects.equals(NotBlank.class.getName(), name) ||
                                Objects.equals(NotEmpty.class.getName(), name))
                ).collect(Collectors.toSet());
        return field.setAnnotations(annotations)
                .setDependencies(dependencies);
    }

    private GenCodeInfo rebuildEntity(GenCodeInfo entity, Set<String> ignoreFields) {
        List<GenCodeInfo.FieldInfo> fields = entity.getFields()
                .stream()
                .filter(field -> !ignoreFields.contains(field.getName()))
                .collect(Collectors.toList());
        return entity
                .duplicate()
                .setFields(fields);
    }

    /**
     * 查询对象，给 {@link #UPDATE_REQUEST_IGNORE_FIELDS} 字段增加范围查询支持
     */
    private GenCodeInfo rebuildEntityByQuery(GenCodeInfo entity) {
        List<GenCodeInfo.FieldInfo> dateFields = entity.getFields()
                .stream()
                .filter(field -> UPDATE_REQUEST_IGNORE_FIELDS.contains(field.getName()))
                // 创建、更新时间增加范围查询支持
                .map(field -> Arrays.asList(
                        field.rename("min" + field.getFirstUpCaseName(), "查询到最小" + field.getName()),
                        field.rename("max" + field.getFirstUpCaseName(), "查询到最大" + field.getName())
                ))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        GenCodeInfo result = rebuildEntityByCreateRequest(entity).duplicate();
        List<GenCodeInfo.FieldInfo> fields = result.getFields();
        fields.addAll(dateFields);
        fields = fields.stream()
                .map(DefaultServiceCodeGenerator::removeValidationAnnotations)
                .collect(Collectors.toList());
        result.setFields(fields);
        return result;
    }

    private static GenCodeInfo.FieldInfo removeValidationAnnotations(GenCodeInfo.FieldInfo field) {
        Set<String> dependencies = field.getDependencies().stream()
                // 查询对象移除验证注解
                .filter(name -> !name.startsWith("javax.validation.constraints"))
                .collect(Collectors.toSet());
        return field.setAnnotations(Collections.emptyList())
                .setDependencies(dependencies);
    }


    private String normalizePath(String path) {
        return path.replace("\\/", File.separator);
    }
}
