package com.iodsky.mysweldo.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "success",
        "message",
        "data",
        "meta",
        "timestamp",
})
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private  PaginationMeta meta;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
