package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BatchUploadErrorVO", description = "批量上传错误")
public class BatchUploadErrorVO {

    @Schema(description = "Excel行号，从1开始", example = "5")
    private Integer rowIndex;

    @Schema(description = "错误信息", example = "样品编号重复")
    private String message;
}
