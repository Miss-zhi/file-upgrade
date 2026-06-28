package com.qiwenshare.file.api;

import com.qiwenshare.file.domain.file.FileBean;
import java.util.List;

/**
 * 文件服务接口
 */
public interface IFileService {

    /** 按路径列出文件和文件夹 */
    List<FileBean> listByPath(String path, String userId);

    /** 上传文件（记录元数据） */
    FileBean upload(String fileName, String filePath, Long fileSize, String fileType, String userId);

    /** 删除文件/文件夹 */
    void delete(String fileId, String userId);

    /** 创建文件夹 */
    FileBean createFolder(String parentPath, String folderName, String userId);

    /** 根据 ID 获取 */
    FileBean getById(String fileId);
}
