package com.wind.common.exception;

import com.wind.common.enums.DescriptiveEnum;

import java.io.Serial;
import java.io.Serializable;

/**
 * 异常码描述接口，不同的业务场景可以实现该接口做扩展。
 * 建议用枚举实现
 *
 * @author wuxp
 * @see DefaultExceptionCode
 */
public interface ExceptionCode extends DescriptiveEnum, Serializable {

    /**
     * 统一表示成功的 code
     */
    String SUCCESSFUL_CODE = "0";

    /**
     * 表示成功的 code
     */
    ExceptionCode SUCCESSFUL = new ExceptionCode() {
        @Serial
        private static final long serialVersionUID = 5034455936657195532L;

        @Override
        public String getCode() {
            return SUCCESSFUL_CODE;
        }

        @Override
        public String getDesc() {
            return "";
        }
    };


    /**
     * @return 异常码
     */
    String getCode();
}
