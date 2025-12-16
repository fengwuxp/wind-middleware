package ${basePackage}.services;

import ${basePackage}.model.dto.${name}DTO;
import ${basePackage}.model.query.${name}Query;
import ${basePackage}.model.request.Create${name}Request;
import ${basePackage}.model.request.Update${name}Request;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;

import import org.jspecify.annotations.NonNull;

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
* @param request 创建请求对象
* @return ${comment} ID
*/
@NonNull Long create${name}(@NonNull Create${name}Request request);

/**
* 更新 ${comment}
*
* @param request 更新请求对象
*/
void update${name}(@NonNull Update${name}Request request);

/**
* 删除${comment}
*
* @param id ${comment} id
*/
default void delete${name}ById(@@NonNull Long id){
delete${name}ByIds(id);
}

/**
* 批量删除${comment}
*
* @param ids ${comment} id
*/
void delete${name}ByIds(@NonNull Long... ids);

/**
* 根据 id 查询${comment}
*
* @param id ${comment} id
* @return ${name}
*/
@NonNull ${name}DTO query${name}ById(@NonNull Long id);

/**
* 分页查询 ${comment}
*
* @param query 查询条件
* @param options 查询选项
* @return ${name} 分页对象
*/
@NonNull WindPagination<${name}DTO> query${name}s(@NonNull ${name}Query query, @NonNull WindQuery<? extends QueryOrderField> options);

}