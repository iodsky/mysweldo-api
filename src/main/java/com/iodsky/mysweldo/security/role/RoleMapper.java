package com.iodsky.mysweldo.security.role;

import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleDto toDto(Role entity) {
        if (entity == null) return null;

        return RoleDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .lastModified(entity.getUpdatedAt())
                .lastModifiedBy(entity.getLastModifiedBy().getId())
                .build();
    }

}
