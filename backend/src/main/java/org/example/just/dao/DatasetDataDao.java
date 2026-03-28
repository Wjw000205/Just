package org.example.just.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.just.entity.DatabaseDataEntity;

import java.util.List;

@Mapper
public interface DatasetDataDao extends BaseMapper<DatabaseDataEntity> {

    @Select({
            "<script>",
            "SELECT COALESCE(MAX(row_id), 0) FROM dataset_data",
            "WHERE deleted = 0",
            "AND column_id IN",
            "<foreach collection='columnIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    Integer selectMaxRowIdByColumnIds(@Param("columnIds") List<Integer> columnIds);

    @Insert({
            "<script>",
            "INSERT INTO dataset_data (column_id, row_id, data_type, data, deleted) VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.columnId}, #{item.rowId}, #{item.dataType}, #{item.data}, #{item.deleted})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<DatabaseDataEntity> list);

    @Select({
            "<script>",
            "SELECT COUNT(DISTINCT row_id) ",
            "FROM dataset_data ",
            "WHERE deleted = 0 ",
            "AND column_id IN ",
            "<foreach collection='columnIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    Long countDistinctRowIds(@Param("columnIds") List<Integer> columnIds);

    @Select({
            "<script>",
            "SELECT DISTINCT row_id ",
            "FROM dataset_data ",
            "WHERE deleted = 0 ",
            "AND column_id IN ",
            "<foreach collection='columnIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "ORDER BY row_id ASC ",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<Integer> selectPagedRowIds(@Param("columnIds") List<Integer> columnIds,
                                    @Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize);

    @Select({
            "<script>",
            "SELECT id, column_id, row_id, data_type, data, deleted ",
            "FROM dataset_data ",
            "WHERE deleted = 0 ",
            "AND column_id IN ",
            "<foreach collection='columnIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND row_id IN ",
            "<foreach collection='rowIds' item='rowId' open='(' separator=',' close=')'>",
            "#{rowId}",
            "</foreach>",
            "ORDER BY row_id ASC, column_id ASC",
            "</script>"
    })
    List<DatabaseDataEntity> selectByColumnIdsAndRowIds(@Param("columnIds") List<Integer> columnIds,
                                                        @Param("rowIds") List<Integer> rowIds);
}