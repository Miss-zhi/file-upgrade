package com.qiwenshare.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiwenshare.file.domain.user.Role;
import com.qiwenshare.file.domain.user.UserBean;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper extends BaseMapper<UserBean> {
    int insertUser(UserBean userBean);

    int insertUserRole(@Param("userId") String userId, @Param("roleId") long roleId);

    List<Role>  selectRoleListByUserId(@Param("userId") String userId);

    String selectSaltByTelephone(@Param("telephone") String telephone);

    UserBean selectUserByTelephoneAndPassword(@Param("telephone") String telephone, @Param("password") String password);

    com.baomidou.mybatisplus.core.metadata.IPage<com.qiwenshare.file.vo.user.UserVO> selectUserListWithStorage(com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.qiwenshare.file.vo.user.UserVO> page, @Param("userSearchDTO") com.qiwenshare.file.dto.user.UserSearchDTO userSearchDTO);

}
