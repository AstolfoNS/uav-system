package com.tf.backend.core.common.exception;

import java.io.Serial;

public class MinioFileUrlExtractException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    public MinioFileUrlExtractException() {
        super();
    }

    public MinioFileUrlExtractException(String message) {
        super(message);
    }

    public MinioFileUrlExtractException(String message, Throwable cause) {
        super(message, cause);
    }

    public MinioFileUrlExtractException(Throwable cause) {
        super(cause);
    }
}
