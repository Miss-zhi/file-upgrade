package com.qiwenshare.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiwenshare.file.domain.file.FileBean;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件 Mapper
 */
@Mapper
public interface FileBeanMapper extends BaseMapper<FileBean> {
}
