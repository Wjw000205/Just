package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "审核模板请求参数")
public class AuditModuleDTO {

    @Schema(description = "模板id", example = "1")
    private Integer id;

    @Schema(description = "审核状态：1-驳回，2-通过", example = "2")
    private Integer auditState;
}