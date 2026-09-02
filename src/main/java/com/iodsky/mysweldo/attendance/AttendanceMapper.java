package com.iodsky.mysweldo.attendance;

import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceDto toDto(Attendance attendance) {
        if (attendance == null) {
            return null;
        }

        return AttendanceDto.builder()
                .id(attendance.getId())
                .employeeId(attendance.getEmployee().getId())
                .timeIn(attendance.getTimeIn())
                .timeOut(attendance.getTimeOut())
                .totalHours(attendance.getTotalHours())
                .build();
    }

    public AttendanceDto toDto(AttendanceView attendance) {
        if (attendance == null) {
            return null;
        }

        return AttendanceDto.builder()
                .id(attendance.getId())
                .employeeFirstName(attendance.getEmployee_FirstName())
                .employeeLastName(attendance.getEmployee_LastName())
                .timeIn(attendance.getTimeIn())
                .timeOut(attendance.getTimeOut())
                .totalHours(attendance.getTotalHours())
                .build();
    }
}