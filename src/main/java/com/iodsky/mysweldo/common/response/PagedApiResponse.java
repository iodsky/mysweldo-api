package com.iodsky.mysweldo.common.response;

import lombok.Getter;

@Getter
public class PagedApiResponse<T> extends ApiResponse<T> {
    private final PaginationMeta meta;

    public PagedApiResponse(boolean success, String message, T data, PaginationMeta meta) {
        super(success, message, data);
        this.meta = meta;
    }
}
