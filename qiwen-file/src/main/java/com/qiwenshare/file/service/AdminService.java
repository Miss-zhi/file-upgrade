package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.file.api.IAdminService;
import com.qiwenshare.file.api.IStorageService;
import com.qiwenshare.file.domain.StorageBean;
import com.qiwenshare.file.domain.user.UserBean;
import com.qiwenshare.file.dto.user.ResetPasswordDTO;
import com.qiwenshare.file.dto.user.StorageUpdateDTO;
import com.qiwenshare.file.dto.user.UserAvailableDTO;
import com.qiwenshare.file.dto.user.UserSearchDTO;
import com.qiwenshare.common.util.HashUtils;
import com.qiwenshare.common.util.PasswordUtil;
import com.qiwenshare.file.mapper.UserMapper;
import com.qiwenshare.file.vo.user.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional(rollbackFor = Exception.class)
public class AdminService implements IAdminService {

    @Resource
    UserMapper userMapper;

    @Resource
    IStorageService storageService;

    @Resource
    UserService userService;

    @Override
    public RestResult<IPage<UserVO>> getUserList(UserSearchDTO userSearchDTO) {
        Page<UserVO> page = new Page<>(userSearchDTO.getCurrentPage(), userSearchDTO.getPageCount());
        IPage<UserVO> userList = userMapper.selectUserListWithStorage(page, userSearchDTO);
        return RestResult.<IPage<UserVO>>success().data(userList);
    }

    @Override
    public RestResult<String> updateUserAvailable(UserAvailableDTO userAvailableDTO) {
        UserBean userBean = new UserBean();
        userBean.setUserId(userAvailableDTO.getUserId());
        userBean.setAvailable(userAvailableDTO.getAvailable());
        userService.updateById(userBean);
        return RestResult.<String>success().message("操作成功");
    }

    @Override
    public RestResult<String> updateUserStorage(StorageUpdateDTO storageUpdateDTO) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StorageBean> lambdaQueryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StorageBean::getUserId, storageUpdateDTO.getUserId());
        StorageBean storageBean = storageService.getOne(lambdaQueryWrapper);
        if (storageBean == null) {
            storageBean = new StorageBean();
            storageBean.setUserId(storageUpdateDTO.getUserId());
            storageBean.setTotalStorageSize(storageUpdateDTO.getTotalStorageSize());
            storageService.save(storageBean);
        } else {
            storageBean.setTotalStorageSize(storageUpdateDTO.getTotalStorageSize());
            storageService.updateById(storageBean);
        }
        return RestResult.<String>success().message("修改存储空间成功");
    }

    @Override
    public RestResult<String> resetPassword(ResetPasswordDTO resetPasswordDTO) {
        UserBean userBean = userService.getById(resetPasswordDTO.getUserId());
        if (userBean == null) {
            return RestResult.fail().message("用户不存在");
        }
        String newSalt = PasswordUtil.getSaltValue();
        String newHashPassword = HashUtils.hashHex("MD5", resetPasswordDTO.getPassword(), newSalt, 1024);
        userBean.setSalt(newSalt);
        userBean.setPassword(newHashPassword);
        userService.updateById(userBean);
        return RestResult.success().message("重置密码成功");
    }
}
