package com.qiwenshare.ufop.exception.operation;

import com.qiwenshare.ufop.exception.UFOPException;

public class DeleteException extends UFOPException {
    public DeleteException(String message, Throwable cause) { super(message, cause); }
    public DeleteException(String message) { super(message); }
}
