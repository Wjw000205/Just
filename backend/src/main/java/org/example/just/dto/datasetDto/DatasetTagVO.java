package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "DatasetTagVO", description = "数据集标签")
public class DatasetTagVO {

    @Schema(description = "标签ID", example = "21")
    private Integer id;

    @Schema(description = "标签名称", example = "粉末")
    private String name;
}
