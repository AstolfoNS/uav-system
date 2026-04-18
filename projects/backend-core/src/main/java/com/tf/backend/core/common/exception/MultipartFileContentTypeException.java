package com.tf.backend.core.common.exception;

import java.io.Serial;

public class MultipartFileContentTypeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    public MultipartFileContentTypeException() {
        super();
    }

    public MultipartFileContentTypeException(String message) {
        super(message);
    }

    public MultipartFileContentTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultipartFileContentTypeException(Throwable cause) {
        super(cause);
    }
}
