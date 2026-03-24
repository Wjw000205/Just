package org.example.just.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "PageResult", description = "分页返回结果")
public class PageResult<T> {

    @Schema(description = "当前页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;

    @Schema(description = "总条数")
    private Long total;

    @Schema(description = "总页数")
    private Long totalPages;

    @Schema(description = "当前页数据")
    private List<T> records;

    public static <T> PageResult<T> of(Integer pageNum, Integer pageSize, Long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setRecords(records);
        long size = (pageSize == null || pageSize <= 0) ? 10 : pageSize;
        result.setTotalPages((total + size - 1) / size);
        return result;
    }
}