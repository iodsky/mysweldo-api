package com.iodsky.mysweldo.leave.credit;

import org.springframework.stereotype.Component;

@Component
public class LeaveCreditMapper {

    public LeaveCreditDto toDto (LeaveCredit leaveCredit) {
        return LeaveCreditDto.builder()
                .id(leaveCredit.getId())
                .employeeId(leaveCredit.getEmployee().getId())
                .type(leaveCredit.getType().toString())
                .effectiveDate(leaveCredit.getEffectiveDate())
                .credits(leaveCredit.getCredits())
                .build();
    }
}
