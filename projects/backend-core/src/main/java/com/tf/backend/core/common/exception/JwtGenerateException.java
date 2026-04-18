package com.tf.backend.core.common.exception;

import java.io.Serial;

public class JwtGenerateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    public JwtGenerateException() {
        super();
    }

    public JwtGenerateException(String message) {
        super(message);
    }

    public JwtGenerateException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwtGenerateException(Throwable cause) {
        super(cause);
    }
}
