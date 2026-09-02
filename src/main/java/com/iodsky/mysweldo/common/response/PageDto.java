package com.iodsky.mysweldo.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class PageDto<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> content;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PaginationMeta meta;

    public static <T> PageDto<T> of(Page<T> page) {
        return PageDto.<T>builder()
                .content(page.getContent())
                .meta(PaginationMeta.of(page))
                .build();
    }
}