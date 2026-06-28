package com.qiwenshare.ufop.operation;

import java.io.OutputStream;

/**
 * 写入操作接口
 */
public interface Writer {
    void write(String path, String content);
}
