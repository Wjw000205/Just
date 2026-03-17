package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.just.dao.DepartmentDao;
import org.example.just.dto.departmentDto.CreateDepartmentDTO;
import org.example.just.dto.departmentDto.UpdateDepartmentDTO;
import org.example.just.entity.DepartmentEntity;
import org.example.just.service.DepartmentService;
import org.example.just.utils.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class DepartmentServiceImp implements DepartmentService {

    private final DepartmentDao departmentDao;

    public DepartmentServiceImp(DepartmentDao departmentDao) {
        this.departmentDao = departmentDao;
    }

    @Transactional
    @Override
    public Result<String> createDepartment(CreateDepartmentDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDepartmentName())) {
            return Result.fail("部门名称不能为空");
        }
        if (!StringUtils.hasText(dto.getLeader())) {
            return Result.fail("部门负责人不能为空");
        }

        LambdaQueryWrapper<DepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentEntity::getDepartmentName, dto.getDepartmentName());

        if (departmentDao.selectCount(wrapper) > 0) {
            return Result.fail("部门名称已存在");
        }

        DepartmentEntity department = new DepartmentEntity();
        department.setDepartmentName(dto.getDepartmentName());
        department.setLeader(dto.getLeader());
        department.setDeleted(0);
        department.setCreatedTime(LocalDateTime.now());

        int rows = departmentDao.insert(department);
        if (rows <= 0) {
            return Result.fail("新建部门失败");
        }

        return Result.success("新建部门成功");
    }

    @Transactional
    @Override
    public Result<String> updateDepartment(UpdateDepartmentDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (dto.getId() == null) {
            return Result.fail("部门ID不能为空");
        }
        if (!StringUtils.hasText(dto.getDepartmentName())) {
            return Result.fail("部门名称不能为空");
        }
        if (!StringUtils.hasText(dto.getLeader())) {
            return Result.fail("部门负责人不能为空");
        }

        DepartmentEntity oldDepartment = departmentDao.selectById(dto.getId());
        if (oldDepartment == null) {
            return Result.fail("部门不存在");
        }

        LambdaQueryWrapper<DepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentEntity::getDepartmentName, dto.getDepartmentName())
                .ne(DepartmentEntity::getId, dto.getId());

        if (departmentDao.selectCount(wrapper) > 0) {
            return Result.fail("部门名称已存在");
        }

        LambdaUpdateWrapper<DepartmentEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DepartmentEntity::getId, dto.getId())
                .set(DepartmentEntity::getDepartmentName, dto.getDepartmentName())
                .set(DepartmentEntity::getLeader, dto.getLeader());

        int rows = departmentDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("修改部门失败");
        }

        return Result.success("修改部门成功");
    }
}