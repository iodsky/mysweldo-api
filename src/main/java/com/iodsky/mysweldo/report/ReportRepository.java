package com.iodsky.mysweldo.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findAllByType(ReportType type, Pageable pageable);

    Page<Report> findAllByPayrollRun_Id(UUID payrollRunId, Pageable pageable);

}