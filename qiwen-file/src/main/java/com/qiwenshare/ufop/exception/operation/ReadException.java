package com.qiwenshare.ufop.exception.operation;

import com.qiwenshare.ufop.exception.UFOPException;

public class ReadException extends UFOPException {
    public ReadException(String message, Throwable cause) { super(message, cause); }
    public ReadException(String message) { super(message); }
}
