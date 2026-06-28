package com.qiwenshare.file.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.file.dto.user.StorageUpdateDTO;
import com.qiwenshare.file.dto.user.UserSearchDTO;
import com.qiwenshare.file.vo.user.UserVO;

public interface IAdminService {
    RestResult<com.baomidou.mybatisplus.core.metadata.IPage<com.qiwenshare.file.vo.user.UserVO>> getUserList(com.qiwenshare.file.dto.user.UserSearchDTO userSearchDTO);
    RestResult<String> updateUserAvailable(com.qiwenshare.file.dto.user.UserAvailableDTO userAvailableDTO);
    RestResult<String> updateUserStorage(com.qiwenshare.file.dto.user.StorageUpdateDTO storageUpdateDTO);
    RestResult<String> resetPassword(com.qiwenshare.file.dto.user.ResetPasswordDTO resetPasswordDTO);
}
