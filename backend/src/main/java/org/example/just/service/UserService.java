package org.example.just.service;

import org.example.just.dto.userDto.UserLoginDTO;
import org.example.just.dto.userDto.UserRegisterDTO;
import org.example.just.utils.Result;

public interface UserService {
    Result<String> register(UserRegisterDTO dto);

    Result<String> login(UserLoginDTO dto);
}
