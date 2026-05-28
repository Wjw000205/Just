package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("manu_dataset")
@Schema(name = "ManuDatabaseEntity", description = "模板目录实体")
public class ManuDatasetEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @TableField("name")
    @Schema(description = "模板/目录名称")
    private String name;

    @TableField("summary")
    @Schema(description = "数据集摘要")
    private String summary;

    @TableField("cover_url")
    @Schema(description = "封面图URL")
    private String coverUrl;

    @TableField("creator")
    @Schema(description = "创建人")
    private String creator;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("parent")
    @Schema(description = "父级目录，null 代表一级目录")
    private Integer parent;

    @TableField("is_menu")
    @Schema(description = "是否目录：1是目录，0不是目录")
    private Integer isMenu;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：0未删除，1已删除")
    private Integer deleted;

    @TableField("module")
    @Schema(description = "所属模板id：例如1")
    private Integer module;

    @TableField("science_category_id")
    @Schema(description = "科学分类ID")
    private Integer scienceCategoryId;

    @TableField("product_category_id")
    @Schema(description = "产业/产品分类ID")
    private Integer productCategoryId;

    @TableField("data_level")
    @Schema(description = "数据级别：highvalue/public/private")
    private String dataLevel;

    @TableField("data_category")
    @Schema(description = "数据类别，当前固定为dataset")
    private String dataCategory;

    @TableField("template_tag_id")
    @Schema(description = "模板标签ID")
    private Integer templateTagId;

    @TableField("dataset_tag_ids")
    @Schema(description = "数据集标签ID列表JSON")
    private String datasetTagIds;

    @TableField("audit_status")
    @Schema(description = "审核状态：0未审核，1审核通过，2审核未通过")
    private Integer auditStatus;
}
