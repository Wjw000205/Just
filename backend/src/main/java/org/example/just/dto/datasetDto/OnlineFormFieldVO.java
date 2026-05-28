package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "OnlineFormFieldVO", description = "在线填写表单字段")
public class OnlineFormFieldVO {

    @Schema(description = "字段唯一标识")
    private String id;

    @Schema(description = "字段显示名称")
    private String label;

    @Schema(description = "字段类型：text/select/textarea", example = "text")
    private String type;

    @Schema(description = "是否必填", example = "true")
    private Boolean required;

    @Schema(description = "占位提示")
    private String placeholder;

    @Schema(description = "字段说明")
    private String description;

    @Schema(description = "下拉选项，仅 type=select 时使用")
    private List<OnlineFormFieldOptionVO> options;
}
