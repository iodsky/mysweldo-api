package com.iodsky.mysweldo.position;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionMapper {

    public PositionDto toDto(Position position) {
        return PositionDto.builder()
                .id(position.getId())
                .departmentId(position.getDepartment() != null ? position.getDepartment().getId() : null)
                .departmentTitle(position.getDepartment() != null ? position.getDepartment().getTitle() : null)
                .title(position.getTitle())
                .createdAt(position.getCreatedAt())
                .updatedAt(position.getUpdatedAt())
                .build();
    }

    public Position toEntity(PositionRequest request) {
        return Position.builder()
                .id(request.getId())
                .title(request.getTitle())
                .build();
    }
}
