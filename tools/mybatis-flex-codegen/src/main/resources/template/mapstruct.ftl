package ${basePackage}.services.mapstruct;

import ${basePackage}.dal.entities.${name};
import ${basePackage}.services.model.dto.${name}DTO;
import ${basePackage}.services.model.request.Create${name}Request;
import ${basePackage}.services.model.request.Update${name}Request;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
* ${comment} Converter
*
* @author ${author}
* @since ${.now?string("yyyy-MM-dd")}
*/
@Mapper
public interface ${javaClassName} {

${name}Converter INSTANCE = Mappers.getMapper(${name}Converter.class);

/**
* 创建请求 convert to ${comment}实体
*
* @param request 创建请求
* @return ${name} 实例
*/
${name} convertTo${name}(Create${name}Request request);

/**
* 更新请求 convert to ${comment}实体
*
* @param request 更新请求
* @return ${name} 实例
*/
${name} convertTo${name}(Update${name}Request request);


/**
* ${name} convert to ${name}DTO
*
* @param data ${name} 实例
* @return  ${name}DTO 实例
*/
${name}DTO convertTo${name}DTO(${name} data);
}
