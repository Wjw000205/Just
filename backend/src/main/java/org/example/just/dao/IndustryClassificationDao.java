package org.example.just.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.just.entity.IndustryClassificationEntity;

import java.util.List;

@Mapper
public interface IndustryClassificationDao extends BaseMapper<IndustryClassificationEntity> {
    
    /**
     * 查询所有分类
     */
    default List<IndustryClassificationEntity> selectAll() {
        return selectList(null);
    }
    
    /**
     * 根据父级 ID 查询子分类
     */
    default List<IndustryClassificationEntity> selectByParentId(String parentId) {
        LambdaQueryWrapper<IndustryClassificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IndustryClassificationEntity::getParentId, parentId);
        wrapper.orderByAsc(IndustryClassificationEntity::getSortOrder);
        return selectList(wrapper);
    }
}
