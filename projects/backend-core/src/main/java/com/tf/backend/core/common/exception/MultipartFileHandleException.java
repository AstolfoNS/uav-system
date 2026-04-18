package com.tf.backend.core.common.exception;

import java.io.Serial;

public class MultipartFileHandleException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    public MultipartFileHandleException() {
        super();
    }

    public MultipartFileHandleException(String message) {
        super(message);
    }

    public MultipartFileHandleException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultipartFileHandleException(Throwable cause) {
        super(cause);
    }
}
