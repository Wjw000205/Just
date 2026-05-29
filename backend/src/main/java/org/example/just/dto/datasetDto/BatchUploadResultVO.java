package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "BatchUploadResultVO", description = "批量上传响应")
public class BatchUploadResultVO {

    @Schema(description = "上传任务ID", example = "job-20260316-0001")
    private String taskId;

    @Schema(description = "成功接收记录数", example = "100")
    private Integer acceptedCount = 0;

    @Schema(description = "失败记录数", example = "3")
    private Integer failedCount = 0;

    @Schema(description = "失败明细")
    private List<BatchUploadErrorVO> errors = new ArrayList<>();
}
