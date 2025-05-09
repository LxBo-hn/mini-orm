package com.t2308e.exception;

public class MiniOrmException extends RuntimeException {
    public MiniOrmException(String message) {
        super(message);
    }

    public MiniOrmException(String message, Throwable cause) {
        super(message, cause);
    }
}
