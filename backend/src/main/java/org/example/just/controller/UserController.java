package org.example.just.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.just.dto.userDto.UserLoginDTO;
import org.example.just.dto.userDto.UserRegisterDTO;
import org.example.just.service.UserService;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户接口", description = "用户相关操作")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册接口，密码会进行BCrypt加密")
    public Result<String> register(@RequestBody UserRegisterDTO dto) {
        return userService.register(dto);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口")
    public Result<String> login(@RequestBody UserLoginDTO dto) {
        return userService.login(dto);
    }
}