package com.wind.server.logging;

import com.wind.script.auditlog.AuditLogRecorder;
import com.wind.script.auditlog.ScriptAuditLogRecorder;
import com.wind.server.web.supports.ApiResp;
import com.wind.web.util.HttpServletRequestUtils;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * web 环境下记录操作日志构建
 *
 * @author wuxp
 * @date 2023-09-23 10:40
 **/
public class WebAuditLogRecorder extends ScriptAuditLogRecorder {

    public WebAuditLogRecorder(AuditLogRecorder recorder) {
        super(recorder, WebAuditLogRecorder::getRequestVariables);
    }

    @Override
    protected Object resolveMethodReturnValue(Object methodReturnValue) {
        if (methodReturnValue instanceof ApiResp) {
            return ((ApiResp<?>) methodReturnValue).getData();
        }
        return super.resolveMethodReturnValue(methodReturnValue);
    }

    private static Map<String, Object> getRequestVariables() {
        HttpServletRequest httpRequest = HttpServletRequestUtils.getContextRequestOfNullable();
        if (httpRequest == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        // 填充 http request 上下文中的变量
        Enumeration<String> attributeNames = httpRequest.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            Object attribute = httpRequest.getAttribute(name);
            if (attribute != null) {
                result.put(name, attribute);
            }
        }
        result.put(HttpHeaders.USER_AGENT, httpRequest.getHeader(HttpHeaders.USER_AGENT));
        return Collections.unmodifiableMap(result);
    }

}
