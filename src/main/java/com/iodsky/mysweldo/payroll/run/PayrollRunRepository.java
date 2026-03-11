package com.iodsky.mysweldo.payroll.run;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Page<PayrollRun> getAllByPeriodStartDateGreaterThanEqualAndPeriodEndDateLessThanEqual(LocalDate periodStartDate, LocalDate periodEnDate, Pageable pageable);
    Page<PayrollRun> getAllByType(PayrollRunType type, Pageable pageable);
    Page<PayrollRun> getAllByStatus(PayrollRunStatus status, Pageable pageable);
    Page<PayrollRun> getAllByTypeAndStatus(PayrollRunType type, PayrollRunStatus status, Pageable pageable);

}
