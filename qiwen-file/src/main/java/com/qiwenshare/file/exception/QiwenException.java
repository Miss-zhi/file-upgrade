package com.qiwenshare.file.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class QiwenException extends RuntimeException {

    private final int code;

    public QiwenException(int code, String message) {
        super(message);
        this.code = code;
    }

    public QiwenException(String message) {
        super(message);
        this.code = 500;
    }
}
