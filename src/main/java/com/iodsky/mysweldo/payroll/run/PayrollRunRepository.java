package com.iodsky.mysweldo.payroll.run;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Page<PayrollRun> getAllByPeriod_StartDateGreaterThanEqualAndPeriod_EndDateLessThanEqual(LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<PayrollRun> getAllByType(PayrollRunType type, Pageable pageable);
    Page<PayrollRun> getAllByStatus(PayrollRunStatus status, Pageable pageable);
    Page<PayrollRun> getAllByTypeAndStatus(PayrollRunType type, PayrollRunStatus status, Pageable pageable);

    @Query("""
            SELECT COUNT(r) > 0
            FROM PayrollRun r
            WHERE r.type = :type
              AND r.period.startDate <= :endDate
              AND r.period.endDate >= :startDate
            """)
    boolean existsOverlappingByType(
            @Param("type") PayrollRunType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT pg_advisory_xact_lock(:lockKey)", nativeQuery = true)
    void acquireRunCreationLock(@Param("lockKey") long lockKey);

}
