package com.qiwenshare.ufop.exception.operation;

import com.qiwenshare.ufop.exception.UFOPException;

public class WriteException extends UFOPException {
    public WriteException(String message, Throwable cause) { super(message, cause); }
    public WriteException(String message) { super(message); }
}
