package com.wind.server.web.exception;


import com.wind.common.WindConstants;
import com.wind.common.exception.BaseException;
import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.server.web.supports.ApiResp;
import com.wind.web.exception.GlobalExceptionLogDecisionMaker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.stream.Collectors;

import static com.wind.common.WindConstants.WIND_SERVER_PROPERTIES_PREFIX;


/**
 * 默认全局异常处理
 *
 * @author wxup
 */
@Slf4j
@ConditionalOnProperty(prefix = WIND_SERVER_PROPERTIES_PREFIX, name = "enabled-global-exception", havingValue = WindConstants.TRUE, matchIfMissing
        = true)
@RestControllerAdvice()
@AllArgsConstructor
public class DefaultGlobalExceptionHandler {

    /**
     * 数据库唯一键异常消息 i18n key
     */
    private static final String DB_DUPLICATE_KEY_I18N_KEY = "$.db.duplicate.key.exception";

    /**
     * 数据库访问异常消息 i18n key
     */
    private static final String DB_ACCESS_DATA_I18N_KEY = "$.db.access.data.exception";

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    @ResponseBody
    public ApiResp<Void> handleValidationException(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> constraintViolations = exception.getConstraintViolations();
        return RestfulApiRespFactory.badRequest(constraintViolations.stream().map(constraintViolation -> {
            String message = constraintViolation.getMessage();
            String propertyPath = constraintViolation.getPropertyPath().toString();
            return MessageFormat.format("{0}：{1}", propertyPath, message);
        }).collect(Collectors.joining("、")));

    }

    /**
     * {@link org.springframework.web.bind.annotation.RequestParam}
     * {@link org.springframework.web.bind.annotation.RequestHeader}
     * 等参数绑定失败异常
     */
    @ExceptionHandler(value = ServletRequestBindingException.class)
    @ResponseBody
    public ApiResp<Void> handleServletRequestBindingException(ServletRequestBindingException exception) {
        return RestfulApiRespFactory.badRequest(exception.getMessage());
    }

    /**
     * 上传文件文件过大
     */
    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    @ResponseBody
    public ApiResp<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        return RestfulApiRespFactory.payloadToLarge();
    }

    /**
     * 文件上传异常
     */
    @ExceptionHandler(value = MultipartException.class)
    @ResponseBody
    public ApiResp<Void> handleMultipartException(MultipartException exception) {
        return RestfulApiRespFactory.badRequest(exception.getMessage());
    }

    /**
     * controller bind data 的异常
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public ApiResp<Void> handleMethodBindException(BindException exception) {
        FieldError fieldError = exception.getFieldError();
        if (fieldError == null) {
            return RestfulApiRespFactory.badRequest(exception.getMessage());
        }
        String message = fieldError.getDefaultMessage();
        return RestfulApiRespFactory.badRequest(String.format("%s#%s：%s", fieldError.getObjectName(), fieldError.getField(), message));
    }

    /**
     * Not Found 异常处理
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ApiResp<Void> handleNotFoundError(Exception exception) {
        return RestfulApiRespFactory.notFound(exception.getMessage());
    }

    /**
     * 数据库唯一键冲突异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public ApiResp<Void> duplicateKeyException(Exception exception) {
        if (GlobalExceptionLogDecisionMaker.requiresPrintErrorLog(exception)) {
            log.error("唯一键冲突", exception);
        }
        return RestfulApiRespFactory.error(SpringI18nMessageUtils.getMessage(DB_DUPLICATE_KEY_I18N_KEY, "数据已存在"));
    }

    /**
     * 数据操作异常
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseBody
    public ApiResp<Void> dataAccessException(Exception exception) {
        if (GlobalExceptionLogDecisionMaker.requiresPrintErrorLog(exception)) {
            log.error("数据操作异常", exception);
        }
        return RestfulApiRespFactory.error(SpringI18nMessageUtils.getMessage(DB_ACCESS_DATA_I18N_KEY, "数据操作失败"));
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler({BaseException.class})
    @ResponseBody
    public ApiResp<Integer> handleBusinessServiceException(BaseException exception) {
        if (GlobalExceptionLogDecisionMaker.requiresPrintErrorLog(exception)) {
            log.error("业务异常，code = {}，errorMessage: {}", exception.getTextCode(), exception.getMessage(), exception);
        }
        return RestfulApiRespFactory.withThrowable(exception);
    }

    /**
     * 统一兜底异常处理，如果前面都没有捕获到，将进入该方法
     */
    @ExceptionHandler({Exception.class})
    @ResponseBody
    public ApiResp<Void> handleException(Exception exception) {
        Throwable throwable = exception;
        if (GlobalExceptionLogDecisionMaker.requiresPrintErrorLog(exception)) {
            log.error("捕获到异常: {}，errorMessage: {}", exception.getClass().getName(), exception.getMessage(), exception);
        }
        if (throwable instanceof UndeclaredThrowableException) {
            // 获取真正的异常
            InvocationTargetException invocationTargetException =
                    (InvocationTargetException) ((UndeclaredThrowableException) throwable).getUndeclaredThrowable();
            throwable = invocationTargetException.getTargetException();
        }
        return RestfulApiRespFactory.error(throwable.getMessage());
    }

}
