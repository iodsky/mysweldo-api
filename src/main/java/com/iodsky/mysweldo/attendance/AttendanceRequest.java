package com.iodsky.mysweldo.attendance;


import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceRequest {

    private Long employeeId;
    private LocalDate date;
    private LocalTime timeIn;
    private LocalTime timeOut;

}
