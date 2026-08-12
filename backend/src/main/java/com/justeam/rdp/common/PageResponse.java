package com.justeam.rdp.common;

import java.util.List;

public record PageResponse<T>(long total, int pageNum, int pageSize, long pages, List<T> list) {
    public static <T> PageResponse<T> of(long total, int pageNum, int pageSize, List<T> list) {
        long pages = pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResponse<>(total, pageNum, pageSize, pages, list);
    }
}

