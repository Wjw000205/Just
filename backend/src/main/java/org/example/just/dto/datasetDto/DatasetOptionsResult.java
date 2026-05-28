package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "DatasetOptionsResult", description = "可用数据集列表响应")
public class DatasetOptionsResult {

    @Schema(description = "业务状态码，0表示成功", example = "0")
    private Integer code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "可选数据集数组")
    private List<DatasetOptionsVO> data;

    @Schema(description = "总条数", example = "1")
    private Integer total;

    public static DatasetOptionsResult success(List<DatasetOptionsVO> data, Integer total) {
        DatasetOptionsResult result = new DatasetOptionsResult();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        result.setTotal(total);
        return result;
    }
}
