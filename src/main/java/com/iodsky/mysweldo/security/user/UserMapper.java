package com.iodsky.mysweldo.security.user;

import org.springframework.stereotype.Component;

@Component
public class UserMapper  {

    public UserDto toDto(User user) {
        return UserDto
                .builder()
                .id(user.getId())
                .employeeId(user.getEmployee().getId())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public User toEntity(UserRequest userRequest) {
        return User.builder()
                .email(userRequest.getEmail())
                .password(userRequest.getPassword())
                .build();
    }

}
