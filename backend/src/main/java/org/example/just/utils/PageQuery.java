package org.example.just.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "PageQuery", description = "分页请求参数")
public class PageQuery {

    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    public int getOffset() {
        int currentPage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        return (currentPage - 1) * size;
    }

    public int getRealPageSize() {
        return (pageSize == null || pageSize < 1) ? 10 : pageSize;
    }

    public int getRealPageNum() {
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }
}