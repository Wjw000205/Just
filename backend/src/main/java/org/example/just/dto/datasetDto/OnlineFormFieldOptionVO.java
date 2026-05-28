package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OnlineFormFieldOptionVO", description = "在线填写表单下拉选项")
public class OnlineFormFieldOptionVO {

    @Schema(description = "选项值", example = "experiment")
    private String value;

    @Schema(description = "选项显示名称", example = "实验测量")
    private String label;
}
