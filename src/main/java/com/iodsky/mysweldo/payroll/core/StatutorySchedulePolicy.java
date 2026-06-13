package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import com.iodsky.mysweldo.payroll.run.PayrollRunStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatutorySchedulePolicy {

    private static final List<PayrollRunStatus> FINALIZED_STATUSES =
            List.of(PayrollRunStatus.APPROVED, PayrollRunStatus.PROCESSED);

    private final PayrollItemRepository payrollItemRepository;

    public boolean shouldCollectStatutory(Long employeeId, PayrollRun run) {
        PayrollFrequency frequency = run.getPeriod().getFrequency();

        if (frequency == PayrollFrequency.SEMI_MONTHLY || frequency == PayrollFrequency.MONTHLY) {
            return true;
        }

        YearMonth runMonth = YearMonth.from(run.getPeriod().getStartDate());
        LocalDate monthStart = runMonth.atDay(1);
        LocalDate monthEnd = runMonth.atEndOfMonth();

        return !payrollItemRepository.existsByEmployeeInMonthWithStatus(
                employeeId, monthStart, monthEnd, FINALIZED_STATUSES);
    }
}
