package com.qiwenshare.file.api;

import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.task.UploadTask;
import java.io.InputStream;

public interface IFiletransferService {

    void uploadChunk(String identifier, int chunkNum, int totalChunks,
                     String fileName, String filePath, long totalSize,
                     String userId, InputStream chunkStream);

    FileBean mergeChunks(String identifier, String filePath, String userId);

    UploadTask getProgress(String identifier);

    void cleanupChunks(String identifier);
}
