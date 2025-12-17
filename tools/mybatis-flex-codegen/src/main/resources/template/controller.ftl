package ${basePackage}.controller;

import com.wind.common.exception.BaseException;
import com.wind.common.query.supports.AbstractPageQuery;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.script.auditlog.AuditLog;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.server.web.supports.ApiResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ${basePackage}.services.model.dto.${name}DTO;
import ${basePackage}.services.model.query.${name}Query;
import ${basePackage}.services.model.request.Create${name}Request;
import ${basePackage}.services.model.request.Update${name}Request;

/**
* ${comment}控制器
*
* @author ${author}
* @since ${.now?string("yyyy-MM-dd")}
*/
@AllArgsConstructor
@RestController
@RequestMapping(${requestBaseMapping})
@Tag(name = "${comment}", description = "${comment}")
@Slf4j
public class ${name}Controller {

private final ${name}Service ${firstLowName}Service;

@PostMapping()
@Operation(summary = "创建")
public ApiResp
<Void> create${name}(@Validated @RequestBody Create${name}Request request) {
    ${firstLowName}Service.create${name}(request);
    return RestfulApiRespFactory.ok();
    }

    @PutMapping()
    @Operation(summary = "更新")
    public ApiResp
    <Void> update${name}(@Validated @RequestBody Update${name}Request request) {
        ${firstLowName}Service.update${name}(request);
        return RestfulApiRespFactory.ok();
        }

        @DeleteMapping()
        @Operation(summary = "删除")
        public ApiResp
        <Void> delete${name}ByIds(@RequestParam("ids") Long[] ids) {
            ${firstLowName}Service.delete${name}ByIds(ids);
            return RestfulApiRespFactory.ok();
            }

            @GetMapping("/{id}")
            @Operation(summary = "获取详情")
            public ApiResp
            <${name}DTO> query${name}ById(@PathVariable("id") Long id) {
                return RestfulApiRespFactory.ok(${firstLowName}Service.query${name}ById(id));
                }

                @GetMapping()
                @Operation(summary = "查询分页数据")
                public ApiResp
                <WindPagination
                <${name}DTO>> query${name}s(${name}Query query, DefaultPageQueryOptions options) {
                    return RestfulApiRespFactory.ok(${firstLowName}Service.query${name}s(query, options));
                    }

                    }
