package com.iodsky.mysweldo.payroll.item;

import com.iodsky.mysweldo.payroll.run.PayrollRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {

    @Query("SELECT COUNT(pi) > 0 FROM PayrollItem pi " +
           "WHERE pi.employee.id = :employeeId " +
           "AND pi.payrollRun.period.startDate BETWEEN :monthStart AND :monthEnd " +
           "AND pi.payrollRun.status IN :statuses")
    boolean existsByEmployeeInMonthWithStatus(
            @Param("employeeId") Long employeeId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("statuses") List<PayrollRunStatus> statuses
    );

    Page<PayrollItem> findAllByEmployee_IdAndPayrollRun_Period_StartDateLessThanEqualAndPayrollRun_Period_EndDateGreaterThanEqual(
            Long employeeId,
            LocalDate endDate,
            LocalDate startDate,
            Pageable pageable
    );

    Page<PayrollItem> findAllByEmployee_Id(Long employeeId, Pageable pageable);

    Boolean existsByPayrollRun_IdAndEmployee_Id(UUID payrollRunId, Long employeeId);

    @EntityGraph(attributePaths = "employerContributions")
    List<PayrollItem> findAllByPayrollRun_Id(UUID payrollRunId);

    Page<PayrollItem> findAllByPayrollRun_Id(UUID payrollRunId, Pageable pageable);

    Optional<PayrollItem> findByPayrollRun_IdAndId(UUID payrollRun_id, UUID id);

}
