package ${basePackage}.services;

import ${basePackage}.model.dto.${name}DTO;
import ${basePackage}.model.query.${name}Query;
import ${basePackage}.model.request.Create${name}Request;
import ${basePackage}.model.request.Update${name}Request;
import com.wind.common.query.supports.Pagination;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
* ${comment}服务
*
* @author ${author}
* @since ${.now?string("yyyy-MM-dd")}
*/
public interface ${javaClassName} {

/**
* 创建 ${comment}
*
* @param req 创建请求对象
* @return ${comment} ID
*/
Long create${name}(Create${name}Request req);

/**
* 更新 ${comment}
*
* @param req 更新请求对象
*/
void update${name}(Update${name}Request req);

/**
* 删除${comment}
*
* @param id ${comment} id
*/
default void delete${name}ById(@NotNull Long id){
delete${name}ByIds(id);
}

/**
* 批量删除${comment}
*
* @param ids ${comment} id
*/
void delete${name}ByIds(@NotEmpty Long... ids);

/**
* 根据 id 查询${comment}
*
* @param id ${comment} id
* @return ${name}
*/
${name}DTO query${name}ById(@NotNull Long id);

/**
* 分页查询 ${comment}
*
* @param query 查询条件
* @return ${name} 分页对象
*/
Pagination
<${name}DTO> query${name}s(${name}Query query);

    }