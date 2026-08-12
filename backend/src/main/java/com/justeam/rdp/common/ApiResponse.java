package com.justeam.rdp.common;

import java.time.Instant;

public record ApiResponse<T>(int code, String message, T data, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data, Instant.now());
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}

