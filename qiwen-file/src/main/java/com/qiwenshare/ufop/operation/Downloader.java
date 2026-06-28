package com.qiwenshare.ufop.operation;

import java.io.InputStream;

/**
 * 下载操作接口
 */
public interface Downloader {
    InputStream download(String path);
}
