package com.qiwenshare.ufop.operation;

import java.io.InputStream;

/** 上传操作接口 */
public interface Uploader {
    void upload(String path, InputStream inputStream);
}
