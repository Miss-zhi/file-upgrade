package com.qiwenshare.ufop.exception.operation;

import com.qiwenshare.ufop.exception.UFOPException;

public class DownloadException extends UFOPException {
    public DownloadException(String message, Throwable cause) { super(message, cause); }
    public DownloadException(String message) { super(message); }
}
