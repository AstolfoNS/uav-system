package com.tf.backend.core.common.exception;

import java.io.Serial;

public class TokenAuthenticationException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;


    public TokenAuthenticationException() {
        super();
    }

    public TokenAuthenticationException(String message) {
        super(message);
    }

    public TokenAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenAuthenticationException(Throwable cause) {
        super(cause);
    }
}
