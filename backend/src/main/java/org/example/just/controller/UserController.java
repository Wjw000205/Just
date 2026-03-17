package org.example.just.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.just.dto.userDto.*;
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

    @GetMapping("/profile")
    @Operation(summary = "查询我的资料", description = "返回用户名、真实姓名、电子邮箱、手机号码、权限、所属部门、申请部门、申请状态")
    public Result<UserProfileVO> getMyProfile(@RequestHeader("username") String username) {
        return userService.getMyProfile(username);
    }

    @PutMapping("/profile")
    @Operation(summary = "编辑我的资料", description = "请求真实姓名、电子邮箱、手机号码")
    public Result<String> updateMyProfile(@RequestHeader("username") String username,
                                          @RequestBody UpdateMyProfileDTO dto) {
        return userService.updateMyProfile(username, dto);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "请求当前密码、新的登陆密码、确认密码")
    public Result<String> updatePassword(@RequestHeader("username") String username,
                                         @RequestBody UpdatePasswordDTO dto) {
        return userService.updatePassword(username, dto);
    }

    @PutMapping("/second-password")
    @Operation(summary = "修改二级密码", description = "请求当前二级密码、新的二级密码、确认二级密码")
    public Result<String> updateSecondPassword(@RequestHeader("username") String username,
                                               @RequestBody UpdateSecondPasswordDTO dto) {
        return userService.updateSecondPassword(username, dto);
    }

    @PostMapping("/complete-info")
    @Operation(summary = "完善用户信息", description = "补充注册时未填写的真实姓名和二级密码")
    public Result<String> completeUserInfo(@RequestHeader("username") String username,
                                           @RequestBody CompleteUserInfoDTO dto) {
        return userService.completeUserInfo(username, dto);
    }
}