package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.just.dto.userDto.*;
import org.example.just.dao.UserDao;
import org.example.just.entity.UserEntity;
import org.example.just.service.UserService;
import org.example.just.utils.JwtUtil;
import org.example.just.utils.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.just.dao.UserDepartmentDao;
import org.example.just.dto.userDto.UpdateMyProfileDTO;
import org.example.just.dto.userDto.UpdatePasswordDTO;
import org.example.just.dto.userDto.UpdateSecondPasswordDTO;
import org.example.just.dto.userDto.UserProfileVO;
import org.example.just.entity.UserDepartmentEntity;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class UserServiceImp implements UserService {

    private final UserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserDepartmentDao userDepartmentDao;
    private final JwtUtil jwtUtil;


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

        if (!StringUtils.hasText(dto.getRealName())) {
            return Result.fail("真实姓名不能为空");
        }

        String username = dto.getUsername().trim();
        String password = dto.getPassword().trim();
        String telephone = dto.getTelephone().trim();
        String email = dto.getEmail().trim();
        String realName = dto.getRealName().trim();

        LambdaQueryWrapper<UserEntity> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleted, 0);
        if (userDao.selectCount(usernameWrapper) > 0) {
            return Result.fail("用户名已存在");
        }

        LambdaQueryWrapper<UserEntity> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(UserEntity::getTelephone, telephone)
                .eq(UserEntity::getDeleted, 0);
        if (userDao.selectCount(phoneWrapper) > 0) {
            return Result.fail("手机号已被注册");
        }

        LambdaQueryWrapper<UserEntity> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(UserEntity::getEmail, email)
                .eq(UserEntity::getDeleted, 0);
        if (userDao.selectCount(emailWrapper) > 0) {
            return Result.fail("邮箱已被注册");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setTelephone(telephone);
        user.setEmail(email);
        user.setRealName(realName);
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
        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Result.success(token);
    }

    @Override
    public Result<UserProfileVO> getMyProfile(String username) {
        if (!StringUtils.hasText(username)) {
            return Result.fail("当前用户不能为空");
        }

        LambdaQueryWrapper<UserEntity> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(UserEntity::getUsername, username).last("limit 1");
        UserEntity user = userDao.selectOne(userWrapper);

        if (user == null) {
            return Result.fail("用户不存在");
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setTelephone(user.getTelephone());
        vo.setRole(user.getRole());

        LambdaQueryWrapper<UserDepartmentEntity> departmentWrapper = new LambdaQueryWrapper<>();
        departmentWrapper.eq(UserDepartmentEntity::getUsername, username).last("limit 1");
        UserDepartmentEntity userDepartment = userDepartmentDao.selectOne(departmentWrapper);

        if (userDepartment == null) {
            vo.setDepartment("无部门");
            vo.setApplyDepartment("无部门");
            vo.setApplyState("未申请");
        } else {
            if (userDepartment.getUserState() != null && userDepartment.getUserState() == 0) {
                vo.setDepartment("无部门");
                vo.setApplyDepartment(userDepartment.getDepartment());
                vo.setApplyState("审核中");
            } else if (userDepartment.getUserState() != null && userDepartment.getUserState() == 1) {
                vo.setDepartment(userDepartment.getDepartment());
                vo.setApplyDepartment("无部门");
                vo.setApplyState("无");
            } else {
                vo.setDepartment("无部门");
                vo.setApplyDepartment(userDepartment.getDepartment());
                vo.setApplyState("未知状态");
            }
        }

        return Result.success(vo);
    }

    @Transactional
    @Override
    public Result<String> updateMyProfile(String username, UpdateMyProfileDTO dto) {
        if (!StringUtils.hasText(username)) {
            return Result.fail("当前用户不能为空");
        }
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getRealName())) {
            return Result.fail("真实姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            return Result.fail("电子邮箱不能为空");
        }
        if (!StringUtils.hasText(dto.getTelephone())) {
            return Result.fail("手机号码不能为空");
        }

        LambdaQueryWrapper<UserEntity> currentWrapper = new LambdaQueryWrapper<>();
        currentWrapper.eq(UserEntity::getUsername, username).last("limit 1");
        UserEntity currentUser = userDao.selectOne(currentWrapper);
        if (currentUser == null) {
            return Result.fail("用户不存在");
        }

        LambdaQueryWrapper<UserEntity> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(UserEntity::getEmail, dto.getEmail())
                .ne(UserEntity::getUsername, username);
        if (userDao.selectCount(emailWrapper) > 0) {
            return Result.fail("电子邮箱已被占用");
        }

        LambdaQueryWrapper<UserEntity> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(UserEntity::getTelephone, dto.getTelephone())
                .ne(UserEntity::getUsername, username);
        if (userDao.selectCount(phoneWrapper) > 0) {
            return Result.fail("手机号码已被占用");
        }

        LambdaUpdateWrapper<UserEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserEntity::getUsername, username)
                .set(UserEntity::getRealName, dto.getRealName())
                .set(UserEntity::getEmail, dto.getEmail())
                .set(UserEntity::getTelephone, dto.getTelephone());

        int rows = userDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("编辑资料失败");
        }

        return Result.success("编辑资料成功");
    }

    @Transactional
    @Override
    public Result<String> updatePassword(String username, UpdatePasswordDTO dto) {
        if (!StringUtils.hasText(username)) {
            return Result.fail("当前用户不能为空");
        }
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getCurrentPassword())) {
            return Result.fail("当前密码不能为空");
        }
        if (!StringUtils.hasText(dto.getNewPassword())) {
            return Result.fail("新的登录密码不能为空");
        }
        if (!StringUtils.hasText(dto.getConfirmPassword())) {
            return Result.fail("确认密码不能为空");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("两次输入的新密码不一致");
        }

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username).last("limit 1");
        UserEntity user = userDao.selectOne(wrapper);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            return Result.fail("当前密码错误");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            return Result.fail("新密码不能与当前密码相同");
        }

        LambdaUpdateWrapper<UserEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserEntity::getUsername, username)
                .set(UserEntity::getPassword, passwordEncoder.encode(dto.getNewPassword()));

        int rows = userDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("修改密码失败");
        }

        return Result.success("修改密码成功");
    }

    @Transactional
    @Override
    public Result<String> updateSecondPassword(String username, UpdateSecondPasswordDTO dto) {
        if (!StringUtils.hasText(username)) {
            return Result.fail("当前用户不能为空");
        }
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getCurrentSecondPassword())) {
            return Result.fail("当前二级密码不能为空");
        }
        if (!StringUtils.hasText(dto.getNewSecondPassword())) {
            return Result.fail("新的二级密码不能为空");
        }
        if (!StringUtils.hasText(dto.getConfirmSecondPassword())) {
            return Result.fail("确认二级密码不能为空");
        }
        if (!dto.getNewSecondPassword().equals(dto.getConfirmSecondPassword())) {
            return Result.fail("两次输入的二级密码不一致");
        }

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username).last("limit 1");
        UserEntity user = userDao.selectOne(wrapper);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        if (!StringUtils.hasText(user.getSecondPassword())) {
            return Result.fail("当前用户尚未设置二级密码");
        }

        if (!passwordEncoder.matches(dto.getCurrentSecondPassword(), user.getSecondPassword())) {
            return Result.fail("当前二级密码错误");
        }

        if (passwordEncoder.matches(dto.getNewSecondPassword(), user.getSecondPassword())) {
            return Result.fail("新二级密码不能与当前二级密码相同");
        }

        LambdaUpdateWrapper<UserEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserEntity::getUsername, username)
                .set(UserEntity::getSecondPassword, passwordEncoder.encode(dto.getNewSecondPassword()));

        int rows = userDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("修改二级密码失败");
        }

        return Result.success("修改二级密码成功");
    }

    @Transactional
    @Override
    public Result<String> completeUserInfo(String username, CompleteUserInfoDTO dto) {
        if (!StringUtils.hasText(username)) {
            return Result.fail("当前用户不能为空");
        }
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getRealName())) {
            return Result.fail("真实姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getSecondPassword())) {
            return Result.fail("二级密码不能为空");
        }
        if (!StringUtils.hasText(dto.getConfirmSecondPassword())) {
            return Result.fail("确认二级密码不能为空");
        }
        if (!dto.getSecondPassword().equals(dto.getConfirmSecondPassword())) {
            return Result.fail("两次输入的二级密码不一致");
        }

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username).last("limit 1");
        UserEntity user = userDao.selectOne(wrapper);

        if (user == null) {
            return Result.fail("用户不存在");
        }

        if (StringUtils.hasText(user.getSecondPassword())) {
            return Result.fail("二级密码已设置，请使用修改二级密码接口");
        }

        LambdaUpdateWrapper<UserEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserEntity::getUsername, username)
                .set(UserEntity::getRealName, dto.getRealName())
                .set(UserEntity::getSecondPassword, passwordEncoder.encode(dto.getSecondPassword()));

        int rows = userDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("完善用户信息失败");
        }

        return Result.success("完善用户信息成功");
    }
}