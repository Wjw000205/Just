package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "ManuDatabaseTreeVO", description = "模板目录树形结构返回对象")
public class ManuDatasetTreeVO {

    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "目录名称")
    private String name;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "父级目录")
    private Integer parent;

    @Schema(description = "是否目录：1是目录，0不是目录")
    private Integer isMenu;

    @Schema(description = "子目录")
    private List<ManuDatasetTreeVO> children = new ArrayList<>();

    @Schema(description = "审核状态：0未审核，1审核通过，2审核未通过")
    private Integer auditStatus;

    @Schema(description = "数据级别：highvalue/public/private")
    private String dataLevel;
}