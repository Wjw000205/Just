package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_favorite")
public class FavoriteEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("username")
    private String username;

    @TableField("dataset_id")
    private Integer datasetId;

    @TableField("favorited")
    private Boolean favorited;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    private Integer deleted;
}
