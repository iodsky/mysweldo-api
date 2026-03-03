package com.iodsky.mysweldo.payroll.core;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, UUID> {

    Page<Payroll> findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
            LocalDate endDate,
            LocalDate startDate,
            Pageable pageable
    );

    Page<Payroll> findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
            Long employeeId,
            LocalDate endDate,
            LocalDate startDate,
            Pageable pageable
    );

    Page<Payroll> findAllByEmployee_Id(Long employeeId, Pageable pageable);

    boolean existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(Long employeeId, LocalDate startDate, LocalDate endDate);

}
