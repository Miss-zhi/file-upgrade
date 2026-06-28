package com.qiwenshare.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiwenshare.file.domain.user.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
