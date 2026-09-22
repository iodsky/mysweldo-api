package com.iodsky.mysweldo.payroll.item;

import com.iodsky.mysweldo.payroll.run.PayrollRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {

    Page<PayrollItem> findAllByEmployee_IdAndPayrollRun_Status(Long employeeId, PayrollRunStatus status, Pageable pageable);

    Page<PayrollItem> findAllByEmployee_IdAndPayrollRun_StatusAndPayrollRun_Period_StartDateLessThanEqualAndPayrollRun_Period_EndDateGreaterThanEqual(
            Long employeeId,
            PayrollRunStatus status,
            LocalDate endDate,
            LocalDate startDate,
            Pageable pageable
    );

    Boolean existsByPayrollRun_IdAndEmployee_Id(UUID payrollRunId, Long employeeId);

    @EntityGraph(attributePaths = "employerContributions")
    List<PayrollItem> findAllByPayrollRun_Id(UUID payrollRunId);

    Page<PayrollItem> findAllByPayrollRun_Id(UUID payrollRunId, Pageable pageable);

    Optional<PayrollItem> findByPayrollRun_IdAndId(UUID payrollRun_id, UUID id);

}
