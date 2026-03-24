package org.example.just.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.just.entity.ModuleEntity;

@Mapper
public interface ModuleDao extends BaseMapper<ModuleEntity> {
}