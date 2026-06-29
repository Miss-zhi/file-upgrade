package com.qiwenshare.file.api;

import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.file.FileVersion;
import java.util.List;

public interface IFileVersionService {

    FileVersion saveVersion(String fileId, String fileName, String filePath,
                            Long fileSize, String storagePath, String userId);

    List<FileVersion> listVersions(String fileId);

    FileBean restoreVersion(String fileId, String versionId, String userId);

    void cleanupOldVersions(String fileId, int maxVersions);
}
