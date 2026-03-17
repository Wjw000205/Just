package org.example.just.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.just.entity.DepartmentEntity;

@Mapper
public interface DepartmentDao extends BaseMapper<DepartmentEntity> {
}