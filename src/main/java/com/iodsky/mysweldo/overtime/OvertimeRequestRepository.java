package com.iodsky.mysweldo.overtime;

import com.iodsky.mysweldo.common.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest, UUID> {

    boolean existsByEmployee_IdAndDate(Long employeeId, LocalDate date);

    Page<OvertimeRequest> findByDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<OvertimeRequest> findAllByEmployee_Id(Long employeeId, Pageable pageable);

    Page<OvertimeRequest> findByEmployee_Supervisor_Id(Long supervisorId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(o.overtimeHours), 0)
            FROM OvertimeRequest o
            WHERE o.employee.id = :employeeId
            AND o.date BETWEEN :startDate AND :endDate
            AND o.status = :status
            """)
    BigDecimal sumOvertimeHoursByEmployeeI_IdAndDateBetweenAndStatus(Long employeeId, LocalDate startDate, LocalDate endDate, RequestStatus status);

}
