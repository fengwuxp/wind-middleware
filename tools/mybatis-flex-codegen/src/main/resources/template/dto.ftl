package ${basePackage}.services.model.${outPutType.dir};
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.*;
<#--类名以Query结尾-->
<#if javaClassName?ends_with('Query')>
    import com.wind.common.query.supports.AbstractPageQuery;
    import com.wind.common.query.supports.DefaultOrderField;
</#if>
<#if dependencies??>
<#--依赖导入处理-->
    <#list dependencies as d >
        import ${d};
    </#list>
</#if>

/**
* ${comment}
*
* @author ${author}
* @since ${.now?string("yyyy-MM-dd")}
*/
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class  ${javaClassName} <#if javaClassName?ends_with('DTO')>implements java.io.Serializable</#if> {

    <#if fields??>
    <#--字段-->
        <#list fields as f >
            <#if f.comment??>
                @Schema(description = "${f.comment}")
            </#if>
            <#list f.annotations as an >
                @${an}
            </#list>
            private ${f.type} ${f.name};

        </#list>
    </#if>
    }
