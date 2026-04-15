package com.tf.backend.core.common.exception;

import com.tf.backend.core.common.enumeration.HttpCode;
import lombok.Getter;

import java.io.Serial;

@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;


    public BizException() {
        super();
    }

    public BizException(String message) {
        super(message);
        this.code = HttpCode.FAILED.getCode(); // 默认业务失败码
    }

    public BizException(HttpCode httpCode) {
        super(httpCode.getMessage());
        this.code = httpCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }

    public BizException(Throwable cause) {
        super(cause);
    }
}
