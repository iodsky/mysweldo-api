package com.iodsky.mysweldo.leave.request;

import com.iodsky.mysweldo.leave.LeaveType;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestMapper {

    public LeaveRequestDto toDto(LeaveRequest leaveRequest) {
        return LeaveRequestDto.builder()
                .id(leaveRequest.getId())
                .employeeId(leaveRequest.getEmployee().getId())
                .requestDate(leaveRequest.getCreatedAt())
                .leaveType(leaveRequest.getLeaveType().toString())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .note(leaveRequest.getNote())
                .status(leaveRequest.getStatus().toString())
                .build();
    }

    public LeaveRequest updateEntity(LeaveRequest entity, LeaveRequestDto dto) {
        entity.setNote(dto.getNote());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setLeaveType(LeaveType.valueOf(dto.getLeaveType()));

        return entity;
    }

}
