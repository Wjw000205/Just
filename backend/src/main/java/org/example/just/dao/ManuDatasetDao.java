package org.example.just.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.just.entity.ManuDatabaseEntity;

@Mapper
public interface ManuDatasetDao extends BaseMapper<ManuDatabaseEntity> {
}