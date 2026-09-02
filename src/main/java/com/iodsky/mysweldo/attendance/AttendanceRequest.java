package com.iodsky.mysweldo.attendance;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceRequest {

    private Long employeeId;
    private LocalDateTime timeIn;
    private LocalDateTime timeOut;

}