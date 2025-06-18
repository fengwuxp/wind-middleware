package ${basePackage}.services.impl;

import com.wind.mybatis.flex.MybatisQueryHelper;
import ${basePackage}.dal.entities.table.${name}NameRefs;
import ${basePackage}.dal.entities.${name};
import ${basePackage}.dal.mapper.${name}Mapper;
import ${basePackage}.services.${name}Service;
import ${basePackage}.mapstruct.${name}Converter;
import ${basePackage}.model.dto.${name}DTO;
import ${basePackage}.model.query.${name}Query;
import ${basePackage}.model.request.Create${name}Request;
import ${basePackage}.model.request.Update${name}Request;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.query.supports.Pagination;

import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.Pagination;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;

/**
* ${comment}服务实现类
*
* @author ${author}
* @since ${.now?string("yyyy-MM-dd")}
*/
@Service
@Slf4j
@AllArgsConstructor
public class ${javaClassName} implements ${name}Service {

private final ${name}Mapper ${firstLowName}Mapper;

@Override
public Long create${name}(Create${name}Request request){
${name} entity = ${name}Converter.INSTANCE.convertTo${name}(request);
${firstLowName}Mapper.insertSelective(entity);
AssertUtils.notNull(entity.getId(), "创建${comment}失败");
return entity.getId();
}


@Override
public void update${name}(Update${name}Request request){
${name} entity = find${name}(request.getId());
<#if existsVersionField>
    if (request.getVersion() == null) {
    request.setVersion(entity.getVersion());
    }
</#if>
entity = ${name}Converter.INSTANCE.convertTo${name}(request);
AssertUtils.isTrue( ${firstLowName}Mapper.update(entity) == 1, "更新${comment}信息失败");
}


@Override
public void delete${name}ByIds(@NotEmpty Long... ids){
AssertUtils.notEmpty(ids, "argument ids must not empty");
<#if useLogicDeleted>
    QueryWrapper wrapper = QueryWrapper.create().where(${name}NameRefs.${firstLowName}.id.in(ids));
    ${name} entity = new ${name}();
    entity.setDeleted(true);
    AssertUtils.isTrue(${firstLowName}Mapper.updateByQuery(entity, true, wrapper) == ids.length, "删除${comment}失败");
<#else >
    int total = ${firstLowName}Mapper.deleteBatchByIds(Arrays.asList(ids));
    AssertUtils.isTrue(total == ids.length, "删除${comment}失败");
</#if>
}

@Override
public ${name}DTO query${name}ById(@NotNull Long id){
return ${name}Converter.INSTANCE.convertTo${name}DTO(find${name}(id));
}

@Override
public Pagination
<${name}DTO> query${name}s(${name}Query query){
    ${name}NameRefs ${firstLowName} = ${name}NameRefs.${firstLowName};
    QueryWrapper queryWrapper = MybatisQueryHelper.from(query).select()
    .from(${firstLowName})
    <#if extraProps.query??>
    <#--字段-->
        <#list extraProps.query.fields as f >
            <#if f_index==0>
                <#if f.type=='String'>
                    .where(${firstLowName}.${f.name}.eq(query.get${f.firstUpCaseName}()))
                <#elseif f.type=='Date' >
                    .where(${firstLowName}.${f.name}<#if f.name?starts_with('min')>.ge<#else >.le</#if>(query.get${f.firstUpCaseName}()))<#if !f_has_next>;</#if>
                <#else >
                    .where(${firstLowName}.${f.name}.eq(query.get${f.firstUpCaseName}()))
                </#if>
            <#else >
                <#if f.type=='String'>
                    .and(${firstLowName}.${f.name}.eq(query.get${f.firstUpCaseName}()))<#if !f_has_next>;</#if>
                <#elseif f.type=='Date' || f.type=='LocalDateTime' >
                    <#if f.name?starts_with('min')>
                        <#assign fieldName=f.name?replace("min","")/>
                        <#assign op="ge"/>
                    <#else >
                        <#assign fieldName=f.name?replace("max","")/>
                        <#assign op="le"/>
                    </#if>
                    .and(${firstLowName}.${fieldName?uncap_first}.${op}(query.get${f.name?cap_first}()))<#if !f_has_next>;</#if>
                <#else >
                    .and(${firstLowName}.${f.name}.eq(query.get${f.firstUpCaseName}()))<#if !f_has_next>;</#if>
                </#if>
            </#if>
        </#list>
    </#if>

    Page<${name}> result = ${firstLowName}Mapper.paginate(MybatisQueryHelper.of(query), queryWrapper);
    return MybatisQueryHelper.convert(result, query,${name}Converter.INSTANCE::convertTo${name}DTO);
    }


    private ${name} find${name}(Long id) {
    ${name} result = ${firstLowName}Mapper.selectOneById(id);
    AssertUtils.notNull(result, "${comment}不存在");
    <#if useLogicDeleted>
        AssertUtils.isFalse(result.getDeleted(), "${comment}不存在或已删除");
    </#if>
    return result;
    }
    }