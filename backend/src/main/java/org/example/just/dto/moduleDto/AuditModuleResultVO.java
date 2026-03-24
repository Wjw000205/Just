package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "AuditModuleResultVO", description = "审核模板返回对象")
public class AuditModuleResultVO {

    @Schema(description = "模板 ID")
    private Integer id;

    @Schema(description = "模板名称")
    private String moduleName;

    @Schema(description = "审核状态：0-驳回，1-通过")
    private Integer status;

    @Schema(description = "审核意见")
    private String remark;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;
}
