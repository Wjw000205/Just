package org.example.just.service;

import org.example.just.dto.userDto.*;
import org.example.just.utils.Result;

public interface UserService {
    Result<String> register(UserRegisterDTO dto);

    Result<String> login(UserLoginDTO dto);

    Result<UserProfileVO> getMyProfile(String username);

    Result<String> updateMyProfile(String username, UpdateMyProfileDTO dto);

    Result<String> updatePassword(String username, UpdatePasswordDTO dto);

    Result<String> updateSecondPassword(String username, UpdateSecondPasswordDTO dto);

    Result<String> completeUserInfo(String username, CompleteUserInfoDTO dto);

    Result<AuthProfileVO> getAuthProfile();
}
