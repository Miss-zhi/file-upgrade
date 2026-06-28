package com.qiwenshare.ufop.exception;

/**
 * UFOP 操作异常
 */
public class UFOPException extends RuntimeException {

    public UFOPException(String message) {
        super(message);
    }

    public UFOPException(String message, Throwable cause) {
        super(message, cause);
    }
}
