package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.just.dto.userDto.UserLoginDTO;
import org.example.just.dto.userDto.UserRegisterDTO;
import org.example.just.dao.UserDao;
import org.example.just.entity.UserEntity;
import org.example.just.service.UserService;
import org.example.just.utils.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImp implements UserService {

    private final UserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImp(UserDao userDao, BCryptPasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Result<String> register(UserRegisterDTO dto) {

        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }

        if (!StringUtils.hasText(dto.getUsername())) {
            return Result.fail("用户名不能为空");
        }

        if (!StringUtils.hasText(dto.getPassword())) {
            return Result.fail("密码不能为空");
        }

        if (!StringUtils.hasText(dto.getTelephone())) {
            return Result.fail("手机号不能为空");
        }

        if (!StringUtils.hasText(dto.getEmail())) {
            return Result.fail("邮箱不能为空");
        }

        LambdaQueryWrapper<UserEntity> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(UserEntity::getUsername, dto.getUsername());
        if (userDao.selectCount(usernameWrapper) > 0) {
            return Result.fail("用户名已存在");
        }

        LambdaQueryWrapper<UserEntity> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(UserEntity::getTelephone, dto.getTelephone());
        if (userDao.selectCount(phoneWrapper) > 0) {
            return Result.fail("手机号已被注册");
        }

        LambdaQueryWrapper<UserEntity> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(UserEntity::getEmail, dto.getEmail());
        if (userDao.selectCount(emailWrapper) > 0) {
            return Result.fail("邮箱已被注册");
        }

        UserEntity user = new UserEntity();
        BeanUtils.copyProperties(dto, user);

        // 密码加密
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(0);
        user.setCreateTime(LocalDateTime.now());
        user.setDeleted(0);

        int rows = userDao.insert(user);
        if (rows <= 0) {
            return Result.fail("注册失败");
        }

        return Result.success("注册成功");
    }

    @Override
    public Result<String> login(UserLoginDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }

        if (!StringUtils.hasText(dto.getUsername())) {
            return Result.fail("用户名不能为空");
        }

        if (!StringUtils.hasText(dto.getPassword())) {
            return Result.fail("密码不能为空");
        }

        LambdaQueryWrapper<UserEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserEntity::getUsername, dto.getUsername())
                .last("limit 1");

        UserEntity user = userDao.selectOne(queryWrapper);

        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        boolean match = passwordEncoder.matches(dto.getPassword(), user.getPassword());
        if (!match) {
            return Result.fail("用户名或密码错误");
        }

        return Result.success("登录成功");
    }
}